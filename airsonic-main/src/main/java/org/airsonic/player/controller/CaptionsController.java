package org.airsonic.player.controller;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.MoreFiles;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.User;
import org.airsonic.player.io.InputStreamReaderThread;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.JWTSecurityService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.MediaFolderService;
import org.airsonic.player.service.NetworkService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping({ "/captions", "/ext/captions" })
public class CaptionsController {
    private static final Logger LOG = LoggerFactory.getLogger(CaptionsController.class);

    private static final String CAPTION_FORMAT_VTT = "vtt";
    private static final String CAPTION_FORMAT_SRT = "srt";
    private static final Set<String> CAPTIONS_FORMATS = ImmutableSet.of(CAPTION_FORMAT_VTT, CAPTION_FORMAT_SRT);

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private JWTSecurityService jwtSecurityService;

    @GetMapping
    public ResponseEntity<Resource> handleRequest(
            Authentication authentication,
            @RequestParam int id,
            @RequestParam(required = false) String captionId,
            @RequestParam(required = false, name = "format") String requiredFormat,
            HttpServletRequest request)
            throws Exception {

        User user = securityService.getUserByName(authentication.getName());
        MediaFile video = this.mediaFileService.getMediaFile(id);
        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(video, user.getUsername())) {
            throw new AccessDeniedException("Access to file " + id + " is forbidden for user " + user.getUsername());
        }

        List<CaptionInfo> captions = listCaptions(video, NetworkService.getBaseUrl(request));
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
                resource = getConvertedResource(captionsFile, "0", effectiveFormat);
            } else {
                throw new NotFoundException("No captions found for file id: " + id);
            }
            time = Files.getLastModifiedTime(captionsFile).toInstant();
        } else {
            Path videoFullPath = video.getFullPath(mediaFolderService.getMusicFolderById(video.getFolderId()).getPath());
            resource = getConvertedResource(videoFullPath, res.getIdentifier(), effectiveFormat);
            time = Files.getLastModifiedTime(videoFullPath).toInstant();
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

    public Resource getConvertedResource(Path inputFile, String identifier, String format) throws IOException {
        String[] split = StringUtils.split(settingsService.getSubtitlesExtractionCommand());
        List<String> command = Arrays.stream(split).sequential()
                .map(i -> {
                    if (i.equals(split[0])) {
                        return SettingsService.resolveTranscodeExecutable(i, i);
                    }
                    if (i.contains("%s")) {
                        return i.replace("%s", inputFile.toString());
                    }
                    if (i.contains("%i")) {
                        return i.replace("%i", identifier);
                    }
                    if (i.contains("%f")) {
                        return i.replace("%f", getForceFormat(format));
                    }

                    return i;
                }).collect(Collectors.toList());
        Process process = new ProcessBuilder(command).start();
        Resource resource = new InputStreamResource(process.getInputStream());
        // Must read stderr from the process, otherwise it may block.
        new InputStreamReaderThread(process.getErrorStream(), "subs-extraction-error-stream-" + inputFile.toString(), true).start();
        return resource;
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
    public @ResponseBody List<CaptionInfo> listCaptions(Authentication authentication, @RequestParam int id, HttpServletRequest request) {
        MediaFile video = mediaFileService.getMediaFile(id);
        if (!(authentication instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(video, authentication.getName())) {
            throw new AccessDeniedException("Access to file " + id + " is forbidden for user " + authentication.getName());
        }

        String user = null;
        Instant expiration = null;

        if (authentication instanceof JWTAuthenticationToken) {
            user = authentication.getName();
            expiration = JWTSecurityService.getExpiration((JWTAuthenticationToken) authentication);
        }

        return listCaptions(video, NetworkService.getBaseUrl(request), user, expiration);
    }

    public List<CaptionInfo> listCaptions(MediaFile video, String basePath) {
        return listCaptions(video, basePath, null, null);
    }

    public List<CaptionInfo> listCaptions(MediaFile video, String basePath, String externalUser, Instant externalExpiration) {
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
                            c.getLanguage(),
                            getUrl(basePath, externalUser, externalExpiration, video.getId(),
                                    String.valueOf(c.getId()))));
        }

        Stream<CaptionInfo> externalCaptions = findExternalCaptionsForVideo(video).stream()
                .map(c -> new CaptionInfo(c.toString(), // leaks internal structure for now
                        CaptionInfo.Location.external,
                        MoreFiles.getFileExtension(c),
                        c.getFileName().toString(),
                        getUrl(basePath, externalUser, externalExpiration, video.getId(),
                                URLEncoder.encode(c.toString(), StandardCharsets.UTF_8))));

        return Stream.concat(internalCaptions, externalCaptions).collect(Collectors.toList());
    }

    public MetaData getVideoMetaData(MediaFile video) {
        Path videoFullPath = video.getFullPath(mediaFolderService.getMusicFolderById(video.getFolderId()).getPath());
        MetaDataParser parser = this.metaDataParserFactory.getParser(videoFullPath);
        return (parser != null) ? parser.getMetaData(videoFullPath) : null;
    }

    public String getUrl(String basePath, String externalUser, Instant externalExpiration, int mediaId, String captionId) {
        boolean ext = !StringUtils.isBlank(externalUser);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString((ext ? "ext/" : "") + "captions")
                .queryParam("id", mediaId)
                .queryParam("captionId", captionId);

        if (ext) {
            builder = jwtSecurityService.addJWTToken(externalUser, builder, externalExpiration);
        }
        return basePath + builder.build().toUriString();
    }

    private Resource getExternalResource(Path captionsFile, String format) throws IOException {
        if ("vtt".equals(format)) {
            return new PathResource(captionsFile);
        } else {
            return new InputStreamResource(new BOMInputStream(Files.newInputStream(captionsFile)));
        }
    }

    public List<Path> findExternalCaptionsForVideo(MediaFile video) {
        MediaFile parent = mediaFileService.getParentOf(video);
        if (parent == null) {
            return Collections.emptyList();
        }
        Path parentPath = parent.getFullPath(mediaFolderService.getMusicFolderById(parent.getFolderId()).getPath());

        try (Stream<Path> children = Files.walk(parentPath)) {
            return children.parallel()
                    .filter(c -> Files.isRegularFile(c))
                    .filter(c -> CAPTIONS_FORMATS.contains(MoreFiles.getFileExtension(c)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("Could not retrieve directory list for {} to find subtitle files for {}", parentPath, video, e);

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
        private final String url;

        public CaptionInfo(String identifier, Location location, String format, String language, String url) {
            this.identifier = identifier;
            this.location = location;
            this.format = format;
            this.language = language;
            this.url = url;
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

        public String getUrl() {
            return url;
        }
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }
}
