package org.airsonic.player.service;

import com.github.biconou.AudioPlayer.api.PlayList;
import com.github.biconou.AudioPlayer.api.PlayerListener;
import org.airsonic.player.domain.*;
import org.airsonic.player.service.jukebox.JavaPlayerFactory;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioSystem;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rémi Cocula
 */
@Service
public class JukeboxJavaService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JukeboxJavaService.class);

    private static final float DEFAULT_GAIN = 0.75f;

    private AudioScrobblerService audioScrobblerService;
    private StatusService statusService;
    private SecurityService securityService;
    private MediaFileService mediaFileService;
    private JavaPlayerFactory javaPlayerFactory;

    private TransferStatus status;
    private PlayStatus playStatus;
    private Map<Integer, Pair<com.github.biconou.AudioPlayer.api.Player, String>> activeAudioPlayers = new ConcurrentHashMap<>();
    private Map<String, List<com.github.biconou.AudioPlayer.api.Player>> activeAudioPlayersPerMixer = new ConcurrentHashMap<>();


    public JukeboxJavaService(AudioScrobblerService audioScrobblerService,
                              StatusService statusService,
                              SecurityService securityService,
                              MediaFileService mediaFileService,
                              JavaPlayerFactory javaPlayerFactory) {
        this.audioScrobblerService = audioScrobblerService;
        this.statusService = statusService;
        this.securityService = securityService;
        this.mediaFileService = mediaFileService;
        this.javaPlayerFactory = javaPlayerFactory;
    }

    /**
     * Finds the corresponding active audio player for a given airsonic player.
     * If no player exists we create one.
     * The JukeboxJavaService references all active audio players in a map indexed by airsonic player id.
     *
     * @param airsonicPlayer a given airsonic player.
     * @return the corresponding active audio player.
     */
    private com.github.biconou.AudioPlayer.api.Player retrieveAudioPlayerForAirsonicPlayer(Player airsonicPlayer) {
        return activeAudioPlayers.compute(airsonicPlayer.getId(), (id, pair) -> {
            if (pair == null) {
                pair = initAudioPlayer(airsonicPlayer);
            } else if (!StringUtils.equals(pair.getRight(), getMixer(airsonicPlayer.getJavaJukeboxMixer()))) {
                // mixer has changed, remove old one and create new one
                pair.getLeft().close();
                activeAudioPlayersPerMixer.getOrDefault(pair.getRight(), Collections.emptyList()).remove(pair.getLeft());

                pair = initAudioPlayer(airsonicPlayer);
            }

            return pair;
        }).getLeft();
    }

    private Pair<com.github.biconou.AudioPlayer.api.Player, String> initAudioPlayer(final Player airsonicPlayer) {

        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }

        log.info("begin initAudioPlayer");

        com.github.biconou.AudioPlayer.api.Player audioPlayer = null;

        String mixer = getMixer(airsonicPlayer.getJavaJukeboxMixer());
        System.out.println("**********MIXERS:" + Util.debugObject(AudioSystem.getMixerInfo()));
        System.out.println("**********SPECMIXER:" + airsonicPlayer.getJavaJukeboxMixer());
        System.out.println("**********MIXER:" + mixer);

        if (mixer != null) {
            log.info("use mixer : {}", mixer);
            audioPlayer = javaPlayerFactory.createJavaPlayer(mixer);
        }

        if (audioPlayer != null) {
            audioPlayer.setGain(DEFAULT_GAIN);
            audioPlayer.registerListener(new PlayerListener() {
                @Override
                public void onBegin(int index, File currentFile) {
                    onSongStart(airsonicPlayer);
                }

                @Override
                public void onEnd(int index, File file) {
                    onSongEnd(airsonicPlayer);
                }

                @Override
                public void onFinished() {
                    airsonicPlayer.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onStop() {
                    airsonicPlayer.getPlayQueue().setStatus(PlayQueue.Status.STOPPED);
                }

                @Override
                public void onPause() {
                    // Nothing to do here
                }
            });
            log.info("New audio player {} has been initialized.", audioPlayer.toString());
        } else {
            throw new RuntimeException("AudioPlayer has not been initialized properly");
        }
        activeAudioPlayersPerMixer.computeIfAbsent(mixer, k -> Collections.synchronizedList(new ArrayList<>())).add(audioPlayer);

        return ImmutablePair.of(audioPlayer, mixer);
    }


    public int getPosition(final Player airsonicPlayer) {

        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (audioPlayer == null) {
            return 0;
        } else {
            return audioPlayer.getPlayingInfos().currentAudioPositionInSeconds();
        }
    }

    public void setPosition(final Player airsonicPlayer, int positionInSeconds) {
        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        if (audioPlayer != null) {
            audioPlayer.setPos(positionInSeconds);
        } else {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " has no real audio player");
        }
    }

    public float getGain(final Player airsonicPlayer) {
        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        return audioPlayer.getGain();
    }

    public void setGain(final Player airsonicPlayer, final float gain) {
        if (!airsonicPlayer.getTechnology().equals(PlayerTechnology.JAVA_JUKEBOX)) {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " is not a java jukebox player");
        }
        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);
        log.debug("setGain : gain={}", gain);
        if (audioPlayer != null) {
            audioPlayer.setGain(gain);
        } else {
            throw new RuntimeException("The player " + airsonicPlayer.getName() + " has no real audio player");
        }
    }


    private void onSongStart(Player player) {
        MediaFile file = player.getPlayQueue().getCurrentFile();
        log.info("[onSongStart] {} starting jukebox for \"{}\"", player.getUsername(), FileUtil.getShortPath(file.getFile()));
        if (playStatus != null) {
            statusService.removeActiveLocalPlay(playStatus);
            playStatus = null;
        }
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        status = statusService.createStreamStatus(player);
        status.setFile(file.getFile());
        status.addBytesTransferred(file.getFileSize());
        mediaFileService.incrementPlayCount(file);
        playStatus = new PlayStatus(status.getId(), file, status.getPlayer(), status.getMillisSinceLastUpdate());
        statusService.addActiveLocalPlay(playStatus);
        scrobble(player, file, false);
    }

    private void onSongEnd(Player player) {
        MediaFile file = player.getPlayQueue().getCurrentFile();
        log.info("[onSongEnd] {} stopping jukebox for \"{}\"", player.getUsername(), FileUtil.getShortPath(file.getFile()));
        if (playStatus != null) {
            statusService.removeActiveLocalPlay(playStatus);
            playStatus = null;
        }
        if (status != null) {
            statusService.removeStreamStatus(status);
            status = null;
        }
        scrobble(player, file, true);
    }

    private void scrobble(Player player, MediaFile file, boolean submission) {
        if (player.getClientId() == null) {  // Don't scrobble REST players.
            audioScrobblerService.register(file, player.getUsername(), submission, null);
        }
    }

    private static String getMixer(String mixer) {
        return Optional.ofNullable(mixer)
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> AudioSystem.getMixerInfo().length > 0 ? AudioSystem.getMixerInfo()[0].getName() : null);
    }

    /**
     * Plays the playqueue of a jukebox player starting at the beginning.
     */
    public void play(Player airsonicPlayer) {
        log.debug("begin play jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        log.debug("Different file to play -> start a new play list");
        if (airsonicPlayer.getPlayQueue().getCurrentFile() != null) {
            audioPlayer.setPlayList(new PlayList() {

                @Override
                public File getNextAudioFile() {
                    airsonicPlayer.getPlayQueue().next();
                    return getCurrentAudioFile();
                }

                @Override
                public File getCurrentAudioFile() {
                    MediaFile current = airsonicPlayer.getPlayQueue().getCurrentFile();
                    if (current != null) {
                        return airsonicPlayer.getPlayQueue().getCurrentFile().getFile().toFile();
                    } else {
                        return null;
                    }
                }

                @Override
                public int getSize() {
                    return airsonicPlayer.getPlayQueue().size();
                }

                @Override
                public int getIndex() {
                    return airsonicPlayer.getPlayQueue().getIndex();
                }
            });

            // Close any other player using the same mixer.
            String mixer = getMixer(airsonicPlayer.getJavaJukeboxMixer());
            List<com.github.biconou.AudioPlayer.api.Player> playersForSameMixer = activeAudioPlayersPerMixer.get(mixer);
            synchronized (playersForSameMixer) {
                playersForSameMixer.forEach(player -> {
                    if (player != audioPlayer) {
                        player.close();
                    }
                });
            }
            audioPlayer.play();
        }
    }

    public void start(Player airsonicPlayer) {
        play(airsonicPlayer);
    }

    public void stop(Player airsonicPlayer) {
        log.debug("begin stop jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        log.debug("PlayQueue.Status is {}", airsonicPlayer.getPlayQueue().getStatus());
        audioPlayer.pause();
    }

    public void skip(Player airsonicPlayer, int index, int offset) {
        log.debug("begin skip jukebox : player = id:{};name:{}", airsonicPlayer.getId(), airsonicPlayer.getName());

        com.github.biconou.AudioPlayer.api.Player audioPlayer = retrieveAudioPlayerForAirsonicPlayer(airsonicPlayer);

        // Control user authorizations
        User user = securityService.getUserByName(airsonicPlayer.getUsername());
        if (!user.isJukeboxRole()) {
            log.warn("{} is not authorized for jukebox playback.", user.getUsername());
            return;
        }

        if (index == 0 && offset == 0) {
            play(airsonicPlayer);
        } else {
            if (offset == 0) {
                audioPlayer.stop();
                audioPlayer.play();
            } else {
                audioPlayer.setPos(offset);
            }
        }
    }
}
