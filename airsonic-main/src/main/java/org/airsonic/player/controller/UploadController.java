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

import com.google.common.util.concurrent.RateLimiter;

import org.airsonic.player.ajax.UploadInfo;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.domain.User;
import org.airsonic.player.service.PlayerService;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.upload.MonitoredDiskFileItemFactory;
import org.airsonic.player.upload.UploadListener;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Controller which receives uploaded files.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/upload")
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SimpMessagingTemplate brokerTemplate;

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> map = new HashMap<>();
        List<Path> uploadedFiles = new ArrayList<>();
        List<Path> unzippedFiles = new ArrayList<>();
        TransferStatus status = null;

        try {

            status = statusService.createUploadStatus(playerService.getPlayer(request, response, false, false));
            status.setBytesTotal(request.getContentLength());
            brokerTemplate.convertAndSendToUser(status.getPlayer().getUsername(), "/queue/uploads/status",
                    new UploadInfo(status.getId(), 0L, status.getBytesTotal()));

            // Check that we have a file upload request
            if (!ServletFileUpload.isMultipartContent(request)) {
                throw new Exception("Illegal request.");
            }

            Path dir = null;
            boolean unzip = false;

            UploadListener listener = new UploadListenerImpl(status, settingsService.getUploadBitrateLimiter(), brokerTemplate);

            FileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
            ServletFileUpload upload = new ServletFileUpload(factory);

            List<?> items = upload.parseRequest(request);

            // First, look for "dir" and "unzip" parameters.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (item.isFormField() && "dir".equals(item.getFieldName())) {
                    dir = Paths.get(item.getString());
                } else if (item.isFormField() && "unzip".equals(item.getFieldName())) {
                    unzip = true;
                }
            }

            if (dir == null) {
                throw new Exception("Missing 'dir' parameter.");
            }

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // Look for file items.
            for (Object o : items) {
                FileItem item = (FileItem) o;

                if (!item.isFormField()) {
                    String fileName = item.getName();
                    if (!fileName.trim().isEmpty()) {

                        Path targetFile = dir.resolve(Paths.get(fileName).getFileName());

                        if (!securityService.isUploadAllowed(targetFile)) {
                            throw new Exception("Permission denied: " + StringEscapeUtils.escapeHtml(targetFile.toString()));
                        }

                        item.write(targetFile.toFile());
                        uploadedFiles.add(targetFile);
                        LOG.info("Uploaded " + targetFile);

                        if (unzip && targetFile.getFileName().toString().toLowerCase().endsWith(".zip")) {
                            unzip(targetFile, unzippedFiles);
                        }
                    }
                }
            }

        } catch (Exception x) {
            LOG.warn("Uploading failed.", x);
            map.put("exception", x);
        } finally {
            if (status != null) {
                statusService.removeUploadStatus(status);
                brokerTemplate.convertAndSendToUser(status.getPlayer().getUsername(), "/queue/uploads/status",
                        new UploadInfo(status.getId(), status.getBytesTotal() + 1, status.getBytesTotal()));
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransferred());
            }
        }

        map.put("uploadedFiles", uploadedFiles);
        map.put("unzippedFiles", unzippedFiles);

        return new ModelAndView("upload", "model", map);
    }

    private void unzip(Path file, List<Path> unzippedFiles) throws Exception {
        LOG.info("Unzipping " + file);

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final Path toPath = file.resolveSibling(entry.getName());
                if (!toPath.normalize().startsWith(file.getParent())) {
                    throw new Exception("Bad zip filename: " + StringEscapeUtils.escapeHtml(toPath.toString()));
                }

                if (entry.isDirectory()) {
                    Files.createDirectory(toPath);
                } else {
                    if (!securityService.isUploadAllowed(toPath)) {
                        throw new Exception("Permission denied: " + StringEscapeUtils.escapeHtml(toPath.toString()));
                    }
                    Files.copy(zipInputStream, toPath);
                    LOG.info("Unzipped " + toPath);
                    unzippedFiles.add(toPath);
                }
            }
        } catch (IOException e) {
            LOG.warn("Something went wrong unzipping {}", file, e);
        } finally {
            FileUtil.delete(file);
        }
    }

    /**
     * Receives callbacks as the file upload progresses.
     */
    private static class UploadListenerImpl implements UploadListener {
        private TransferStatus status;
        private SimpMessagingTemplate brokerTemplate;
        private RateLimiter rateLimiter;
        private volatile int lastBroadcastPercentage = 0;

        private UploadListenerImpl(TransferStatus status, RateLimiter rateLimiter,
                SimpMessagingTemplate brokerTemplate) {
            this.status = status;
            this.brokerTemplate = brokerTemplate;
            this.rateLimiter = rateLimiter;
        }

        @Override
        public void start(String fileName) {
            status.setFile(Paths.get(fileName));
        }

        @Override
        public void bytesRead(long bytesRead) {
            status.addBytesTransferred(bytesRead);
            broadcast();
            // Throttle bitrate.
            rateLimiter.acquire((int) bytesRead);
        }

        private void broadcast() {
            long percentDone = 100 * status.getBytesTransferred() / Math.max(1, status.getBytesTotal());
            // broadcast every 2% (no need to broadcast at every byte read)
            if (percentDone - lastBroadcastPercentage > 2) {
                lastBroadcastPercentage = (int) percentDone;
                CompletableFuture.runAsync(() -> brokerTemplate.convertAndSendToUser(
                        status.getPlayer().getUsername(),
                        "/queue/uploads/status",
                        new UploadInfo(status.getId(), status.getBytesTransferred(), status.getBytesTotal())));
            }
        }
    }

}
