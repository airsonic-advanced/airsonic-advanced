package org.airsonic.player.service.hls;

import com.google.common.io.MoreFiles;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.VideoTranscodingSettings;
import org.airsonic.player.io.InputStreamReaderThread;
import org.airsonic.player.io.TranscodeInputStream;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.TranscodingService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HlsSession {
    private final Logger LOG;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(Util.getDaemonThreadfactory("hls-destroy-session"));

    private static final long SESSION_TIMEOUT_SECONDS = 120L;

    private final Key sessionKey;

    private final MediaFile mediaFile;

    private final TranscodingService transcodingService;

    private Process process;

    private ScheduledFuture<?> destroySessionFuture;

    public HlsSession(Key sessionKey, MediaFile mediaFile, TranscodingService transcodingService) {
        this.LOG = LoggerFactory.getLogger(HlsSession.class.toString() + "-" + sessionKey.id());
        this.LOG.info("Creating HLS session {}: {}", sessionKey.id(), sessionKey);
        this.sessionKey = sessionKey;
        this.mediaFile = mediaFile;
        this.transcodingService = transcodingService;
    }

    public Path waitForSegment(int segmentIndex, long timeoutMillis) throws Exception {
        this.LOG.debug("Requesting hls segment {}", segmentIndex);
        scheduleSessionDestruction();
        Path segment = getSegment(segmentIndex);
        if (segment != null) {
            this.LOG.debug("Segment {} already produced.", segmentIndex);
            return segment;
        }
        if (!isProcessAlive()) {
            startProcess(segmentIndex);
        } else {
            Integer latestCompleted = getLatestCompletedSegmentIndex();
            if (latestCompleted != null
                    && (segmentIndex < latestCompleted.intValue() || segmentIndex > latestCompleted.intValue() + 2)) {
                destroySession();
                startProcess(segmentIndex);
            }
        }
        long timeout = currentTimeMillis() + timeoutMillis;
        while (segment == null && currentTimeMillis() < timeout && isProcessAlive()) {
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
        this.LOG.debug("Destroying hls session");
        killProcess();
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
            if (!result.isEmpty() && isProcessAlive()) {
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

    private void startProcess(int segmentIndex) throws IOException {
        String[] size = StringUtils.split(this.sessionKey.getSize(), "x");
        VideoTranscodingSettings vts = new VideoTranscodingSettings(
                Integer.valueOf(size[0]), Integer.valueOf(size[1]),
                segmentIndex * this.sessionKey.getDuration(), this.sessionKey.getDuration(),
                (this.sessionKey.getAudioTrack() == null) ? 1 : this.sessionKey.getAudioTrack(), segmentIndex,
                getDirectory().resolve("%d.ts").toString(), getDirectory().resolve("out.m3u8").toString());
        TranscodingService.Parameters parameters = transcodingService.getParameters(mediaFile, null, this.sessionKey.getMaxBitRate(), "ts", vts);
        TranscodeInputStream in = (TranscodeInputStream) transcodingService.getTranscodedInputStream(parameters);

        process = in.getProcess();
        (new InputStreamReaderThread(process.getInputStream(), getClass().getSimpleName(), true)).start();
    }

    private void killProcess() {
        if (this.process != null) {
            this.LOG.info("Killing hls process");
            try {
                this.process.destroy();
            } catch (Exception e) {
                this.LOG.error("Failed to kill hls process", e);
            }
        }
    }

    private boolean isProcessAlive() {
        if (this.process == null)
            return false;
        try {
            this.process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
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
