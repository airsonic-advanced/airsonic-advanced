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

import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.service.search.IndexManager;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.subsonic.restapi.ScanStatus;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Provides services for scanning the music library.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaScannerService.class);

    private volatile boolean scanning;

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private IndexManager indexManager;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private CoverArtService coverArtService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private TaskSchedulingService taskService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    @Value("${MediaScannerParallelism:#{T(java.lang.Runtime).getRuntime().availableProcessors() + 1}}")
    private int scannerParallelism;

    private AtomicInteger scanCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        indexManager.initializeIndexDirectory();
        schedule();
    }

    public void initNoSchedule() throws IOException {
        indexManager.deleteOldIndexFiles();
    }

    /**
     * Schedule background execution of media library scanning.
     */
    public synchronized void schedule() {
        long daysBetween = settingsService.getIndexCreationInterval();
        int hour = settingsService.getIndexCreationHour();

        if (daysBetween == -1) {
            LOG.info("Automatic media scanning disabled.");
            taskService.unscheduleTask("mediascanner-IndexingTask");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(0).withSecond(0);
        if (now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        long initialDelayMillis = ChronoUnit.MILLIS.between(now, nextRun);
        Instant firstTime = Instant.now().plusMillis(initialDelayMillis);

        taskService.scheduleAtFixedRate("mediascanner-IndexingTask", () -> scanLibrary(), firstTime, Duration.ofDays(daysBetween), true);

        LOG.info("Automatic media library scanning scheduled to run every {} day(s), starting at {}", daysBetween, nextRun);

        // In addition, create index immediately if it doesn't exist on disk.
        if (neverScanned()) {
            LOG.info("Media library never scanned. Doing it now.");
            scanLibrary();
        }
    }

    boolean neverScanned() {
        return indexManager.getStatistics() == null;
    }

    /**
     * Returns whether the media library is currently being scanned.
     */
    public boolean isScanning() {
        return scanning;
    }

    private void setScanning(boolean scanning) {
        this.scanning = scanning;
        broadcastScanStatus();
    }

    private void broadcastScanStatus() {
        CompletableFuture.runAsync(() -> {
            ScanStatus status = new ScanStatus();
            status.setCount(scanCount.longValue());
            status.setScanning(scanning);
            messagingTemplate.convertAndSend("/topic/scanStatus", status);
        });
    }

    /**
     * Returns the number of files scanned so far.
     */
    public int getScanCount() {
        return scanCount.get();
    }

    private static ForkJoinWorkerThreadFactory mediaScannerThreadFactory = new ForkJoinWorkerThreadFactory() {
        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("MediaLibraryScanner-" + worker.getPoolIndex());
            worker.setPriority(Thread.MIN_PRIORITY);
            return worker;
        }
    };

    /**
     * Scans the media library.
     * The scanning is done asynchronously, i.e., this method returns immediately.
     */
    public synchronized void scanLibrary() {
        if (isScanning()) {
            return;
        }
        setScanning(true);

        ForkJoinPool pool = new ForkJoinPool(scannerParallelism, mediaScannerThreadFactory, null, true);

        CompletableFuture.runAsync(() -> doScanLibrary(pool), pool)
                .thenRunAsync(() -> playlistService.importPlaylists(), pool)
                .whenComplete((r,e) -> pool.shutdown())
                .whenComplete((r,e) -> setScanning(false));
    }

    private void doScanLibrary(ForkJoinPool pool) {
        LOG.info("Starting to scan media library.");
        MediaLibraryStatistics statistics = new MediaLibraryStatistics();
        LOG.debug("New last scan date is {}", statistics.getScanDate());

        try {
            Map<String, Album> albums = new ConcurrentHashMap<>();
            Map<String, Set<Album>> artistAlbums = new ConcurrentHashMap<>();
            Map<Integer, Set<String>> encountered = new ConcurrentHashMap<>();
            Genres genres = new Genres();

            scanCount.set(0);

            mediaFileService.setMemoryCacheEnabled(false);
            indexManager.startIndexing();

            // Recurse through all files on disk.
            mediaFolderService.getAllMusicFolders().parallelStream()
                    .forEach(musicFolder -> scanFile(mediaFileService.getMediaFile(Paths.get(""), musicFolder, false),
                            musicFolder, statistics, albums, artistAlbums, genres, encountered));

            LOG.info("Scanned media library with {} entries.", scanCount.get());

            // Update statistics
            statistics.incrementArtists(artistAlbums.size());
            statistics.incrementAlbums(albums.size());

            LOG.info("Starting persistence phase of media scan");
            List<CompletableFuture<Album>> albumsInDB = albums.values().parallelStream()
                    .map(a -> CompletableFuture.supplyAsync(() -> {
                        albumDao.createOrUpdateAlbum(a);
                        return a;
                    }, pool)).collect(toList());
            CompletableFuture<Void> albumsInDBAll = CompletableFuture.allOf(albumsInDB.toArray(new CompletableFuture[0]));
            albumsInDBAll.thenAcceptAsync(v -> LOG.info("Albums persisted in DB."), pool);

            CompletableFuture<Void> albumsInIndexAll = CompletableFuture.allOf(albumsInDB.parallelStream()
                    .map(cfa -> cfa.thenAcceptAsync(indexManager::index, pool))
                    .toArray(CompletableFuture[]::new));
            albumsInIndexAll.thenAcceptAsync(v -> LOG.info("Albums persisted in index."), pool);

            CompletableFuture<Void> albumsNotPresent = albumsInDBAll.thenRunAsync(() -> {
                LOG.info("Marking non-present albums.");
                albumDao.markNonPresent(statistics.getScanDate());
                LOG.info("Marked non-present albums.");
            }, pool);

            List<CompletableFuture<Artist>> artistsInMem = artistAlbums.entrySet().parallelStream().map(e -> albumsInDBAll.thenApplyAsync(v -> {
                Artist ar = artistDao.getArtist(e.getKey());
                if (ar == null) {
                    ar = new Artist();
                    ar.setName(e.getKey());
                }
                ar.setPresent(true);
                ar.setLastScanned(statistics.getScanDate());
                ar.setAlbumIds(e.getValue().stream().map(al -> al.getId()).collect(toSet()));
                ar.setArt(e.getValue().stream()
                        .filter(al -> al.getArt() != null)
                        .findFirst()
                        .map(al -> new CoverArt(-1, EntityType.ARTIST, al.getArt().getPath(), al.getArt().getFolderId(), false))
                        .orElse(null));
                return ar;
            }, pool)).collect(toList());
            CompletableFuture<Void> artistsInMemAll = CompletableFuture.allOf(artistsInMem.toArray(new CompletableFuture[0]));
            artistsInMemAll.thenAcceptAsync(v -> LOG.info("Artist list generated."), pool);

            CompletableFuture<Void> albumsCoverArtAll = CompletableFuture.allOf(albumsInDB.parallelStream()
                    .map(cfa -> cfa.thenAcceptBothAsync(artistsInMemAll, (a, v) -> coverArtService.persistIfNeeded(a), pool))
                    .toArray(CompletableFuture[]::new));
            albumsCoverArtAll.thenAcceptAsync(v -> LOG.info("Album cover arts persisted."), pool);

            List<CompletableFuture<Artist>> artistsInDB = artistsInMem.parallelStream()
                    .map(cfa -> cfa.thenApplyAsync(a -> {
                        artistDao.createOrUpdateArtist(a);
                        return a;
                    }, pool)).collect(toList());
            CompletableFuture<Void> artistsInDBAll = CompletableFuture.allOf(artistsInDB.toArray(new CompletableFuture[0]));
            artistsInDBAll.thenAcceptAsync(v -> LOG.info("Artists persisted in DB."), pool);

            CompletableFuture<Void> artistsInIndexAll = CompletableFuture.allOf(artistsInDB.parallelStream()
                    .map(cfa -> cfa.thenAcceptAsync(indexManager::index, pool))
                    .toArray(CompletableFuture[]::new));
            artistsInIndexAll.thenAcceptAsync(v -> LOG.info("Artists persisted in index."), pool);

            CompletableFuture<Void> artistsCoverArtAll = CompletableFuture.allOf(artistsInDB.parallelStream()
                    .map(cfa -> cfa.thenAcceptAsync(coverArtService::persistIfNeeded, pool))
                    .toArray(CompletableFuture[]::new));
            artistsCoverArtAll.thenAcceptAsync(v -> LOG.info("Artist cover arts persisted."), pool);

            CompletableFuture<Void> artistsNotPresent = artistsInDBAll.thenRunAsync(() -> {
                LOG.info("Marking non-present artists.");
                artistDao.markNonPresent(statistics.getScanDate());
                LOG.info("Marked non-present artists.");
            }, pool);

            CompletableFuture<Void> albumPersistence = CompletableFuture
                    .allOf(albumsInDBAll, albumsInIndexAll, albumsCoverArtAll, albumsNotPresent)
                    .thenAcceptAsync(v -> LOG.info("Album persistence complete."), pool);
            CompletableFuture<Void> artistPersistence = CompletableFuture
                    .allOf(artistsInMemAll, artistsInDBAll, artistsInIndexAll, artistsCoverArtAll, artistsNotPresent)
                    .thenAcceptAsync(v -> LOG.info("Artist persistence complete."), pool);

            LOG.info("Marking present files");
            CompletableFuture<Void> mediaFilePersistence = CompletableFuture
                    .runAsync(() -> mediaFileDao.markPresent(encountered, statistics.getScanDate()), pool)
                    .thenRunAsync(() -> {
                        LOG.info("Marking non-present files.");
                        mediaFileDao.markNonPresent(statistics.getScanDate());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("File marking complete"), pool);

            LOG.info("Persisting genres");
            CompletableFuture<Void> genrePersistence = CompletableFuture
                    .runAsync(() -> {
                        LOG.info("Updating genres");
                        boolean genresSuccessful = mediaFileDao.updateGenres(genres.getGenres());
                        LOG.info("Genre persistence successfully complete: {}", genresSuccessful);
                    }, pool);

            CompletableFuture.allOf(albumPersistence, artistPersistence, mediaFilePersistence, genrePersistence).join();

            if (settingsService.getClearFullScanSettingAfterScan()) {
                settingsService.setClearFullScanSettingAfterScan(null);
                settingsService.setFullScan(null);
                settingsService.save();
            }

            LOG.info("Completed media library scan.");

        } catch (Throwable x) {
            LOG.error("Failed to scan media library.", x);
        } finally {
            mediaFileService.setMemoryCacheEnabled(true);
            indexManager.stopIndexing(statistics);
            LOG.info("Media library scan took {}s", ChronoUnit.SECONDS.between(statistics.getScanDate(), Instant.now()));
        }
    }

    private void scanFile(MediaFile file, MusicFolder musicFolder, MediaLibraryStatistics statistics,
            Map<String, Album> albums, Map<String, Set<Album>> artistAlbums, Genres genres, Map<Integer, Set<String>> encountered) {
        if (scanCount.incrementAndGet() % 250 == 0) {
            broadcastScanStatus();
            LOG.info("Scanned media library with {} entries.", scanCount.get());
        }

        LOG.trace("Scanning file {} in folder {} ({})", file.getPath(), musicFolder.getId(), musicFolder.getName());

        // Update the root folder if it has changed
        if (!musicFolder.getId().equals(file.getFolderId())) {
            file.setFolderId(musicFolder.getId());
            mediaFileService.updateMediaFile(file);
        }

        indexManager.index(file, musicFolder);
        Set<Genre> fileGenres = updateGenres(file, genres);

        if (file.isDirectory()) {
            mediaFileService.getChildrenOf(file, true, true, false, false).parallelStream()
                    .forEach(child -> scanFile(child, musicFolder, statistics, albums, artistAlbums, genres, encountered));
        } else {
            if (musicFolder.getType() == MusicFolder.Type.MEDIA) {
                updateAlbumAndArtist(file, musicFolder, statistics.getScanDate(), albums, artistAlbums, fileGenres);
            }
            statistics.incrementSongs(1);
        }

        encountered.computeIfAbsent(file.getFolderId(), k -> ConcurrentHashMap.newKeySet()).add(file.getPath());

        if (file.getDuration() != null) {
            statistics.incrementTotalDurationInSeconds(file.getDuration());
        }
        if (file.getFileSize() != null) {
            statistics.incrementTotalLengthInBytes(file.getFileSize());
        }
    }

    private Set<Genre> updateGenres(MediaFile file, Genres genres) {
        String genre = file.getGenre();
        if (genre == null) {
            return Collections.emptySet();
        }
        Set<Genre> fileGenres = genres.addGenres(Genres.parseGenres(genre, settingsService.getGenreSeparators()));
        fileGenres.forEach(g -> {
            if (file.isAlbum()) {
                g.incrementAlbumCount();
            } else if (file.isAudio()) {
                g.incrementSongCount();
            }
        });
        return fileGenres;
    }

    private void updateAlbumAndArtist(MediaFile file, MusicFolder musicFolder, Instant lastScanned,
            Map<String, Album> albums, Map<String, Set<Album>> artistAlbums, Set<Genre> fileGenres) {
        String artist = file.getAlbumArtist() != null ? file.getAlbumArtist() : file.getArtist();
        String albumName = file.getAlbumName() != null ? file.getAlbumName() : "(Unknown)";

        // albumName is never null
        if (artist == null || file.getParentPath() == null || !file.isAudio()) {
            return;
        }

        Album album = albums.computeIfAbsent(albumName + "|" + artist, k -> {
            Album a = albumDao.getAlbum(artist, albumName);
            if (a != null) {
                // reset stats when first retrieve from the db for new scan
                a.setDuration(0);
                a.setSongCount(0);
                a.getMediaFileIds().clear();
                a.getGenres().clear();
            } else {
                a = new Album();
                a.setName(albumName);
                a.setArtist(artist);
                a.setCreated(file.getChanged());
            }

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            // add album for artist
            artistAlbums.computeIfAbsent(artist, art -> ConcurrentHashMap.newKeySet()).add(a);

            return a;
        });

        if (file.getDuration() != null) {
            album.incrementDuration(file.getDuration());
        }
        if (file.isAudio()) {
            album.incrementSongCount();
        }
        if (file.getMusicBrainzReleaseId() != null) {
            album.setMusicBrainzReleaseId(file.getMusicBrainzReleaseId());
        }
        if (file.getYear() != null) {
            album.setYear(file.getYear());
        }
        album.getGenres().addAll(fileGenres);

        MediaFile parent = mediaFileService.getParentOf(file);
        if (parent != null) {
            album.getMediaFileIds().add(parent.getId());

            if (album.getArt() == null) {
                CoverArt art = coverArtService.get(EntityType.MEDIA_FILE, parent.getId());
                if (art != null) {
                    album.setArt(new CoverArt(-1, EntityType.ALBUM, art.getPath(), art.getFolderId(), false));
                }
            }
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            mediaFileService.updateMediaFile(file);
        }
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
}
