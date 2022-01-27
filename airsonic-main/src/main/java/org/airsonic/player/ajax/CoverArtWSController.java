package org.airsonic.player.ajax;

import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.LastFmCoverArt;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.CoverArtService;
import org.airsonic.player.service.LastFmService;
import org.airsonic.player.service.MediaFileService;
import org.airsonic.player.service.MediaFolderService;
import org.airsonic.player.service.SecurityService;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
@MessageMapping("/coverart")
public class CoverArtWSController {
    private static final Logger LOG = LoggerFactory.getLogger(CoverArtWSController.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private LastFmService lastFmService;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private CoverArtService coverArtService;

    @MessageMapping("/search")
    @SendToUser(broadcast = false)
    public List<LastFmCoverArt> searchCoverArt(CoverArtSearchRequest req) {
        return lastFmService.searchCoverArt(req.getArtist(), req.getAlbum());
    }

    /**
     * Downloads and saves the cover art at the given URL.
     *
     * @return The error string if something goes wrong, <code>"OK"</code> otherwise.
     */
    @MessageMapping("/set")
    @SendToUser(broadcast = false)
    public String setCoverArtImage(CoverArtSetRequest req) {
        try {
            MediaFile mediaFile = mediaFileService.getMediaFile(req.getId());
            saveCoverArt(mediaFile, req.getUrl());
            return "OK";
        } catch (Exception e) {
            LOG.warn("Failed to save cover art for media file {}", req.getId(), e);
            return e.toString();
        }
    }

    private void saveCoverArt(MediaFile dir, String url) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(20 * 1000) // 20 seconds
                .setSocketTimeout(20 * 1000) // 20 seconds
                .build();
        HttpGet method = new HttpGet(url);
        method.setConfig(requestConfig);

        // Attempt to resolve proper suffix.
        String suffix = "jpg";
        if (url.toLowerCase().endsWith(".gif")) {
            suffix = "gif";
        } else if (url.toLowerCase().endsWith(".png")) {
            suffix = "png";
        }

        // Check permissions.
        MusicFolder folder = mediaFolderService.getMusicFolderById(dir.getFolderId());
        Path fullPath = dir.getFullPath(folder.getPath());
        Path newCoverFile = fullPath.resolve("cover." + suffix);
        if (!securityService.isWriteAllowed(folder.getPath().relativize(newCoverFile), folder)) {
            throw new SecurityException("Permission denied: " + StringEscapeUtils.escapeHtml(newCoverFile.toString()));
        }

        try (CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(method);
                InputStream input = response.getEntity().getContent()) {

            // If file exists, create a backup.
            backup(newCoverFile, fullPath.resolve("cover." + suffix + ".backup"));

            // Write file.
            Files.copy(input, newCoverFile, StandardCopyOption.REPLACE_EXISTING);
        }

        coverArtService.upsert(EntityType.MEDIA_FILE, dir.getId(), folder.getPath().relativize(newCoverFile).toString(), dir.getFolderId(), true);
    }

    private void backup(Path newCoverFile, Path backup) {
        if (Files.exists(newCoverFile)) {
            try {
                Files.move(newCoverFile, backup, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Backed up old image file to {}", backup);
            } catch (IOException e) {
                LOG.warn("Failed to create image file backup {}", backup, e);
            }
        }
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setLastFmService(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
    }

    public static class CoverArtSearchRequest {
        private String artist;
        private String album;

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }
    }

    public static class CoverArtSetRequest {
        private int id;
        private String url;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
