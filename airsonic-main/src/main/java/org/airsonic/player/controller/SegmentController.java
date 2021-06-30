package org.airsonic.player.controller;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.io.PipeStreams.MonitoredResource;
import org.airsonic.player.security.JWTAuthenticationToken;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.service.hls.FFmpegHlsSession;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.PostConstruct;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Controller
@RequestMapping({ "/segment/**", "/ext/segment/**" })
public class SegmentController {

    private static final Logger LOG = LoggerFactory.getLogger(SegmentController.class);

    @Autowired
    private PlayerService playerService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SettingsService settingsService;

    private final Map<FFmpegHlsSession.Key, FFmpegHlsSession> sessions = new HashMap<>();

    @PostConstruct
    public void init() {
        FileUtil.delete(FFmpegHlsSession.getHlsRootDirectory());
    }

    @GetMapping
    public ResponseEntity<Resource> handleRequest(Authentication auth,
            @RequestParam int id,
            @RequestParam int segmentIndex,
            @RequestParam(name = "player") String playerId,
            @RequestParam int maxBitRate,
            @RequestParam String size,
            @RequestParam(required = false) Integer audioTrack,
            ServletWebRequest swr) throws Exception {
        MediaFile mediaFile = this.mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            throw new NotFoundException("Media file not found: " + id);
        }
        Player player = this.playerService.getPlayer(swr.getRequest(), swr.getResponse());
        User user = securityService.getUserByName(auth.getName());
        if (!(auth instanceof JWTAuthenticationToken)
                && !securityService.isFolderAccessAllowed(mediaFile, user.getUsername())) {
            throw new AccessDeniedException("Access to file " + id + " is forbidden for user " + user.getUsername());
        }
        TransferStatus status = this.statusService.createStreamStatus(player);
        status.setFile(mediaFile.getFile());
        FFmpegHlsSession.Key sessionKey = new FFmpegHlsSession.Key(id, playerId, maxBitRate, size, audioTrack);
        FFmpegHlsSession session = getOrCreateSession(sessionKey, mediaFile);
        Path segmentFile = session.waitForSegment(segmentIndex, 30000L);
        if (segmentFile == null) {
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Timed out producing segment " + segmentIndex + " for media file " + id);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setAccessControlAllowOrigin("*");
        headers.setContentType(MediaType.parseMediaType("video/MP2T"));

        Supplier<TransferStatus> statusSupplier = () -> status;
        Consumer<TransferStatus> statusCloser = s -> {
            securityService.updateUserByteCounts(user, s.getBytesTransferred(), 0L, 0L);
            statusService.removeStreamStatus(s);
        };
        BiConsumer<InputStream, TransferStatus> inputStreamInit = (i, s) -> {
            LOG.info("{}: {} listening to {}", player.getIpAddress(), player.getUsername(),
                    FileUtil.getShortPath(mediaFile.getFile()));
            if (segmentIndex == 0)
                this.mediaFileService.incrementPlayCount(mediaFile);
        };

        Resource resource = new MonitoredResource(new PathResource(segmentFile),
                settingsService.getDownloadBitrateLimiter(), statusSupplier, statusCloser, inputStreamInit);

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private FFmpegHlsSession getOrCreateSession(FFmpegHlsSession.Key sessionKey, MediaFile mediaFile) {
        synchronized (this.sessions) {
            FFmpegHlsSession session = this.sessions.get(sessionKey);
            if (session == null) {
                this.sessions.keySet().parallelStream()
                        .filter(k -> k.getMediaFileId() == sessionKey.getMediaFileId())
                        .filter(k -> StringUtils.equals(k.getPlayerId(), sessionKey.getPlayerId()))
                        .filter(k -> this.sessions.get(k) != null)
                        .forEach(k -> this.sessions.remove(k).destroySession());

                session = new FFmpegHlsSession(sessionKey, mediaFile);
                this.sessions.put(sessionKey, session);
            }
            return session;
        }
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

}
