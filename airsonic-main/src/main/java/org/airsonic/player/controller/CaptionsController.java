package org.airsonic.player.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.MoreFiles;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.User;
import org.airsonic.player.io.InputStreamReaderThread;
import org.airsonic.player.io.PipeStreams;
import org.airsonic.player.io.PipeStreams.PipedInputStream;
import org.airsonic.player.io.PipeStreams.PipedOutputStream;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/captions")
public class CaptionsController {
    private static final Logger LOG = LoggerFactory.getLogger(CaptionsController.class);

    private static final String CAPTION_FORMAT_VTT = "vtt";
    private static final String CAPTION_FORMAT_SRT = "srt";
    private static final Set<String> CAPTIONS_FORMATS = ImmutableSet.of(CAPTION_FORMAT_VTT, CAPTION_FORMAT_SRT);

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;

    @GetMapping
    public ResponseEntity<Resource> handleRequest(
            Authentication authentication,
            @RequestParam int id,
            @RequestParam(required = false) String captionId,
            @RequestParam(required = false, name = "format") String requiredFormat) throws Exception {

        User user = securityService.getUserByName(authentication.getName());
        MediaFile video = this.mediaFileService.getMediaFile(id);
        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(video, user.getUsername())) {
            throw new AccessDeniedException("Access to file " + id + " is forbidden for user " + user.getUsername());
        }

        List<CaptionInfo> captions = listCaptions(video);
        CaptionInfo res;
        if (captionId == null) {
            res = captions.stream().findFirst().orElse(null);
        } else {
            res = captions.stream().filter(c -> StringUtils.equalsIgnoreCase(captionId, c.getIdentifier())).findAny().orElse(null);
        }

        if (res == null) {
            throw new NotFoundException("No captions found for file id: " + id);
        }

        String effectiveFormat = requiredFormat != null ? requiredFormat : res.getFormat();

        Resource resource = null;
        Instant time = null;

        if (res.getLocation() == CaptionInfo.Location.external) {
            Path captionsFile = Paths.get(res.getIdentifier());

            if (effectiveFormat.equalsIgnoreCase(res.getFormat())) {
                resource = getExternalResource(captionsFile, res.getFormat());
            } else if ("srt".equals(res.getFormat()) && "vtt".equals(requiredFormat)) {
                Process process = new ProcessBuilder(
                        ImmutableList.of(SettingsService.getTranscodeDirectory().resolve("ffmpeg").toString(), "-i",
                                captionsFile.toString(), "-map", "0:0", "-f", getForceFormat(effectiveFormat), "-"))
                                        .start();

                resource = new InputStreamResource(process.getInputStream());

                // Must read stderr from the process, otherwise it may block.
                new InputStreamReaderThread(process.getErrorStream(), "ffmpeg-error-stream-" + UUID.randomUUID(), true)
                        .start();

                // resource = getConvertedResource(captionsFile);
            } else {
                throw new NotFoundException("No captions found for file id: " + id);
            }
            time = Files.getLastModifiedTime(captionsFile).toInstant();
        } else {
            Process process = new ProcessBuilder(
                    ImmutableList.of(SettingsService.getTranscodeDirectory().resolve("ffmpeg").toString(), "-i",
                            video.getFile().toString(), "-map",
                    "0:" + res.getIdentifier(), "-f", getForceFormat(effectiveFormat), "-")).start();

            resource = new InputStreamResource(process.getInputStream());

            // Must read stderr from the process, otherwise it may block.
            new InputStreamReaderThread(process.getErrorStream(), "ffmpeg-error-stream-" + UUID.randomUUID(), true).start();
            time = Files.getLastModifiedTime(video.getFile()).toInstant();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(CAPTION_FORMAT_VTT.equalsIgnoreCase(effectiveFormat)
                ? new MediaType("text", "vtt", StandardCharsets.UTF_8)
                : new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setAccessControlAllowOrigin("*");


        return ResponseEntity.ok()
                .lastModified(time)
                .headers(headers)
                .body(resource);
    }

    public static String getForceFormat(String format) {
        switch (format) {
            case "vtt":
                return "webvtt";
            default:
                return format;
        }
    }

    public static String getDisplayFormat(String format) {
        switch (format) {
            case "webvtt":
                return "vtt";
            case "subrip":
                return "srt";
            default:
                return format;
        }
    }

    @GetMapping("/list")
    public @ResponseBody List<CaptionInfo> listCaptions(@RequestParam int id) {
        return listCaptions(mediaFileService.getMediaFile(id));
    }

    public List<CaptionInfo> listCaptions(MediaFile video) {
        MetaData metaData = getVideoMetaData(video);
        Stream<CaptionInfo> internalCaptions;
        if (metaData == null || metaData.getSubtitleTracks().isEmpty()) {
            internalCaptions = Stream.empty();
        } else {
            internalCaptions = metaData.getSubtitleTracks().stream()
                    .map(c -> new CaptionInfo(
                            String.valueOf(c.getId()),
                            CaptionInfo.Location.internal,
                            getDisplayFormat(c.getCodec()),
                            c.getLanguage()));
        }

        Stream<CaptionInfo> externalCaptions = findExternalCaptionsForVideo(video).stream()
                .map(c -> new CaptionInfo(c.toString(), // leaks internal structure for now
                        CaptionInfo.Location.external,
                        MoreFiles.getFileExtension(c),
                        c.getFileName().toString()));

        return Stream.concat(internalCaptions, externalCaptions).collect(Collectors.toList());
    }

    public MetaData getVideoMetaData(MediaFile video) {
        MetaDataParser parser = this.metaDataParserFactory.getParser(video.getFile());
        return (parser != null) ? parser.getMetaData(video.getFile()) : null;
    }

    private Resource getExternalResource(Path captionsFile, String format) throws IOException {
        if ("vtt".equals(format)) {
            return new PathResource(captionsFile);
        } else {
            return new InputStreamResource(new BOMInputStream(Files.newInputStream(captionsFile)));
        }
    }

    // srt -> vtt
    private Resource getConvertedResource(Path captionsFile) throws IOException {
        Consumer<InputStream> poutInit = (input) -> {
            PipedInputStream pin = (PipedInputStream) input;

            // start a new thread to feed data in
            new Thread(() -> {
                try (BOMInputStream bomInputStream = new BOMInputStream(Files.newInputStream(captionsFile));
                        Reader reader = new InputStreamReader(bomInputStream,
                                ByteOrderMark.UTF_8.equals(bomInputStream.getBOM()) ? "UTF-8" : "ISO-8859-1");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        PipedOutputStream po = new PipedOutputStream(pin);
                        Writer writer = new OutputStreamWriter(po, "UTF-8");
                        BufferedWriter bufferedWriter = new BufferedWriter(writer);) {
                    bufferedWriter.append("WEBVTT");
                    bufferedWriter.newLine();
                    bufferedWriter.newLine();
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        line = line.replaceFirst("(\\d+:\\d+:\\d+),(\\d+) --> (\\d+:\\d+:\\d+),(\\d+)",
                                "$1.$2 --> $3.$4");
                        bufferedWriter.append(line);
                        bufferedWriter.newLine();
                        line = bufferedReader.readLine();
                    }
                    bufferedWriter.flush();
                } catch (Exception e) {
                    LOG.warn("Error writing to subtitles stream for {}", captionsFile, e);
                }
            }, "CaptionsControllerDatafeed").start();

            // wait for src data thread to connect
            while (pin.source == null) {
                // sit and wait and ponder life
            }
        };

        PipedInputStream in = new PipedInputStream();
        return new PipeStreams.DelayedResource(new InputStreamResource(in), poutInit);
    }

    public List<Path> findExternalCaptionsForVideo(MediaFile video) {
        Path file = video.getFile();

        try (Stream<Path> children = Files.walk(file.getParent())) {
            return children.parallel()
                    .filter(c -> Files.isRegularFile(c))
                    .filter(c -> CAPTIONS_FORMATS.contains(MoreFiles.getFileExtension(c)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("Could not retrieve directory list for {} to find subtitle files for {}", file.getParent(), file, e);

            return Collections.emptyList();
        }
    }

    public static class CaptionInfo {
        public enum Location {
            internal, external
        }

        private final String identifier;
        private final Location location;
        private final String format;
        private final String language;

        public CaptionInfo(String identifier, Location location, String format, String language) {
            this.identifier = identifier;
            this.location = location;
            this.format = format;
            this.language = language;
        }

        public String getIdentifier() {
            return identifier;
        }

        public Location getLocation() {
            return location;
        }

        public String getFormat() {
            return format;
        }

        public String getLanguage() {
            return language;
        }
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
