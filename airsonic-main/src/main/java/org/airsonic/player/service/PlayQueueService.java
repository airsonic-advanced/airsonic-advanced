package org.airsonic.player.service;

import com.google.common.collect.ImmutableMap;

import org.airsonic.player.ajax.MediaFileEntry;
import org.airsonic.player.ajax.PlayQueueInfo;
import org.airsonic.player.dao.InternetRadioDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.PlayQueueDao;
import org.airsonic.player.domain.InternetRadio;
import org.airsonic.player.domain.InternetRadioSource;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.PlayQueue;
import org.airsonic.player.domain.PlayQueue.RepeatStatus;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.PodcastEpisode;
import org.airsonic.player.domain.PodcastStatus;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SavedPlayQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;

@Service
public class PlayQueueService {
    @Autowired
    private JukeboxService jukeboxService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private LastFmService lastFmService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private PodcastService podcastService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private PlayQueueDao playQueueDao;
    @Autowired
    private InternetRadioDao internetRadioDao;
    @Autowired
    private JWTSecurityService jwtSecurityService;
    @Autowired
    private InternetRadioService internetRadioService;
    @Autowired
    private SimpMessagingTemplate brokerTemplate;

    public void start(Player player) {
        player.getPlayQueue().setStatus(PlayQueue.Status.PLAYING);
        if (player.isJukebox()) {
            jukeboxService.start(player);
        }
        runAsync(() -> brokerTemplate.convertAndSendToUser(player.getUsername(),
                "/queue/playqueues/" + player.getId() + "/playstatus", PlayQueue.Status.PLAYING));
    }

    public void stop(Player player) {
        player.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
        if (player.isJukebox()) {
            jukeboxService.stop(player);
        }
        runAsync(() -> brokerTemplate.convertAndSendToUser(player.getUsername(),
                "/queue/playqueues/" + player.getId() + "/playstatus", PlayQueue.Status.STOPPED));
    }

    public void toggleStartStop(Player player) {
        if (player.getPlayQueue().getStatus() == PlayQueue.Status.STOPPED) {
            start(player);
        } else {
            stop(player);
        }
    }

    public void skip(Player player, int index, long offset) {
        player.getPlayQueue().setIndex(index);
        if (player.isJukebox()) {
            jukeboxService.skip(player, index, (int) (offset / 1000));
        }

        runAsync(() -> {
            brokerTemplate.convertAndSendToUser(player.getUsername(),
                    "/queue/playqueues/" + player.getId() + "/skip", ImmutableMap.of("index", index, "offset", offset));
            brokerTemplate.convertAndSendToUser(player.getUsername(),
                    "/queue/playqueues/" + player.getId() + "/playstatus", player.getPlayQueue().getStatus());
        });
    }

    public void reloadSearchCriteria(Player player, String sessionId) {
        PlayQueue playQueue = player.getPlayQueue();
        int size = playQueue.size();
        playQueue.setInternetRadio(null);
        if (playQueue.getRandomSearchCriteria() != null) {
            playQueue.addFiles(true, mediaFileService.getRandomSongs(playQueue.getRandomSearchCriteria(), player.getUsername()));
        }

        broadcastPlayQueue(player, pq -> pq.setStartPlayerAt(size), sessionId);
    }

    public int savePlayQueue(Player player, int index, long offset) {
        PlayQueue playQueue = player.getPlayQueue();
        List<Integer> ids = MediaFile.toIdList(playQueue.getFiles());

        Integer currentId = index == -1 ? null : playQueue.getFile(index).getId();
        SavedPlayQueue savedPlayQueue = new SavedPlayQueue(null, player.getUsername(), ids, currentId, offset,
                Instant.now(), player.getUsername());
        playQueueDao.savePlayQueue(savedPlayQueue);

        return savedPlayQueue.getId();
    }

    public void loadSavedPlayQueue(Player player, String sessionId) {
        SavedPlayQueue savedPlayQueue = playQueueDao.getPlayQueue(player.getUsername());

        if (savedPlayQueue == null) {
            return;
        }

        PlayQueue playQueue = player.getPlayQueue();
        playQueue.clear();
        for (Integer mediaFileId : savedPlayQueue.getMediaFileIds()) {
            MediaFile mediaFile = mediaFileService.getMediaFile(mediaFileId);
            if (mediaFile != null) {
                playQueue.addFiles(true, mediaFile);
            }
        }

        long positionMillis = savedPlayQueue.getPositionMillis() == null ? 0L : savedPlayQueue.getPositionMillis();
        Integer currentId = savedPlayQueue.getCurrentMediaFileId();
        int currentIndex = Optional.ofNullable(currentId).map(mediaFileService::getMediaFile)
                .map(c -> playQueue.getFiles().indexOf(c)).orElse(-1);

        broadcastPlayQueue(player,
                currentIndex == -1 ? identity : pq -> pq.setStartPlayerAt(currentIndex).setStartPlayerAtPosition(positionMillis),
                sessionId);
    }

    public void playMediaFile(Player player, int id, String sessionId) {
        MediaFile file = mediaFileService.getMediaFile(id);

        List<MediaFile> songs;

        if (file.isFile()) {
            boolean queueFollowingSongs = settingsService.getUserSettings(player.getUsername()).isQueueFollowingSongs();
            if (queueFollowingSongs) {
                MediaFile dir = mediaFileService.getParentOf(file);
                songs = mediaFileService.getChildrenOf(dir, true, false, true);
                if (!songs.isEmpty()) {
                    int index = songs.indexOf(file);
                    songs = songs.subList(index, songs.size());
                }
            } else {
                songs = Arrays.asList(file);
            }
        } else {
            songs = mediaFileService.getDescendantsOf(file, true);
        }

        doPlay(player, songs, null, sessionId);
    }

    /**
     * @param index Start playing at this index, or play whole radio playlist if {@code null}.
     * @throws Exception
     */
    public void playInternetRadio(Player player, int id, Integer index, String sessionId) throws Exception {
        InternetRadio radio = internetRadioDao.getInternetRadioById(id);
        if (!radio.isEnabled()) {
            throw new Exception("Radio is not enabled");
        } else {
            internetRadioService.clearInternetRadioSourceCache(radio.getId());
        }

        doPlay(player, Collections.emptyList(), radio, sessionId);
    }

    /**
     * @param index Start playing at this index, or play whole playlist if {@code null}.
     */
    public void playPlaylist(Player player, int id, Integer index, String sessionId) {
        boolean queueFollowingSongs = settingsService.getUserSettings(player.getUsername()).isQueueFollowingSongs();

        List<MediaFile> files = playlistService.getFilesInPlaylist(id, true);
        if (!files.isEmpty() && index != null) {
            if (queueFollowingSongs) {
                files = files.subList(index, files.size());
            } else {
                files = Arrays.asList(files.get(index));
            }
        }

        // Remove non-present files
        files.removeIf(file -> !file.isPresent());
        doPlay(player, files, null, sessionId);
    }

    /**
     * @param index Start playing at this index, or play all top songs if {@code null}.
     */
    public void playTopSong(Player player, int id, Integer index, String sessionId) {
        boolean queueFollowingSongs = settingsService.getUserSettings(player.getUsername()).isQueueFollowingSongs();

        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(player.getUsername());
        List<MediaFile> files = lastFmService.getTopSongs(mediaFileService.getMediaFile(id), 50, musicFolders);
        if (!files.isEmpty() && index != null) {
            if (queueFollowingSongs) {
                files = files.subList(index, files.size());
            } else {
                files = Arrays.asList(files.get(index));
            }
        }

        doPlay(player, files, null, sessionId);
    }

    public void playPodcastChannel(Player player, int id, String sessionId) {
        List<PodcastEpisode> episodes = podcastService.getEpisodes(id);
        List<MediaFile> files = new ArrayList<>(episodes.size());
        for (PodcastEpisode episode : episodes) {
            if (episode.getStatus() == PodcastStatus.COMPLETED) {
                MediaFile mediaFile = mediaFileService.getMediaFile(episode.getMediaFileId());
                if (mediaFile != null && mediaFile.isPresent()) {
                    files.add(mediaFile);
                }
            }
        }

        doPlay(player, files, null, sessionId);
    }

    public void playPodcastEpisode(Player player, int id, String sessionId) {
        boolean queueFollowingSongs = settingsService.getUserSettings(player.getUsername()).isQueueFollowingSongs();

        PodcastEpisode episode = podcastService.getEpisode(id, false);
        List<PodcastEpisode> allEpisodes = podcastService.getEpisodes(episode.getChannelId());
        List<MediaFile> files = new ArrayList<>(allEpisodes.size());

        for (PodcastEpisode ep : allEpisodes) {
            if (ep.getStatus() == PodcastStatus.COMPLETED) {
                MediaFile mediaFile = mediaFileService.getMediaFile(ep.getMediaFileId());
                if (mediaFile != null && mediaFile.isPresent()
                        && (ep.getId().equals(episode.getId()) || queueFollowingSongs && !files.isEmpty())) {
                    files.add(mediaFile);
                }
            }
        }

        doPlay(player, files, null, sessionId);
    }

    public void playNewestPodcastEpisode(Player player, Integer index, String sessionId) {
        boolean queueFollowingSongs = settingsService.getUserSettings(player.getUsername()).isQueueFollowingSongs();

        List<PodcastEpisode> episodes = podcastService.getNewestEpisodes(10);
        List<MediaFile> files = episodes.stream().map(PodcastEpisode::getId).map(mediaFileService::getMediaFile)
                .collect(Collectors.toList());

        if (!files.isEmpty() && index != null) {
            if (queueFollowingSongs) {
                files = files.subList(index, files.size());
            } else {
                files = Arrays.asList(files.get(index));
            }
        }

        doPlay(player, files, null, sessionId);
    }

    public void playStarred(Player player, String sessionId) {
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(player.getUsername());
        List<MediaFile> files = mediaFileDao.getStarredFiles(0, Integer.MAX_VALUE, player.getUsername(), musicFolders);
        doPlay(player, files, null, sessionId);
    }

    public void playShuffle(Player player, String albumListType, int offset, int count, String genre, String decade,
            String sessionId) {
        MusicFolder selectedMusicFolder = settingsService.getSelectedMusicFolder(player.getUsername());
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(player.getUsername(),
                selectedMusicFolder == null ? null : selectedMusicFolder.getId());
        List<MediaFile> albums;
        if ("highest".equals(albumListType)) {
            albums = ratingService.getHighestRatedAlbums(offset, count, musicFolders);
        } else if ("frequent".equals(albumListType)) {
            albums = mediaFileService.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
        } else if ("recent".equals(albumListType)) {
            albums = mediaFileService.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
        } else if ("newest".equals(albumListType)) {
            albums = mediaFileService.getNewestAlbums(offset, count, musicFolders);
        } else if ("starred".equals(albumListType)) {
            albums = mediaFileService.getStarredAlbums(offset, count, player.getUsername(), musicFolders);
        } else if ("random".equals(albumListType)) {
            albums = searchService.getRandomAlbums(count, musicFolders);
        } else if ("alphabetical".equals(albumListType)) {
            albums = mediaFileService.getAlphabeticalAlbums(offset, count, true, musicFolders);
        } else if ("decade".equals(albumListType)) {
            int fromYear = Integer.parseInt(decade);
            int toYear = fromYear + 9;
            albums = mediaFileService.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
        } else if ("genre".equals(albumListType)) {
            albums = mediaFileService.getAlbumsByGenre(offset, count, genre, musicFolders);
        } else {
            albums = Collections.emptyList();
        }

        List<MediaFile> songs = new ArrayList<>();
        for (MediaFile album : albums) {
            songs.addAll(mediaFileService.getChildrenOf(album, true, false, false));
        }
        Collections.shuffle(songs);
        songs = songs.subList(0, Math.min(40, songs.size()));

        doPlay(player, songs, null, sessionId);
    }

    public void playRandom(Player player, int id, int count, String sessionId) {
        MediaFile file = mediaFileService.getMediaFile(id);
        List<MediaFile> randomFiles = mediaFileService.getRandomSongsForParent(file, count);

        doPlay(player, randomFiles, null, sessionId);
    }

    public void playSimilar(Player player, int id, int count, String sessionId) {
        MediaFile artist = mediaFileService.getMediaFile(id);
        List<MusicFolder> musicFolders = settingsService.getMusicFoldersForUser(player.getUsername());
        List<MediaFile> similarSongs = lastFmService.getSimilarSongs(artist, count, musicFolders);

        doPlay(player, similarSongs, null, sessionId);
    }

    private void doPlay(Player player, List<MediaFile> files, InternetRadio radio, String sessionId) {
        if (player.isWeb()) {
            mediaFileService.removeVideoFiles(files);
        }
        player.getPlayQueue().addFiles(false, files);
        player.getPlayQueue().setRandomSearchCriteria(null);
        player.getPlayQueue().setInternetRadio(radio);
        broadcastPlayQueue(player, startAt0, sessionId);
    }

    public void add(Player player, List<Integer> ids, Integer index, boolean removeVideoFiles, boolean broadcast) {
        PlayQueue playQueue = player.getPlayQueue();
        List<MediaFile> files = new ArrayList<>();
        for (int id : ids) {
            MediaFile ancestor = mediaFileService.getMediaFile(id);
            files.addAll(mediaFileService.getDescendantsOf(ancestor, true));
        }
        if (removeVideoFiles) {
            mediaFileService.removeVideoFiles(files);
        }
        if (index != null) {
            playQueue.addFilesAt(files, index);
        } else {
            playQueue.addFiles(true, files);
        }
        playQueue.setRandomSearchCriteria(null);
        playQueue.setInternetRadio(null);
        if (broadcast) {
            broadcastPlayQueue(player);
        }
    }

    public void addRandomCriteria(Player player, boolean append, RandomSearchCriteria criteria, boolean autoRandom) {
        player.getPlayQueue().addFiles(append, mediaFileService.getRandomSongs(criteria, player.getUsername()));
        player.getPlayQueue().setRandomSearchCriteria(autoRandom ? criteria : null);
        player.getPlayQueue().setInternetRadio(null);
        broadcastPlayQueue(player);
    }

    public void addPlaylist(Player player, int id, boolean removeVideoFiles) {
        PlayQueue playQueue = player.getPlayQueue();

        List<MediaFile> files = playlistService.getFilesInPlaylist(id);
        if (removeVideoFiles) {
            mediaFileService.removeVideoFiles(files);
        }

        playQueue.addFiles(true, files);

        playQueue.setRandomSearchCriteria(null);
        playQueue.setInternetRadio(null);
        broadcastPlayQueue(player);
    }

    public void reset(Player player, List<Integer> ids, boolean removeVideoFiles) {
        PlayQueue playQueue = player.getPlayQueue();
        MediaFile currentFile = playQueue.getCurrentFile();
        PlayQueue.Status status = playQueue.getStatus();

        playQueue.clear();
        add(player, ids, null, removeVideoFiles, false);

        int index = currentFile == null ? -1 : playQueue.getFiles().indexOf(currentFile);
        playQueue.setIndex(index);
        playQueue.setStatus(status);
        broadcastPlayQueue(player);
    }

    public void clear(Player player) {
        player.getPlayQueue().clear();
        broadcastPlayQueue(player);
    }

    public void shuffle(Player player) {
        player.getPlayQueue().shuffle();
        broadcastPlayQueue(player);
    }

    public void remove(Player player, List<Integer> indexes) {
        Collections.sort(indexes);

        for (int i = indexes.size() - 1; i >= 0; i--) {
            player.getPlayQueue().removeFileAt(indexes.get(i));
        }
        broadcastPlayQueue(player);
    }

    public void rearrange(Player player, List<Integer> indexes) {
        player.getPlayQueue().rearrange(indexes);
        broadcastPlayQueue(player);
    }

    public void up(Player player, int index) {
        player.getPlayQueue().moveUp(index);
        broadcastPlayQueue(player);
    }

    public void down(Player player, int index) {
        player.getPlayQueue().moveDown(index);
        broadcastPlayQueue(player);
    }

    public void toggleRepeat(Player player) {
        PlayQueue playQueue = player.getPlayQueue();
        if (playQueue.isShuffleRadioEnabled()) {
            playQueue.setRandomSearchCriteria(null);
            playQueue.setRepeatStatus(RepeatStatus.OFF);
        } else {
            playQueue.setRepeatStatus(RepeatStatus.getNext(playQueue.getRepeatStatus()));
        }
        runAsync(() -> brokerTemplate.convertAndSendToUser(player.getUsername(),
                "/queue/playqueues/" + player.getId() + "/repeat", playQueue.getRepeatStatus()));
    }

    public void undo(Player player) {
        player.getPlayQueue().undo();
        broadcastPlayQueue(player);
    }

    public void sort(Player player, PlayQueue.SortOrder order) {
        player.getPlayQueue().sort(order);
        broadcastPlayQueue(player);
    }

    private static final Function<PlayQueueInfo, PlayQueueInfo> identity = pq -> pq;
    private static final Function<PlayQueueInfo, PlayQueueInfo> startAt0 = pq -> pq.setStartPlayerAt(0);

    //
    // Methods dedicated to jukebox
    //
    public void setJukeboxGain(Player player, float gain) {
        jukeboxService.setGain(player, gain);
        runAsync(() -> brokerTemplate.convertAndSendToUser(player.getUsername(),
                "/queue/playqueues/" + player.getId() + "/jukebox/gain", gain));
    }

    public void setJukeboxPosition(Player player, int positionInSeconds) {
        jukeboxService.setPosition(player, positionInSeconds);
        runAsync(() -> brokerTemplate.convertAndSendToUser(player.getUsername(),
                "/queue/playqueues/" + player.getId() + "/jukebox/position", positionInSeconds));
    }

    //
    // End : Methods dedicated to jukebox
    //

    private void broadcastPlayQueue(Player player) {
        broadcastPlayQueue(player, identity, null);
    }

    private void broadcastPlayQueue(Player player, Function<PlayQueueInfo, PlayQueueInfo> playQueueModifier, String triggeringSessionId) {
        runAsync(() -> {
            PlayQueueInfo info = playQueueModifier.apply(getPlayQueueInfo(player));
            brokerTemplate.convertAndSendToUser(player.getUsername(),
                    "/queue/playqueues/" + player.getId() + "/updated", info);
            postBroadcast(info, player, triggeringSessionId);
        });
    }

    private void postBroadcast(PlayQueueInfo info, Player player, String sessionId) {
        if (info.getStartPlayerAt() != -1) {
            if (player.isWeb() && sessionId != null) {
                // trigger the web player to start playing at this location
                SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                headerAccessor.setSessionId(sessionId);
                brokerTemplate.convertAndSendToUser(player.getUsername(),
                        "/queue/playqueues/" + player.getId() + "/skip",
                        ImmutableMap.of("index", info.getStartPlayerAt(), "offset", info.getStartPlayerAtPosition()),
                        headerAccessor.getMessageHeaders());
            } else if (!player.isExternalWithPlaylist()) {
                skip(player, info.getStartPlayerAt(), info.getStartPlayerAtPosition());
            }
        }
    }

    public PlayQueueInfo getPlayQueueInfo(Player player) {
        PlayQueue playQueue = player.getPlayQueue();

        List<MediaFileEntry> entries;
        if (playQueue.isInternetRadioEnabled()) {
            entries = convertInternetRadio(player);
        } else {
            entries = convertMediaFileList(player);
        }

        float gain = jukeboxService.getGain(player);

        return new PlayQueueInfo(entries, playQueue.getStatus(), playQueue.getRepeatStatus(), playQueue.isShuffleRadioEnabled(), playQueue.isInternetRadioEnabled(), gain);
    }

    private List<MediaFileEntry> convertMediaFileList(Player player) {
        String url = ""; // NetworkService.getBaseUrl(request);
        Function<MediaFile, String> streamUrlGenerator = file -> url + "stream?player=" + player.getId() + "&id="
                + file.getId();
        Function<MediaFile, String> remoteStreamUrlGenerator = file -> jwtSecurityService
                .addJWTToken(player.getUsername(), url + "ext/stream?player=" + player.getId() + "&id=" + file.getId());
        Function<MediaFile, String> remoteCoverArtUrlGenerator = file -> jwtSecurityService
                .addJWTToken(player.getUsername(), url + "ext/coverArt.view?id=" + file.getId());
        return mediaFileService.toMediaFileEntryList(player.getPlayQueue().getFiles(), player.getUsername(), true, true,
                streamUrlGenerator, remoteStreamUrlGenerator, remoteCoverArtUrlGenerator);
    }

    private List<MediaFileEntry> convertInternetRadio(Player player) {
        PlayQueue playQueue = player.getPlayQueue();
        InternetRadio radio = playQueue.getInternetRadio();

        final String radioHomepageUrl = radio.getHomepageUrl();
        final String radioName = radio.getName();

        List<InternetRadioSource> sources = internetRadioService.getInternetRadioSources(radio);
        List<MediaFileEntry> entries = new ArrayList<>(sources.size());
        for (InternetRadioSource streamSource : sources) {
            // Fake entry id so that the source can be selected in the UI
            int streamId = -(1 + entries.size());
            Integer streamTrackNumber = entries.size();
            String streamUrl = streamSource.getStreamUrl();
            entries.add(new MediaFileEntry(streamId, // Entry id
                    streamTrackNumber, // Track number
                    streamUrl, // Track title (use radio stream URL for now)
                    "", // Track artist
                    radioName, // Album name (use radio name)
                    "Internet Radio", // Genre
                    0, // Year
                    "", // Bit rate
                    null, // Dimensions
                    0.0, // Duration
                    "", // Format
                    "", // Content Type
                    "", // Entry Type
                    "", // File size
                    false, // Starred
                    true, // Present
                    radioHomepageUrl, // Album URL (use radio home page URL)
                    streamUrl, // Stream URL
                    streamUrl, // Remote stream URL
                    null, // Cover art URL
                    null // Remote cover art URL
            ));
        }

        return entries;
    }
}
