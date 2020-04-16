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

import com.google.common.collect.Streams;

import org.airsonic.player.domain.*;
import org.airsonic.player.io.PipeStreams.MonitoredResource;
import org.airsonic.player.io.PipeStreams.PipedInputStream;
import org.airsonic.player.io.PipeStreams.PipedOutputStream;
import org.airsonic.player.service.*;
import org.airsonic.player.spring.KnownLengthInputStreamResource;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.LambdaUtils;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A controller used for downloading files to a remote client. If the requested
 * path refers to a file, the given file is downloaded. If the requested path
 * refers to a directory, the entire directory (including sub-directories) are
 * downloaded as an uncompressed zip-file.
 *
 * @author Sindre Mehus
 * @author Randomnic
 */

@Controller
@RequestMapping("/download")
public class DownloadController {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadController.class);

    @Autowired
    private PlayerService playerService;
    @Autowired
    private StatusService statusService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileService mediaFileService;

    @GetMapping
    public ResponseEntity<Resource> handleRequest(Principal p,
            @RequestParam Optional<Integer> id,
            @RequestParam(required = false) Integer playlist,
            @RequestParam(required = false) Integer player,
            @RequestParam(required = false, name = "i") List<Integer> indices,
            ServletWebRequest swr) throws Exception {
        User user = securityService.getUserByName(p.getName());
        Player transferPlayer = playerService.getPlayer(swr.getRequest(), swr.getResponse(), false, false);
        String defaultDownloadName = null;
        ResponseDTO response = null;

        Supplier<TransferStatus> statusSupplier = () -> statusService.createDownloadStatus(transferPlayer);

        Consumer<TransferStatus> statusCloser = status -> {
            statusService.removeDownloadStatus(status);
            securityService.updateUserByteCounts(user, 0L, status.getBytesTransferred(), 0L);
            LOG.info("Transferred {} bytes to user: {}, player: {}", status.getBytesTransferred(), user.getUsername(), transferPlayer);
        };

        MediaFile mediaFile = id.map(mediaFileService::getMediaFile).orElse(null);

        if (mediaFile != null) {
            if (!securityService.isFolderAccessAllowed(mediaFile, user.getUsername())) {
                throw new AccessDeniedException("Access to file " + mediaFile.getId() + " is forbidden for user " + user.getUsername());
            }

            if (mediaFile.isFile()) {
                response = prepareResponse(Collections.singletonList(mediaFile), null, statusSupplier, statusCloser);
                defaultDownloadName = mediaFile.getFile().getFileName().toString();
            } else {
                response = prepareResponse(mediaFileService.getChildrenOf(mediaFile, true, false, true), indices,
                        statusSupplier, statusCloser, indices == null ? mediaFile.getCoverArtFile() : null);
                defaultDownloadName = FilenameUtils.getBaseName(mediaFile.getPath()) + ".zip";
            }
        } else if (playlist != null) {
            response = prepareResponse(playlistService.getFilesInPlaylist(playlist), indices, statusSupplier, statusCloser);
            defaultDownloadName = playlistService.getPlaylist(playlist).getName() + ".zip";
        } else if (player != null) {
            response = prepareResponse(transferPlayer.getPlayQueue().getFiles(), indices, statusSupplier, statusCloser);
            defaultDownloadName = "download.zip";
        }

        if (swr.checkNotModified(String.valueOf(response.getSize()), response.getChanged())) {
            return null;
        }

        String filename = Optional.ofNullable(response.getProposedName()).orElse(defaultDownloadName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentType(MediaType.parseMediaType(StringUtil.getMimeType(FilenameUtils.getExtension(filename))));
        LOG.info("Downloading '{}' to {}", filename, player);
        return ResponseEntity.ok().headers(headers).body(response.getResource());
    }

    /**
     * Computes the CRC checksum for the given file.
     *
     * @param file The file to compute checksum for.
     * @return A CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    private static long computeCrc(Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(is);
                CheckedInputStream cis = new CheckedInputStream(bis, new CRC32())) {
            byte[] buf = new byte[8192];
            while ((cis.read(buf)) != -1) {
                continue;
            }

            return cis.getChecksum().getValue();
        }
    }

    private static long zipSize(Stream<Entry<String, Long>> paths) {
        return paths.mapToLong(e -> 30 + 46 + (2L * e.getKey().length()) + e.getValue()).sum() + 22;
    }

    private ResponseDTO prepareResponse(List<MediaFile> files, List<Integer> indices,
            Supplier<TransferStatus> statusSupplier, Consumer<TransferStatus> statusCloser, Path... additionalFiles)
            throws IOException {
        if (indices == null) {
            indices = IntStream.range(0, files.size()).boxed().collect(Collectors.toList());
        } else if (indices.parallelStream().anyMatch(i -> i >= files.size())) {
            throw new IllegalArgumentException("Can't have index > number of files");
        }

        if (indices.size() == 0) {
            // nothing, just return empty
            return new ResponseDTO(null, "emptyfile.download", 0, -1);
        }

        if (indices.size() == 1 && (additionalFiles == null || additionalFiles.length == 0)) {
            // single file
            MediaFile file = files.get(indices.get(0));
            Path path = file.getFile();
            long changed = file.getChanged() == null ? -1 : file.getChanged().toEpochMilli();
            return new ResponseDTO(
                    new MonitoredResource(
                            new FileSystemResource(path),
                            settingsService.getDownloadBitrateLimiter(),
                            statusSupplier,
                            statusCloser,
                        (input, status) -> {}),
                    path.getFileName().toString(),
                    file.getFileSize(),
                    changed);
        } else {
            // get a list of all paths under the tree, plus their zip names and sizes
            Collection<Pair<Path, Pair<String, Long>>> pathsToZip = Streams
                    .concat(indices.stream().map(i -> files.get(i)).map(x -> x.getFile()), Stream.of(additionalFiles))
                    .flatMap(p -> {
                        Path parent = p.getParent();
                        try (Stream<Path> paths = Files.walk(p)) {
                            return paths.filter(f -> !f.getFileName().toString().startsWith(".")).map(f -> {
                                String zipName = parent.relativize(f).toString();
                                long size = 0L;
                                if (Files.isRegularFile(f)) {
                                    size = FileUtil.size(f);
                                } else {
                                    zipName = zipName + '/';
                                }
                                return Pair.of(f, Pair.of(zipName, size));
                            }).collect(Collectors.toList()).stream();
                        } catch (Exception e) {
                            LOG.warn("Error retrieving file to zip", e);
                            return Stream.empty();
                        }
                    }).filter(f -> Objects.nonNull(f))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // zip to out
            BiConsumer<InputStream, TransferStatus> poutInit = (input, status) -> {
                PipedInputStream pin = (PipedInputStream) input;

                // start a new thread to feed data in
                new Thread(() -> {
                    try (PipedOutputStream pout = new PipedOutputStream(pin);
                            ZipOutputStream zout = new ZipOutputStream(pout)) {
                        zout.setMethod(ZipOutputStream.STORED); // No compression.
                        pathsToZip.stream().forEach(LambdaUtils.uncheckConsumer(f -> {
                            status.setFile(f.getKey());
                            ZipEntry zipEntry = new ZipEntry(f.getValue().getKey());
                            zipEntry.setSize(f.getValue().getValue());
                            zipEntry.setCompressedSize(f.getValue().getValue());

                            if (f.getValue().getKey().endsWith("/") && f.getValue().getValue() == 0L) {
                                // directory
                                zipEntry.setCrc(0);
                                zout.putNextEntry(zipEntry);
                            } else {
                                zipEntry.setCrc(computeCrc(f.getKey()));
                                zout.putNextEntry(zipEntry);
                                Files.copy(f.getKey(), zout);
                            }

                            zout.closeEntry();
                        }));
                    } catch (Exception e1) {
                        LOG.debug("Error with output to zip", e1);
                    }
                }, "DownloadControllerDatafeed").start();

                // wait for src data thread to connect
                while (pin.source == null) {
                    // sit and wait and ponder life
                }
            };

            long size = zipSize(pathsToZip.stream().map(e -> e.getValue()));

            PipedInputStream pin = new PipedInputStream(null, 16 * 1024); // 16 Kb buffer

            return new ResponseDTO(
                    new MonitoredResource(
                            new KnownLengthInputStreamResource(pin, size),
                            settingsService.getDownloadBitrateLimiter(),
                            statusSupplier,
                            statusCloser,
                            poutInit),
                    null, size, -1);
        }
    }

    public static class ResponseDTO {
        private final Resource resource;
        private final String proposedName;
        // used as an ETag to see if a resource has changed
        private final long size;
        // used for Last-Modified
        private final long changed;

        public ResponseDTO(Resource resource, String proposedName, long size, long changed) {
            this.resource = resource;
            this.proposedName = proposedName;
            this.size = size;
            this.changed = changed;
        }

        public Resource getResource() {
            return resource;
        }

        public String getProposedName() {
            return proposedName;
        }

        public long getSize() {
            return size;
        }

        public long getChanged() {
            return changed;
        }

    }

}
