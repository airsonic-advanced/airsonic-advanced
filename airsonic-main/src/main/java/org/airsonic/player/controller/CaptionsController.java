package org.airsonic.player.controller;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.MoreFiles;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.User;
import org.airsonic.player.io.PipeStreams;
import org.airsonic.player.io.PipeStreams.PipedInputStream;
import org.airsonic.player.io.PipeStreams.PipedOutputStream;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.SecurityService;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Controller
@RequestMapping("/captions")
public class CaptionsController {
    private static final Logger LOG = LoggerFactory.getLogger(CaptionsController.class);

    private static final String CAPTION_FORMAT_VTT = "vtt";
    private static final String CAPTION_FORMAT_SRT = "srt";
    private static final Set<String> CAPTIONS_FORMATS = ImmutableSet.of(CAPTION_FORMAT_VTT, CAPTION_FORMAT_SRT);

    private MediaFileService mediaFileService;
    private SecurityService securityService;

    @GetMapping
    public ResponseEntity<Resource> handleRequest(
            Authentication authentication,
            @RequestParam int id,
            @RequestParam(required = false, name = "format") String requiredFormat) throws Exception {

        User user = securityService.getUserByName(authentication.getName());
        MediaFile video = this.mediaFileService.getMediaFile(id);
        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(video, user.getUsername())) {
            throw new AccessDeniedException("Access to file " + id + " is forbidden for user " + user.getUsername());
        }
        Path captionsFile = findCaptionsForVideo(video);
        if (captionsFile == null) {
            throw new NotFoundException("No captions found for file id: " + id);
        }
        String actualFormat = MoreFiles.getFileExtension(captionsFile);
        String effectiveFormat = requiredFormat != null ? requiredFormat : actualFormat;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(CAPTION_FORMAT_VTT.equalsIgnoreCase(effectiveFormat)
                ? new MediaType("text", "vtt", StandardCharsets.UTF_8)
                : new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setAccessControlAllowOrigin("*");

        Resource resource = null;
        if (requiredFormat == null || requiredFormat.equalsIgnoreCase(actualFormat)) {
            resource = getResource(captionsFile, actualFormat);
        } else if ("srt".equals(actualFormat) && "vtt".equals(requiredFormat)) {
            resource = getConvertedResource(captionsFile);
        } else {
            throw new NotFoundException("No captions found for file id: " + id);
        }

        return ResponseEntity.ok()
                .lastModified(Files.getLastModifiedTime(captionsFile).toInstant())
                .headers(headers)
                .body(resource);
    }

    private Resource getResource(Path captionsFile, String format) throws IOException {
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

    public Path findCaptionsForVideo(MediaFile video) {
        Path file = video.getFile();
        String videoFileBaseName = MoreFiles.getNameWithoutExtension(file.getFileName());

        try (Stream<Path> children = Files.list(file.getParent())) {
            return children.parallel()
                    .filter(c -> Files.isRegularFile(c))
                    .filter(c -> CAPTIONS_FORMATS.contains(MoreFiles.getFileExtension(c)))
                    .filter(c -> MoreFiles.getNameWithoutExtension(c).startsWith(videoFileBaseName)
                            || videoFileBaseName.startsWith(MoreFiles.getNameWithoutExtension(c)))
                    .findAny()
                    .orElse(null);
        } catch (IOException e) {
            LOG.warn("Could not retrieve directory list for {} to find subtitle files for {}", file.getParent(), file, e);

            return null;
        }
    }

    @Autowired
    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    @Autowired
    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
