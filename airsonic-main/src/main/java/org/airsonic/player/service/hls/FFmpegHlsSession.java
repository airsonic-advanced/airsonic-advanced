package org.airsonic.player.service.hls;

import com.google.common.io.MoreFiles;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.io.InputStreamReaderThread;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FFmpegHlsSession {
    private final Logger LOG;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final long SESSION_TIMEOUT_SECONDS = 120L;

    private final Key sessionKey;

    private final MediaFile mediaFile;

    private Process ffmpegProcess;

    private ScheduledFuture<?> destroySessionFuture;

    public FFmpegHlsSession(Key sessionKey, MediaFile mediaFile) {
        this.LOG = LoggerFactory.getLogger(FFmpegHlsSession.class.toString() + "-" + sessionKey.id());
        this.LOG.info("Creating FFmpeg HLS session {}: {}", sessionKey.id(), sessionKey);
        this.sessionKey = sessionKey;
        this.mediaFile = mediaFile;
    }

    public Path waitForSegment(int segmentIndex, long timeoutMillis) throws Exception {
        this.LOG.debug("Requesting segment {}", segmentIndex);
        scheduleSessionDestruction();
        Path segment = getSegment(segmentIndex);
        if (segment != null) {
            this.LOG.debug("Segment {} already produced.", segmentIndex);
            return segment;
        }
        if (!isFFmpegAlive()) {
            startFFmpeg(segmentIndex);
        } else {
            Integer latestCompleted = getLatestCompletedSegmentIndex();
            if (latestCompleted != null
                    && (segmentIndex < latestCompleted.intValue() || segmentIndex > latestCompleted.intValue() + 2)) {
                destroySession();
                startFFmpeg(segmentIndex);
            }
        }
        long timeout = currentTimeMillis() + timeoutMillis;
        while (segment == null && currentTimeMillis() < timeout && isFFmpegAlive()) {
            this.LOG.debug("Segment {} not yet produced. Waiting.", segmentIndex);
            Thread.sleep(2000L);
            segment = getSegment(segmentIndex);
        }
        if (segment != null) {
            this.LOG.debug("Segment {} produced.", segmentIndex);
        } else {
            this.LOG.warn("Timed out for segment {}", segmentIndex);
        }
        return segment;
    }

    private synchronized void scheduleSessionDestruction() {
        if (this.destroySessionFuture != null)
            this.destroySessionFuture.cancel(false);
        this.destroySessionFuture = EXECUTOR.schedule(() -> this.destroySession(), SESSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void destroySession() {
        this.LOG.debug("Destroying session");
        killFFmpeg();
        FileUtil.delete(getDirectory());
    }

    private Path getSegment(int segmentIndex) {
        SortedSet<Integer> segmentIndexes = getCompletedSegmentIndexes();
        return segmentIndexes.contains(Integer.valueOf(segmentIndex)) ? getDirectory().resolve(segmentIndex + ".ts") : null;
    }

    private Integer getLatestCompletedSegmentIndex() {
        SortedSet<Integer> completed = getCompletedSegmentIndexes();
        return completed.isEmpty() ? null : completed.last();
    }

    private SortedSet<Integer> getCompletedSegmentIndexes() {
        try (Stream<Path> children = Files.list(getDirectory())) {
            SortedSet<Integer> result = children.filter(Files::isRegularFile)
                    .filter(c -> "ts".equals(MoreFiles.getFileExtension(c))).map(MoreFiles::getNameWithoutExtension)
                    .map(Integer::valueOf).collect(Collectors.toCollection(() -> new TreeSet<>()));
            if (!result.isEmpty() && isFFmpegAlive()) {
                result.remove(result.last());
            }
            return result;
        } catch (IOException e) {
            LOG.warn("Could not retrieve directory list for {} to find segment files", getDirectory(), e);

            return Collections.emptySortedSet();
        }
    }

    private Path getDirectory() {
        Path dir = getHlsRootDirectory().resolve(this.sessionKey.id());
        if (!Files.exists(dir)) {
            try {
                dir = Files.createDirectories(dir);
                LOG.info("Created hls cache {}", dir);
            } catch (Exception e) {
                LOG.error("Failed to create hls cache {}", dir, e);
            }
        }

        return dir;
    }

    public static Path getHlsRootDirectory() {
        return SettingsService.getAirsonicHome().resolve("hls");
    }

    private void startFFmpeg(int segmentIndex) throws IOException {
        List<String> command = new ArrayList<String>();
        int peakVideoBitRate = this.sessionKey.getMaxBitRate();
        int averageVideoBitRate = getAverageVideoBitRate(peakVideoBitRate);
        int audioBitRate = getSuitableAudioBitRate(peakVideoBitRate);
        int audioTrack = (this.sessionKey.getAudioTrack() == null) ? 1 : this.sessionKey.getAudioTrack().intValue();
        command.add(SettingsService.resolveTranscodeExecutable("ffmpeg"));
        if (segmentIndex > 0) {
            command.add("-ss");
            command.add(String.valueOf(segmentIndex * this.sessionKey.getDuration()));
        }
        command.add("-i");
        command.add(this.mediaFile.getPath());
        command.add("-s");
        command.add(this.sessionKey.getSize());
        command.add("-c:v");
        command.add("libx264");
        command.add("-flags");
        command.add("+cgop");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:v");
        command.add(averageVideoBitRate + "k");
        command.add("-maxrate");
        command.add(peakVideoBitRate + "k");
        command.add("-b:a");
        command.add(audioBitRate + "k");
        command.add("-bufsize");
        command.add("256k");
        command.add("-map");
        command.add("0:0");
        command.add("-map");
        command.add("0:" + audioTrack);
        command.add("-ac");
        command.add("2");
        command.add("-preset");
        command.add("superfast");
        if (segmentIndex > 0)
            command.add("-copyts");
        // command.add("-pix_fmt yuv420p");
        command.add("-v");
        command.add("error");
        command.add("-force_key_frames");
        command.add("expr:gte(t,n_forced*10)");
        command.add("-start_number");
        command.add(String.valueOf(segmentIndex));
        command.add("-hls_time");
        command.add(this.sessionKey.getDuration().toString());
        command.add("-hls_list_size");
        command.add("0");
        command.add("-hls_segment_filename");
        command.add(getDirectory().resolve("%d.ts").toString());
        command.add(getDirectory().resolve("out.m3u8").toString());
        this.LOG.info("Starting ffmpeg for hls: {}", command);
        this.ffmpegProcess = (new ProcessBuilder(command)).redirectErrorStream(true).start();
        (new InputStreamReaderThread(this.ffmpegProcess.getInputStream(), getClass().getSimpleName(), true)).start();
    }

    private void killFFmpeg() {
        if (this.ffmpegProcess != null) {
            this.LOG.info("Killing ffmpeg");
            try {
                this.ffmpegProcess.destroy();
            } catch (Exception e) {
                this.LOG.error("Failed to kill ffmpeg", e);
            }
        }
    }

    private boolean isFFmpegAlive() {
        if (this.ffmpegProcess == null)
            return false;
        try {
            this.ffmpegProcess.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public static Dimension getSuitableVideoSize(Integer existingWidth, Integer existingHeight, int peakVideoBitRate) {
        int w;
        if (peakVideoBitRate < 400) {
            w = 416;
        } else if (peakVideoBitRate < 800) {
            w = 480;
        } else if (peakVideoBitRate < 1200) {
            w = 640;
        } else if (peakVideoBitRate < 2200) {
            w = 768;
        } else if (peakVideoBitRate < 3300) {
            w = 960;
        } else if (peakVideoBitRate < 8600) {
            w = 1280;
        } else {
            w = 1920;
        }
        int h = even(w * 9 / 16);
        if (existingWidth == null || existingHeight == null)
            return new Dimension(w, h);
        if (existingWidth.intValue() < w || existingHeight.intValue() < h)
            return new Dimension(even(existingWidth.intValue()), even(existingHeight.intValue()));
        double aspectRatio = existingWidth.doubleValue() / existingHeight.doubleValue();
        h = (int) Math.round(w / aspectRatio);
        return new Dimension(even(w), even(h));
    }

    private static int even(int size) {
        return size + size % 2;
    }

    public static int getSuitableAudioBitRate(int peakVideoBitRate) {
        if (peakVideoBitRate < 1200)
            return 64;
        if (peakVideoBitRate < 5000)
            return 96;
        return 128;
    }

    public static int getAverageVideoBitRate(int peakVideoBitRate) {
        switch (peakVideoBitRate) {
            case 200:
                return 145;
            case 400:
                return 365;
            case 800:
                return 730;
            case 1200:
                return 1100;
            case 2200:
                return 2000;
            case 3300:
                return 3000;
            case 5000:
                return 4500;
            case 6500:
                return 6000;
        }
        return (int) (peakVideoBitRate * 0.9D);
    }

    private long currentTimeMillis() {
        return System.nanoTime() / 1000000L;
    }

    public static class Key {
        private final int mediaFileId;
        private final String playerId;
        private final int maxBitRate;
        private final String size;
        private final Integer duration;
        private final Integer audioTrack;

        public Key(int mediaFileId, String playerId, int maxBitRate, String size, Integer duration, Integer audioTrack) {
            this.mediaFileId = mediaFileId;
            this.playerId = playerId;
            this.maxBitRate = maxBitRate;
            this.size = size;
            this.duration = duration;
            this.audioTrack = audioTrack;
        }

        public String id() {
            return String.valueOf(Math.abs(this.hashCode()));
        }

        public int getMediaFileId() {
            return this.mediaFileId;
        }

        public String getPlayerId() {
            return this.playerId;
        }

        public int getMaxBitRate() {
            return this.maxBitRate;
        }

        public String getSize() {
            return this.size;
        }

        public Integer getDuration() {
            return duration;
        }

        public Integer getAudioTrack() {
            return this.audioTrack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return this.mediaFileId == key.mediaFileId && Objects.equals(this.playerId, key.playerId)
                    && Objects.equals(this.maxBitRate, key.maxBitRate) && Objects.equals(this.size, key.size)
                    && Objects.equals(this.audioTrack, key.audioTrack) && Objects.equals(this.duration, key.duration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.mediaFileId, this.playerId, this.maxBitRate, this.size, this.duration, this.audioTrack);
        }

        @Override
        public String toString() {
            return "{mediaFileId=" + this.mediaFileId + ", playerId='" + this.playerId + '\'' + ", maxBitRate='"
                    + this.maxBitRate + '\'' + ", size='" + this.size + '\'' + ", duration='" + this.duration + '\''
                    + ", audioTrack=" + this.audioTrack + '}';
        }
    }
}
