/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.service;

import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistFactory;
import chameleon.playlist.SpecificPlaylistProvider;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlaylistDao;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.Playlist;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.playlist.PlaylistExportHandler;
import org.airsonic.player.service.playlist.PlaylistImportHandler;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Provides services for loading and saving playlists to and from persistent storage.
 *
 * @author Sindre Mehus
 * @see PlayQueue
 */
@Service
public class PlaylistService {

    private static final Logger LOG = LoggerFactory.getLogger(PlaylistService.class);
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private PlaylistDao playlistDao;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private List<PlaylistExportHandler> exportHandlers;
    @Autowired
    private List<PlaylistImportHandler> importHandlers;
    @Autowired
    private SimpMessagingTemplate brokerTemplate;
    @Autowired
    private PathWatcherService pathWatcherService;

    public PlaylistService(
            MediaFileDao mediaFileDao,
            PlaylistDao playlistDao,
            SecurityService securityService,
            SettingsService settingsService,
            List<PlaylistExportHandler> exportHandlers,
            List<PlaylistImportHandler> importHandlers
    ) {
        Assert.notNull(mediaFileDao, "mediaFileDao must not be null");
        Assert.notNull(playlistDao, "playlistDao must not be null");
        Assert.notNull(securityService, "securityservice must not be null");
        Assert.notNull(settingsService, "settingsService must not be null");
        Assert.notNull(exportHandlers, "exportHandlers must not be null");
        Assert.notNull(importHandlers, "importHandlers must not be null");
        this.mediaFileDao = mediaFileDao;
        this.playlistDao = playlistDao;
        this.securityService = securityService;
        this.settingsService = settingsService;
        this.exportHandlers = exportHandlers;
        this.importHandlers = importHandlers;
    }

    @PostConstruct
    public void init() throws IOException {
        addPlaylistFolderWatcher();
    }

    BiConsumer<Path, WatchEvent<Path>> playlistModified = (p, we) -> {
        Path fullPath = p.resolve(we.context());
        importPlaylist(fullPath, playlistDao.getAllPlaylists());
    };

    public void addPlaylistFolderWatcher() {
        Path playlistFolder = Paths.get(settingsService.getPlaylistFolder());
        if (Files.exists(playlistFolder) && Files.isDirectory(playlistFolder)) {
            try {
                pathWatcherService.setWatcher("Playlist folder watcher", playlistFolder, playlistModified, null, playlistModified, null);
            } catch (Exception e) {
                LOG.warn("Issues setting watcher for folder: {}", playlistFolder);
            }
        }
    }

    public List<Playlist> getAllPlaylists() {
        return playlistDao.getAllPlaylists();
    }

    public List<Playlist> getReadablePlaylistsForUser(String username) {
        return playlistDao.getReadablePlaylistsForUser(username);
    }

    public List<Playlist> getWritablePlaylistsForUser(String username) {

        // Admin users are allowed to modify all playlists that are visible to them.
        if (securityService.isAdmin(username)) {
            return getReadablePlaylistsForUser(username);
        }

        return playlistDao.getWritablePlaylistsForUser(username);
    }

    @Cacheable(cacheNames = "playlistCache", unless = "#result == null")
    public Playlist getPlaylist(int id) {
        return playlistDao.getPlaylist(id);
    }

    @Cacheable(cacheNames = "playlistUsersCache", unless = "#result == null")
    public List<String> getPlaylistUsers(int playlistId) {
        return playlistDao.getPlaylistUsers(playlistId);
    }

    public List<MediaFile> getFilesInPlaylist(int id) {
        return getFilesInPlaylist(id, false);
    }

    public List<MediaFile> getFilesInPlaylist(int id, boolean includeNotPresent) {
        return mediaFileDao.getFilesInPlaylist(id).stream().filter(x -> includeNotPresent || x.isPresent()).collect(Collectors.toList());
    }

    public void setFilesInPlaylist(int id, List<MediaFile> files) {
        playlistDao.setFilesInPlaylist(id, files);
        Playlist playlist = new Playlist(getPlaylist(id));
        double duration = files.parallelStream().filter(f -> f.getDuration() != null).mapToDouble(f -> f.getDuration()).sum();
        playlist.setFileCount(files.size());
        playlist.setDuration(duration);
        updatePlaylist(playlist, true);
    }

    public void createPlaylist(Playlist playlist) {
        playlistDao.createPlaylist(playlist);
        if (playlist.getShared()) {
            runAsync(() -> brokerTemplate.convertAndSend("/topic/playlists/updated", playlist));
        } else {
            runAsync(() -> brokerTemplate.convertAndSendToUser(playlist.getUsername(), "/queue/playlists/updated", playlist));
        }
    }

    @CacheEvict(cacheNames = "playlistUsersCache", key = "#playlist.id")
    public void addPlaylistUser(Playlist playlist, String username) {
        playlistDao.addPlaylistUser(playlist.getId(), username);
        // this might cause dual notifications on the client if the playlist is already public
        runAsync(() -> brokerTemplate.convertAndSendToUser(username, "/queue/playlists/updated", playlist));
    }

    @CacheEvict(cacheNames = "playlistUsersCache", key = "#playlist.id")
    public void deletePlaylistUser(Playlist playlist, String username) {
        playlistDao.deletePlaylistUser(playlist.getId(), username);
        if (!playlist.getShared()) {
            runAsync(() -> brokerTemplate.convertAndSendToUser(username, "/queue/playlists/deleted", playlist.getId()));
        }
    }

    public boolean isReadAllowed(Playlist playlist, String username) {
        if (username == null) {
            return false;
        }
        if (username.equals(playlist.getUsername()) || playlist.getShared()) {
            return true;
        }
        return playlistDao.getPlaylistUsers(playlist.getId()).contains(username);
    }

    public boolean isWriteAllowed(Playlist playlist, String username) {
        return username != null && username.equals(playlist.getUsername());
    }

    @CacheEvict(cacheNames = "playlistCache")
    public void deletePlaylist(int id) {
        playlistDao.deletePlaylist(id);
        runAsync(() -> brokerTemplate.convertAndSend("/topic/playlists/deleted", id));
    }

    public void updatePlaylist(Playlist playlist) {
        updatePlaylist(playlist, false);
    }

    /**
     * DO NOT pass in the mutated cache value. This method relies on the existing
     * cached value to check the differences
     */
    @CacheEvict(cacheNames = "playlistCache", key = "#playlist.id")
    public void updatePlaylist(Playlist playlist, boolean filesChangedBroadcastContext) {
        Playlist oldPlaylist = getPlaylist(playlist.getId());
        playlistDao.updatePlaylist(playlist);
        runAsync(() -> {
            BroadcastedPlaylist bp = new BroadcastedPlaylist(playlist, filesChangedBroadcastContext);
            if (playlist.getShared()) {
                brokerTemplate.convertAndSend("/topic/playlists/updated", bp);
            } else {
                if (oldPlaylist.getShared()) {
                    brokerTemplate.convertAndSend("/topic/playlists/deleted", playlist.getId());
                }
                Stream.concat(Stream.of(playlist.getUsername()), getPlaylistUsers(playlist.getId()).stream())
                        .forEach(u -> brokerTemplate.convertAndSendToUser(u, "/queue/playlists/updated", bp));
            }
        });
    }

    public static class BroadcastedPlaylist extends Playlist {
        private final boolean filesChanged;

        public BroadcastedPlaylist(Playlist p, boolean filesChanged) {
            super(p);
            this.filesChanged = filesChanged;
        }

        public boolean getFilesChanged() {
            return filesChanged;
        }
    }

    public String getExportPlaylistExtension() {
        String format = settingsService.getPlaylistExportFormat();
        SpecificPlaylistProvider provider = SpecificPlaylistFactory.getInstance().findProviderById(format);
        return provider.getContentTypes()[0].getExtensions()[0];
    }

    public void exportPlaylist(int id, OutputStream out) throws Exception {
        String format = settingsService.getPlaylistExportFormat();
        SpecificPlaylistProvider provider = SpecificPlaylistFactory.getInstance().findProviderById(format);
        PlaylistExportHandler handler = getExportHandler(provider);
        SpecificPlaylist specificPlaylist = handler.handle(id, provider);
        specificPlaylist.writeTo(out, StringUtil.ENCODING_UTF8);
    }

    private PlaylistImportHandler getImportHandler(SpecificPlaylist playlist) {
        return importHandlers.stream()
                             .filter(handler -> handler.canHandle(playlist.getClass()))
                             .findFirst()
                             .orElseThrow(() -> new RuntimeException("No import handler for " + playlist.getClass()
                                                                                                        .getName()));

    }

    private PlaylistExportHandler getExportHandler(SpecificPlaylistProvider provider) {
        return exportHandlers.stream()
                             .filter(handler -> handler.canHandle(provider.getClass()))
                             .findFirst()
                             .orElseThrow(() -> new RuntimeException("No export handler for " + provider.getClass()
                                                                                                        .getName()));
    }

    public void importPlaylists() {
        try {
            LOG.info("Starting playlist import.");
            doImportPlaylists();
            LOG.info("Completed playlist import.");
        } catch (Throwable x) {
            LOG.warn("Failed to import playlists: " + x, x);
        }
    }

    private void doImportPlaylists() {
        String playlistFolderPath = settingsService.getPlaylistFolder();
        if (playlistFolderPath == null) {
            return;
        }
        Path playlistFolder = Paths.get(playlistFolderPath);
        if (!Files.exists(playlistFolder)) {
            return;
        }

        List<Playlist> allPlaylists = playlistDao.getAllPlaylists();
        try (Stream<Path> children = Files.walk(playlistFolder)) {
            children.forEach(f -> importPlaylist(f, allPlaylists));
        } catch (IOException ex) {
            LOG.warn("Error while reading directory {} when importing playlists", playlistFolder, ex);
        }
    }

    private void importPlaylist(Path f, List<Playlist> allPlaylists) {
        if (Files.isRegularFile(f) && Files.isReadable(f)) {
            try {
                importPlaylistIfUpdated(f, allPlaylists.stream().filter(p -> f.getFileName().toString().equals(p.getImportedFrom())).findAny().orElse(null));
            } catch (Exception x) {
                LOG.warn("Failed to auto-import playlist {}", f, x);
            }
        }
    }

    public Playlist importPlaylist(String username, String playlistName, String fileName, InputStream inputStream, Playlist existingPlaylist) throws Exception {

        // TODO: handle other encodings
        final SpecificPlaylist inputSpecificPlaylist = SpecificPlaylistFactory.getInstance().readFrom(inputStream,
                "UTF-8");
        if (inputSpecificPlaylist == null) {
            throw new Exception("Unsupported playlist " + fileName);
        }
        PlaylistImportHandler importHandler = getImportHandler(inputSpecificPlaylist);
        LOG.debug("Using {} playlist import handler", importHandler.getClass().getSimpleName());

        Pair<List<MediaFile>, List<String>> result = importHandler.handle(inputSpecificPlaylist);

        if (result.getLeft().isEmpty() && !result.getRight().isEmpty()) {
            throw new Exception("No songs in the playlist were found.");
        }

        for (String error : result.getRight()) {
            LOG.warn("File in playlist '{}' not found: {}", fileName, error);
        }
        Instant now = Instant.now();
        Playlist playlist;
        if (existingPlaylist == null) {
            playlist = new Playlist();
            playlist.setUsername(username);
            playlist.setCreated(now);
            playlist.setChanged(now);
            playlist.setShared(true);
            playlist.setName(playlistName);
            playlist.setComment("Auto-imported from " + fileName);
            playlist.setImportedFrom(fileName);
            createPlaylist(playlist);
        } else {
            playlist = existingPlaylist;
        }

        setFilesInPlaylist(playlist.getId(), result.getLeft());

        return playlist;
    }

    private void importPlaylistIfUpdated(Path file, Playlist existingPlaylist) throws Exception {
        String fileName = file.getFileName().toString();
        if (existingPlaylist != null && Files.getLastModifiedTime(file).toMillis() <= existingPlaylist.getChanged().toEpochMilli()) {
            // Already imported and not changed since.
            return;
        }

        try (InputStream in = Files.newInputStream(file)) {
            importPlaylist(User.USERNAME_ADMIN, FilenameUtils.getBaseName(fileName), fileName, in, existingPlaylist);
            LOG.info("Auto-imported playlist {}", file);
        }
    }
}
