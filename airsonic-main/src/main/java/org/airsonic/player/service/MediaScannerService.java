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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
            // Maps from artist name to album count.
            Map<String, AtomicInteger> albumCount = new ConcurrentHashMap<>();
            Map<String, Artist> artists = new ConcurrentHashMap<>();
            Map<String, Album> albums = new ConcurrentHashMap<>();
            Map<Integer, Album> albumsInDb = new ConcurrentHashMap<>();
            Map<Integer, Set<String>> encountered = new ConcurrentHashMap<>();
            Genres genres = new Genres();

            scanCount.set(0);

            mediaFileService.setMemoryCacheEnabled(false);
            indexManager.startIndexing();

            // Recurse through all files on disk.
            mediaFolderService.getAllMusicFolders()
                .parallelStream()
                    .forEach(musicFolder -> scanFile(mediaFileService.getMediaFile(Paths.get(""), musicFolder, false),
                            musicFolder, statistics, albumCount, artists, albums, albumsInDb, genres, encountered));

            LOG.info("Scanned media library with {} entries.", scanCount.get());

            // Update statistics
            statistics.incrementArtists(albumCount.size());
            statistics.incrementAlbums(albumCount.values().parallelStream().mapToInt(x -> x.get()).sum());

            LOG.info("Persisting albums");
            CompletableFuture<Void> albumPersistence = CompletableFuture
                    .allOf(albums.values().parallelStream()
                            .distinct()
                            .map(a -> CompletableFuture.supplyAsync(() -> {
                                albumDao.createOrUpdateAlbum(a);
                                return a;
                            }, pool).thenAcceptAsync(coverArtService::persistIfNeeded))
                            .toArray(CompletableFuture[]::new))
                    .thenRunAsync(() -> {
                        LOG.info("Marking non-present albums.");
                        albumDao.markNonPresent(statistics.getScanDate());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("Album persistence complete"), pool);

            LOG.info("Persisting artists");
            CompletableFuture<Void> artistPersistence = CompletableFuture
                    .allOf(artists.values().parallelStream()
                            .map(a -> CompletableFuture.supplyAsync(() -> {
                                artistDao.createOrUpdateArtist(a);
                                return a;
                            }, pool).thenAcceptAsync(coverArtService::persistIfNeeded))
                            .toArray(CompletableFuture[]::new))
                    .thenRunAsync(() -> {
                        LOG.info("Marking non-present artists.");
                        artistDao.markNonPresent(statistics.getScanDate());
                    }, pool)
                    .thenRunAsync(() -> LOG.info("Artist persistence complete"), pool);

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
            Map<String, AtomicInteger> albumCount, Map<String, Artist> artists, Map<String, Album> albums,
            Map<Integer, Album> albumsInDb, Genres genres, Map<Integer, Set<String>> encountered) {
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

        if (file.isDirectory()) {
            mediaFileService.getChildrenOf(file, true, true, false, false).parallelStream()
                    .forEach(child -> scanFile(child, musicFolder, statistics, albumCount, artists, albums, albumsInDb, genres, encountered));
        } else {
            if (musicFolder.getType() == MusicFolder.Type.MEDIA) {
                updateAlbum(file, musicFolder, statistics.getScanDate(), albumCount, albums, albumsInDb);
                updateArtist(file, musicFolder, statistics.getScanDate(), albumCount, artists);
            }
            statistics.incrementSongs(1);
        }

        updateGenres(file, genres);
        encountered.computeIfAbsent(file.getFolderId(), k -> ConcurrentHashMap.newKeySet()).add(file.getPath());

        if (file.getDuration() != null) {
            statistics.incrementTotalDurationInSeconds(file.getDuration());
        }
        if (file.getFileSize() != null) {
            statistics.incrementTotalLengthInBytes(file.getFileSize());
        }
    }

    private void updateGenres(MediaFile file, Genres genres) {
        String genre = file.getGenre();
        if (genre == null) {
            return;
        }
        if (file.isAlbum()) {
            genres.incrementAlbumCount(genre, settingsService.getGenreSeparators());
        } else if (file.isAudio()) {
            genres.incrementSongCount(genre, settingsService.getGenreSeparators());
        }
    }

    private void updateAlbum(MediaFile file, MusicFolder musicFolder, Instant lastScanned, Map<String, AtomicInteger> albumCount, Map<String, Album> albums, Map<Integer, Album> albumsInDb) {
        String artist = file.getAlbumArtist() != null ? file.getAlbumArtist() : file.getArtist();
        if (file.getAlbumName() == null || artist == null || file.getParentPath() == null || !file.isAudio()) {
            return;
        }

        final AtomicBoolean firstEncounter = new AtomicBoolean(false);
        Album album = albums.compute(file.getAlbumName() + "|" + artist, (k,v) -> {
            Album a = v;

            if (a == null) {
                Album dbAlbum = albumDao.getAlbum(artist, file.getAlbumName());
                if (dbAlbum != null) {
                    a = albumsInDb.computeIfAbsent(dbAlbum.getId(), aid -> {
                        // reset stats when first retrieve from the db for new scan
                        dbAlbum.setDuration(0);
                        dbAlbum.setSongCount(0);
                        return dbAlbum;
                    });
                }
            }

            if (a == null) {
                a = new Album();
                a.setPath(file.getParentPath());
                a.setName(file.getAlbumName());
                a.setArtist(artist);
                a.setCreated(file.getChanged());
            }

            firstEncounter.set(!lastScanned.equals(a.getLastScanned()));

            if (file.getDuration() != null) {
                a.incrementDuration(file.getDuration());
            }
            if (file.isAudio()) {
                a.incrementSongCount();
            }

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            return a;
        });

        if (file.getMusicBrainzReleaseId() != null) {
            album.setMusicBrainzReleaseId(file.getMusicBrainzReleaseId());
        }
        if (file.getYear() != null) {
            album.setYear(file.getYear());
        }
        if (file.getGenre() != null) {
            album.setGenre(file.getGenre());
        }

        if (album.getArt() == null) {
            MediaFile parent = mediaFileService.getParentOf(file, true); // true because the parent has recently already been scanned
            if (parent != null) {
                CoverArt art = coverArtService.get(EntityType.MEDIA_FILE, parent.getId());
                if (!CoverArt.NULL_ART.equals(art)) {
                    album.setArt(new CoverArt(-1, EntityType.ALBUM, art.getPath(), art.getFolderId(), false));
                }
            }
        }

        if (firstEncounter.get()) {
            album.setFolderId(musicFolder.getId());
            albumCount.computeIfAbsent(artist, k -> new AtomicInteger(0)).incrementAndGet();
            indexManager.index(album);
        }

        // Update the file's album artist, if necessary.
        if (!ObjectUtils.equals(album.getArtist(), file.getAlbumArtist())) {
            file.setAlbumArtist(album.getArtist());
            mediaFileService.updateMediaFile(file);
        }
    }

    private void updateArtist(MediaFile file, MusicFolder musicFolder, Instant lastScanned, Map<String, AtomicInteger> albumCount, Map<String, Artist> artists) {
        if (file.getAlbumArtist() == null || !file.isAudio()) {
            return;
        }

        final AtomicBoolean firstEncounter = new AtomicBoolean(false);

        Artist artist = artists.compute(file.getAlbumArtist(), (k,v) -> {
            Artist a = v;

            if (a == null) {
                a = artistDao.getArtist(k);
            }

            if (a == null) {
                a = new Artist();
                a.setName(k);
            }

            int n = Math.max(Optional.ofNullable(albumCount.get(a.getName())).map(x -> x.get()).orElse(0), Optional.ofNullable(a.getAlbumCount()).orElse(0));
            a.setAlbumCount(n);

            firstEncounter.set(!lastScanned.equals(a.getLastScanned()));

            a.setLastScanned(lastScanned);
            a.setPresent(true);

            return a;
        });

        if (artist.getArt() == null) {
            MediaFile parent = mediaFileService.getParentOf(file, true); // true because the parent has recently already been scanned
            if (parent != null) {
                CoverArt art = coverArtService.get(EntityType.MEDIA_FILE, parent.getId());
                if (!CoverArt.NULL_ART.equals(art)) {
                    artist.setArt(new CoverArt(-1, EntityType.ARTIST, art.getPath(), art.getFolderId(), false));
                }
            }
        }

        if (firstEncounter.get()) {
            artist.setFolderId(musicFolder.getId());
            indexManager.index(artist, musicFolder);
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
