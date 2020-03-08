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
package org.airsonic.player.controller;

import com.google.common.io.ByteStreams;

import org.airsonic.player.domain.*;
import org.airsonic.player.io.PipeStreams.MonitoredInputStream;
import org.airsonic.player.io.PipeStreams.PipedInputStream;
import org.airsonic.player.io.PipeStreams.PipedOutputStream;
import org.airsonic.player.io.PlayQueueInputStream;
import org.airsonic.player.io.ShoutCastOutputStream;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.*;
import org.airsonic.player.service.sonos.SonosHelper;
import org.airsonic.player.spring.KnownLengthInputStreamResource;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.LambdaUtils;
import org.airsonic.player.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.http.HttpServletRequest;

import java.awt.*;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A controller which streams the content of a {@link PlayQueue} to a remote
 * {@link Player}.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({ "/stream/**", "/ext/stream/**" })
public class StreamController {

    private static final Logger LOG = LoggerFactory.getLogger(StreamController.class);

    @Autowired
    private StatusService statusService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private AudioScrobblerService audioScrobblerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SearchService searchService;

    @GetMapping
    public ResponseEntity<Resource> handleRequest(Authentication authentication,
            @RequestParam(required = false) Integer playlist,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String suffix,
            @RequestParam Optional<Integer> maxBitRate,
            @RequestParam Optional<Integer> id,
            @RequestParam Optional<String> path,
            @RequestParam(defaultValue = "false") boolean hls,
            @RequestParam(required = false) Double offsetSeconds,
            ServletWebRequest swr) throws Exception {
        Player player = playerService.getPlayer(swr.getRequest(), swr.getResponse(), false, true);
        User user = securityService.getUserByName(player.getUsername());

        if (!(authentication instanceof JWTAuthenticationToken) && !user.isStreamRole()) {
            throw new AccessDeniedException("Streaming is forbidden for user " + user.getUsername());
        }

        Long expectedSize = null;

        // If "playlist" request parameter is set, this is a Podcast request. In that case, create a separate
        // play queue (in order to support multiple parallel Podcast streams).
        boolean isPodcast = playlist != null;
        if (isPodcast) {
            PlayQueue playQueue = new PlayQueue();
            playQueue.addFiles(false, playlistService.getFilesInPlaylist(playlist));
            player.setPlayQueue(playQueue);
            // Note: does not take transcoding into account
            expectedSize = playQueue.length();
            LOG.info("{}: Incoming Podcast request for playlist {}", swr.getRequest().getRemoteAddr(), playlist);
        }

//        response.setHeader("Access-Control-Allow-Origin", "*");

        String targetFormat = hls ? "ts" : format;
        Integer bitRate = maxBitRate.filter(x -> x != 0).orElse(null);

        VideoTranscodingSettings videoTranscodingSettings = null;

        // Is this a request for a single file (typically from the embedded Flash player)?
        // In that case, create a separate playlist (in order to support multiple parallel streams).
        // Also, enable partial download (HTTP byte range).
        MediaFile file = path.map(mediaFileService::getMediaFile)
                .orElseGet(() -> id.map(mediaFileService::getMediaFile).orElse(null));
        boolean isSingleFile = file != null;

        Long byteOffset = null;

        if (isSingleFile) {

            if (!(authentication instanceof JWTAuthenticationToken)
                    && !securityService.isFolderAccessAllowed(file, user.getUsername())) {
                throw new AccessDeniedException("Access to file " + file.getId() + " is forbidden for user " + user.getUsername());
            }

            // Update the index of the currently playing media file. At
            // this point we haven't yet modified the play queue to support
            // multiple streams, so the current play queue is the real one.
            int currentIndex = player.getPlayQueue().getFiles().indexOf(file);
            player.getPlayQueue().setIndex(currentIndex);

            // Create a new, fake play queue that only contains the
            // currently playing media file, in case multiple streams want
            // to use the same player.
            PlayQueue playQueue = new PlayQueue();
            playQueue.addFiles(true, file);
            player.setPlayQueue(playQueue);

            if (file.isVideo() || hls) {
                videoTranscodingSettings = createVideoTranscodingSettings(file, swr.getRequest());
            }

            TranscodingService.Parameters parameters = transcodingService.getParameters(file, player, bitRate,
                    targetFormat, videoTranscodingSettings);

            // Support ranges as long as we're not transcoding blindly; video is always
            // assumed to transcode
            expectedSize = file.isVideo() || !parameters.isRangeAllowed() ? null : parameters.getExpectedLength();

            // roughly adjust for offset seconds
            if (expectedSize != null && expectedSize > 0 && offsetSeconds != null && offsetSeconds > 0 && file.getDuration() != null) {
                byteOffset = Math.round(expectedSize * offsetSeconds / file.getDuration());
                expectedSize = Math.max(0, expectedSize - byteOffset);
            }

            if (swr.checkNotModified(
                    Optional.ofNullable(expectedSize).map(String::valueOf).orElse(null),
                    file.getChanged().toEpochMilli())) {
                return null;
            }

            // Set content type of response
            suffix = transcodingService.getSuffix(player, file, targetFormat);
        }

        // Terminate any other streams to this player.
        if (!isPodcast && !isSingleFile) {
            statusService.getStreamStatusesForPlayer(player).forEach(TransferStatus::terminate);
        }

        // If playqueue is in auto-random mode, populate it with new random songs.
        if (player.getPlayQueue().getIndex() == -1 && player.getPlayQueue().getRandomSearchCriteria() != null) {
            player.getPlayQueue().addFiles(false, searchService.getRandomSongs(player.getPlayQueue().getRandomSearchCriteria()));
            LOG.info("Recreated random playlist with {} songs.", player.getPlayQueue().size());
        }

        VideoTranscodingSettings videoTranscodingSettingsF = videoTranscodingSettings;
        TransferStatus status = statusService.createStreamStatus(player);

        Consumer<MediaFile> fileStartListener = mediaFile -> {
            LOG.info("{}: {} listening to {}", player.getIpAddress(), player.getUsername(), FileUtil.getShortPath(mediaFile.getFile()));
            mediaFileService.incrementPlayCount(mediaFile);
            scrobble(mediaFile, player, false);
            status.setFile(mediaFile.getFile());
            statusService.addActiveLocalPlay(
                    new PlayStatus(status.getId(), mediaFile, player, status.getMillisSinceLastUpdate()));
        };
        Consumer<MediaFile> fileEndListener = mediaFile -> {
            scrobble(mediaFile, player, true);
            statusService.removeActiveLocalPlay(
                    new PlayStatus(status.getId(), mediaFile, player, status.getMillisSinceLastUpdate()));
        };
        Function<MediaFile, InputStream> streamGenerator = LambdaUtils.uncheckFunction(
            mediaFile -> transcodingService.getTranscodedInputStream(
                    transcodingService.getParameters(mediaFile, player, bitRate, targetFormat, videoTranscodingSettingsF)));

        HttpHeaders headers = new HttpHeaders();
        InputStream playStream = new PlayQueueInputStream(player.getPlayQueue(), fileStartListener, fileEndListener, streamGenerator);
        BiConsumer<InputStream, TransferStatus> streamInit = (i, s) -> {};

        // Enabled SHOUTcast, if requested.
        if ("1".equals(swr.getHeader("icy-metadata"))) {
            expectedSize = null;
            ShoutcastDetails shoutcastDetails = getShoutcastDetails(playStream);
            playStream = shoutcastDetails.getStream();
            streamInit = shoutcastDetails.getStreamInit();
            headers.addAll(shoutcastDetails.getHeaders());
        }

        // Deal with offset seconds by skipping over bytes from the underlying stream
        // Note that if Range and offsetSeconds both come in request, then first
        // offsetSeconds skips over a certain length, then Range bytes are allocated,
        // so Range of 0 will start from bytesSkipped
        if (byteOffset != null) {
            BiConsumer<InputStream, TransferStatus> streamInitF = streamInit;
            Long byteOffsetF = byteOffset;
            streamInit = LambdaUtils.uncheckBiConsumer((i, s) -> {
                streamInitF.accept(i, s);
                s.addBytesSkipped(i.skip(byteOffsetF));
            });
        }

        if (expectedSize != null) {
            playStream = new ThresholdInputStream(playStream, expectedSize);
        }

        Supplier<TransferStatus> statusSupplier = () -> status;
        Consumer<TransferStatus> statusCloser = s -> {
            securityService.updateUserByteCounts(user, s.getBytesTransferred(), 0L, 0L);
            statusService.removeStreamStatus(s);
        };

        InputStream monitoredStream = new MonitoredInputStream(
                playStream,
                settingsService.getDownloadBitrateLimiter(),
                statusSupplier, statusCloser,
                streamInit);

        Resource resource = expectedSize == null ?
                new InputStreamResource(monitoredStream) :
                new KnownLengthInputStreamResource(monitoredStream, expectedSize);

        boolean sonos = SonosHelper.AIRSONIC_CLIENT_ID.equals(player.getClientId());
        headers.setContentType(MediaType.parseMediaType(StringUtil.getMimeType(suffix, sonos)));

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private void scrobble(MediaFile mediaFile, Player player, boolean submission) {
        // Don't scrobble REST players (except Sonos)
        if (player.getClientId() == null || player.getClientId().equals(SonosHelper.AIRSONIC_CLIENT_ID)) {
            audioScrobblerService.register(mediaFile, player.getUsername(), submission, null);
        }
    }

    private ShoutcastDetails getShoutcastDetails(InputStream input) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("icy-metaint", String.valueOf(ShoutCastOutputStream.META_DATA_INTERVAL));
        responseHeaders.set("icy-notice1", "This stream is served using Airsonic");
        responseHeaders.set("icy-notice2", "Airsonic - Free media streamer");
        responseHeaders.set("icy-name", "Airsonic");
        responseHeaders.set("icy-genre", "Mixed");
        responseHeaders.set("icy-url", "https://airsonic.github.io/");

        return new ShoutcastDetails(new PipedInputStream(), (i, s) -> {
            PipedInputStream pin = (PipedInputStream) i;

            // start a new thread to feed data in
            new Thread(() -> {
                try (InputStream in = input;
                        PipedOutputStream pout = new PipedOutputStream(pin);
                        ShoutCastOutputStream shout = new ShoutCastOutputStream(pout,
                            () -> Optional.ofNullable(s).map(TransferStatus::getFile).map(Path::toString)
                                    .orElseGet(settingsService::getWelcomeTitle))) {
                    // IOUtils.copy(playStream, shout);
                    ByteStreams.copy(in, shout);
                    // StreamUtils.copy(playStream, shout);
                } catch (Exception e) {
                    LOG.debug("Error with output to Shoutcast stream", e);
                }
            }, "ShoutcastStreamDatafeed").start();

            // wait for src data thread to connect
            while (pin.source == null) {
                // sit and wait and ponder life
            }
        }, responseHeaders);
    }

    private static class ShoutcastDetails {
        private final InputStream stream;
        private final BiConsumer<InputStream, TransferStatus> streamInit;
        private final HttpHeaders headers;

        public ShoutcastDetails(InputStream stream, BiConsumer<InputStream, TransferStatus> streamInit,
                HttpHeaders headers) {
            this.stream = stream;
            this.streamInit = streamInit;
            this.headers = headers;
        }

        public InputStream getStream() {
            return stream;
        }

        public BiConsumer<InputStream, TransferStatus> getStreamInit() {
            return streamInit;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }
    }

    /**
     *
     * Class that ensures a stream meets at least minimum length. If underlying
     * stream is shorter, then this will pad stream with zeros until threshold is
     * met If underlying stream is longer, then this will warn as soon as it goes
     * over.
     *
     */
    private static class ThresholdInputStream extends FilterInputStream {
        private long threshold;
        private long bytesRead = 0;

        protected ThresholdInputStream(InputStream in, long threshold) {
            super(in);
            this.threshold = threshold;
        }

        private void warn() {
            LOG.warn("Stream output exceeded expected length of {}. It is likely that "
                    + "the transcoder is not adhering to the bitrate limit or the media "
                    + "source is corrupted or has grown larger", threshold);
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read == -1 && bytesRead < threshold) {
                read = 0;
            } else if (read != -1 && bytesRead == threshold) {
                warn();
            }

            bytesRead++;

            return read;
        }

        private static byte[] zeros = new byte[4096];

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read == -1 && bytesRead < threshold) {
                if (zeros.length < len) {
                    zeros = new byte[len];
                }
                System.arraycopy(zeros, 0, b, off, len);
                read = Math.min(len, (int) (threshold - bytesRead));
            } else if (read != -1 && bytesRead <= threshold && bytesRead + read > threshold) {
                warn();
            }

            bytesRead += read;

            return read;
        }

    }

    private VideoTranscodingSettings createVideoTranscodingSettings(MediaFile file, HttpServletRequest request)
            throws ServletRequestBindingException {
        Integer existingWidth = file.getWidth();
        Integer existingHeight = file.getHeight();
        Integer maxBitRate = ServletRequestUtils.getIntParameter(request, "maxBitRate");
        int timeOffset = ServletRequestUtils.getIntParameter(request, "timeOffset", 0);
        double defaultDuration = file.getDuration() == null ? Double.MAX_VALUE : file.getDuration() - timeOffset;
        double duration = ServletRequestUtils.getDoubleParameter(request, "duration", defaultDuration);
        boolean hls = ServletRequestUtils.getBooleanParameter(request, "hls", false);

        Dimension dim = getRequestedVideoSize(request.getParameter("size"));
        if (dim == null) {
            dim = getSuitableVideoSize(existingWidth, existingHeight, maxBitRate);
        }

        return new VideoTranscodingSettings(dim.width, dim.height, timeOffset, duration, hls);
    }

    protected Dimension getRequestedVideoSize(String sizeSpec) {
        if (sizeSpec == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("^(\\d+)x(\\d+)$");
        Matcher matcher = pattern.matcher(sizeSpec);
        if (matcher.find()) {
            int w = Integer.parseInt(matcher.group(1));
            int h = Integer.parseInt(matcher.group(2));
            if (w >= 0 && h >= 0 && w <= 2000 && h <= 2000) {
                return new Dimension(w, h);
            }
        }
        return null;
    }

    protected Dimension getSuitableVideoSize(Integer existingWidth, Integer existingHeight, Integer maxBitRate) {
        if (maxBitRate == null) {
            return new Dimension(400, 224);
        }

        int w;
        if (maxBitRate < 400) {
            w = 400;
        } else if (maxBitRate < 600) {
            w = 480;
        } else if (maxBitRate < 1800) {
            w = 640;
        } else {
            w = 960;
        }
        int h = even(w * 9 / 16);

        if (existingWidth == null || existingHeight == null) {
            return new Dimension(w, h);
        }

        if (existingWidth < w || existingHeight < h) {
            return new Dimension(even(existingWidth), even(existingHeight));
        }

        double aspectRate = existingWidth.doubleValue() / existingHeight.doubleValue();
        h = (int) Math.round(w / aspectRate);

        return new Dimension(even(w), even(h));
    }

    // Make sure width and height are multiples of two, as some versions of ffmpeg
    // require it.
    private int even(int size) {
        return size + (size % 2);
    }
}
