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

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.MoreFiles;
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
import org.airsonic.player.util.LambdaUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;


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

    private static final Set<String> SUPPORTED_ZIP_FORMATS = ImmutableSet.of("zip", "7z", "rar", "cpio", "jar", "tar");

    @PostMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> map = new HashMap<>();
        List<Path> uploadedFiles = new ArrayList<>();
        List<Path> unzippedFiles = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
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

            securityService.checkUploadAllowed(dir, false);

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

                        try {
                            securityService.checkUploadAllowed(targetFile, true);
                        } catch (IOException e) {
                            exceptions.add(e);
                            continue;
                        }

                        item.write(targetFile.toFile());
                        uploadedFiles.add(targetFile);
                        LOG.info("Uploaded {} ", targetFile);

                        if (unzip && SUPPORTED_ZIP_FORMATS.contains(MoreFiles.getFileExtension(targetFile).toLowerCase())) {
                            unzip(targetFile, unzippedFiles, exceptions);
                        }
                    }
                }
            }
        } catch (Exception x) {
            LOG.warn("Uploading failed.", x);
            exceptions.add(x);
        } finally {
            if (status != null) {
                statusService.removeUploadStatus(status);
                brokerTemplate.convertAndSendToUser(status.getPlayer().getUsername(), "/queue/uploads/status",
                        new UploadInfo(status.getId(), status.getBytesTotal() + 1, status.getBytesTotal()));
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, 0L, status.getBytesTransferred());
            }
        }

        map.put("exceptions", exceptions);
        map.put("uploadedFiles", uploadedFiles);
        map.put("unzippedFiles", unzippedFiles);

        return new ModelAndView("upload", "model", map);
    }

    private void unzip(Path file, List<Path> unzippedFiles, List<Exception> exceptions) {
        LOG.info("Unzipping {}", file);
        boolean unzipped = false;

        // rar files
        if (file.getFileName().toString().toLowerCase().endsWith(".rar")) {
            LOG.info("Trying rar-specific extraction method for {}", file);
            try (Archive zip = new Archive(file.toFile(), null)) {
                if (zip.isEncrypted()) {
                    throw new AccessDeniedException(file.toString(), null, "Archive is encrypted");
                }

                for (FileHeader fh : zip) {
                    if (fh.isEncrypted()) {
                        LOG.info("Can't read {} in {}", fh.getFileNameString(), file);
                        continue;
                    }

                    copyEntry(file, new ArchiveEntry() {
                        @Override
                        public boolean isDirectory() {
                            return fh.isDirectory();
                        }

                        @Override
                        public long getSize() {
                            return fh.getFullUnpackSize();
                        }

                        @Override
                        public String getName() {
                            if (fh.isFileHeader() && fh.isUnicode()) {
                                return fh.getFileNameW();
                            } else {
                                return fh.getFileNameString();
                            }
                        }

                        @Override
                        public Date getLastModifiedDate() {
                            return fh.getArcTime();
                        }
                    }, dest -> {
                            try (OutputStream os = Files.newOutputStream(dest)) {
                                zip.extractFile(fh, os);
                            } catch (RarException e) {
                                throw new IOException(e);
                            }
                        }, unzippedFiles, exceptions);
                }
            } catch (Exception e) {
                LOG.warn("Something went wrong unzipping {}", file, e);
                exceptions.add(e);
            } finally {
                unzipped = true;
                FileUtil.delete(file);
            }
        }

        // zip files
        if (file.getFileName().toString().toLowerCase().endsWith(".zip")) {
            LOG.info("Trying zip-specific extraction method for {}", file);
            try (FileChannel channel = FileChannel.open(file); ZipFile zip = new ZipFile(channel)) {
                Enumeration<ZipArchiveEntry> entries = zip.getEntries();
                ZipArchiveEntry entry = null;
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement();

                    if (!zip.canReadEntryData(entry)) {
                        LOG.info("Can't read {} in {}", entry.getName(), file);
                        continue;
                    }

                    try (InputStream is = zip.getInputStream(entry)) {
                        copyEntry(file, entry, dest -> Files.copy(is, dest), unzippedFiles, exceptions);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Something went wrong unzipping {}", file, e);
                exceptions.add(e);
            } finally {
                unzipped = true;
                FileUtil.delete(file);
            }
        }

        // 7z files
        if (file.getFileName().toString().toLowerCase().endsWith(".7z")) {
            LOG.info("Trying 7z-specific extraction method for {}", file);
            try (FileChannel channel = FileChannel.open(file); SevenZFile zip = new SevenZFile(channel)) {
                byte[] buffer = new byte[8042];
                SevenZArchiveEntry entry = null;
                while ((entry = zip.getNextEntry()) != null) {
                    copyEntry(file, entry, dest -> {
                        int bytesRead = -1;
                        try (OutputStream os = Files.newOutputStream(dest)) {
                            while ((bytesRead = zip.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                    }, unzippedFiles, exceptions);
                }
            } catch (Exception e) {
                LOG.warn("Something went wrong unzipping {}", file, e);
                exceptions.add(e);
            } finally {
                unzipped = true;
                FileUtil.delete(file);
            }
        }

        // Generic
        if (!unzipped) {
            LOG.info("Trying a generic extraction method for {}", file);
            try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file));
                    ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis)) {
                ArchiveEntry entry = null;
                while ((entry = ais.getNextEntry()) != null) {
                    if (!ais.canReadEntryData(entry)) {
                        LOG.info("Can't read {} in {}", entry.getName(), file);
                        continue;
                    }
                    copyEntry(file, entry, dest -> Files.copy(ais, dest), unzippedFiles, exceptions);
                }
            } catch (Exception e) {
                LOG.warn("Something went wrong unzipping {}", file, e);
                exceptions.add(e);
            } finally {
                FileUtil.delete(file);
            }
        }
    }

    private void copyEntry(Path file, ArchiveEntry entry, LambdaUtils.ThrowingConsumer<Path, IOException> copier,
            List<Path> unzippedFiles, List<Exception> exceptions) {
        final Path toPath = file.resolveSibling(entry.getName());
        try {
            if (!toPath.normalize().startsWith(file.getParent())) {
                throw new IOException("Bad zip filename: " + toPath.toString());
            }
            if (entry.isDirectory()) {
                Files.createDirectories(toPath);
            } else {
                Path parent = toPath.getParent();
                Files.createDirectories(parent);
                if (!Files.isDirectory(parent)) {
                    throw new IOException("Failed to create directory: " + parent);
                }

                securityService.checkUploadAllowed(toPath, true);

                copier.accept(toPath);
                unzippedFiles.add(toPath);
                LOG.debug("Unzipped {}", toPath);

            }
        } catch (IOException e) {
            exceptions.add(e);
            LOG.debug("Could not unzip {}", toPath, e);
        }

        LOG.info("Processed {}", toPath);
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
