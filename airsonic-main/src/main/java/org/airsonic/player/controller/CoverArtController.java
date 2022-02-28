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

import com.google.common.io.MoreFiles;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.ArtistDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.service.*;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.LastModified;

import javax.annotation.PostConstruct;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Controller which produces cover art images.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping({"/coverArt", "/ext/coverArt"})
public class CoverArtController implements LastModified {

    public static final String ALBUM_COVERART_PREFIX = "al-";
    public static final String ARTIST_COVERART_PREFIX = "ar-";
    public static final String PLAYLIST_COVERART_PREFIX = "pl-";
    public static final String PODCAST_COVERART_PREFIX = "pod-";

    private static final Logger LOG = LoggerFactory.getLogger(CoverArtController.class);

    @Autowired
    private MediaFileService mediaFileService;
    @Autowired
    private CoverArtService coverArtService;
    @Autowired
    private TranscodingService transcodingService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private PodcastService podcastService;
    @Autowired
    private ArtistDao artistDao;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private JaudiotaggerParser jaudiotaggerParser;
    private Semaphore semaphore;

    @PostConstruct
    public void init() {
        semaphore = new Semaphore(settingsService.getCoverArtConcurrency());
    }

    @Override
    public long getLastModified(HttpServletRequest request) {
        CoverArtRequest coverArtRequest = createCoverArtRequest(request);
        if (coverArtRequest == null) {
            return -1L;
        }
        return coverArtRequest.lastModified().toEpochMilli();
    }

    @GetMapping
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        CoverArtRequest coverArtRequest = createCoverArtRequest(request);
        LOG.trace("handleRequest - {}", coverArtRequest);
        Integer size = ServletRequestUtils.getIntParameter(request, "size");

        // Send fallback image if no ID is given. (No need to cache it, since it will be cached in browser.)
        if (coverArtRequest == null) {
            sendFallback(size, response);
            return;
        }

        try {
            // Optimize if no scaling is required.
            if (size == null && coverArtRequest.coverArt != null) {
                LOG.trace("sendUnscaled - {}", coverArtRequest);
                sendUnscaled(coverArtRequest, response);
                return;
            }

            // Send cached image, creating it if necessary.
            if (size == null) {
                size = CoverArtScheme.LARGE.getSize() * 2;
            }
            Path cachedImage = getCachedImage(coverArtRequest, size);
            sendImage(cachedImage, response);
        } catch (Exception e) {
            LOG.debug("Sending fallback as an exception was encountered during normal cover art processing", e);
            sendFallback(size, response);
        }

    }

    private CoverArtRequest createCoverArtRequest(HttpServletRequest request) {
        String id = request.getParameter("id");
        if (id == null) {
            return null;
        }

        if (id.startsWith(ALBUM_COVERART_PREFIX)) {
            return createAlbumCoverArtRequest(Integer.valueOf(id.replace(ALBUM_COVERART_PREFIX, "")));
        }
        if (id.startsWith(ARTIST_COVERART_PREFIX)) {
            return createArtistCoverArtRequest(Integer.valueOf(id.replace(ARTIST_COVERART_PREFIX, "")));
        }
        if (id.startsWith(PLAYLIST_COVERART_PREFIX)) {
            return createPlaylistCoverArtRequest(Integer.valueOf(id.replace(PLAYLIST_COVERART_PREFIX, "")));
        }
        if (id.startsWith(PODCAST_COVERART_PREFIX)) {
            return createPodcastCoverArtRequest(Integer.valueOf(id.replace(PODCAST_COVERART_PREFIX, "")), request);
        }
        return createMediaFileCoverArtRequest(Integer.valueOf(id), request);
    }

    private CoverArtRequest createAlbumCoverArtRequest(int id) {
        Album album = albumDao.getAlbum(id);
        return album == null ? null : new AlbumCoverArtRequest(album);
    }

    private CoverArtRequest createArtistCoverArtRequest(int id) {
        Artist artist = artistDao.getArtist(id);
        return artist == null ? null : new ArtistCoverArtRequest(artist);
    }

    private PlaylistCoverArtRequest createPlaylistCoverArtRequest(int id) {
        Playlist playlist = playlistService.getPlaylist(id);
        return playlist == null ? null : new PlaylistCoverArtRequest(playlist);
    }

    private CoverArtRequest createPodcastCoverArtRequest(int id, HttpServletRequest request) {
        PodcastChannel channel = podcastService.getChannel(id);
        if (channel == null) {
            return null;
        }
        if (channel.getMediaFileId() == null) {
            return new PodcastCoverArtRequest(channel);
        }
        return createMediaFileCoverArtRequest(channel.getMediaFileId(), request);
    }

    private CoverArtRequest createMediaFileCoverArtRequest(int id, HttpServletRequest request) {
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }
        if (mediaFile.isVideo()) {
            int offset = ServletRequestUtils.getIntParameter(request, "offset", 60);
            return new VideoCoverArtRequest(mediaFile, offset);
        }

        var dir = mediaFile.isDirectory() ? mediaFile : mediaFileService.getParentOf(mediaFile);

        return new MediaFileCoverArtRequest(dir, mediaFile.isDirectory() ? null : mediaFile.getId());
    }

    private void sendImage(Path file, HttpServletResponse response) throws IOException {
        response.setContentType(StringUtil.getMimeType(MoreFiles.getFileExtension(file)));
        Files.copy(file, response.getOutputStream());
    }

    private void sendFallback(Integer size, HttpServletResponse response) throws IOException {
        if (response.getContentType() == null) {
            response.setContentType(StringUtil.getMimeType("jpeg"));
        }
        try (InputStream in = getClass().getResourceAsStream("default_cover.jpg")) {
            BufferedImage image = ImageIO.read(in);
            if (size != null) {
                image = scale(image, size, size);
            }
            ImageIO.write(image, "jpeg", response.getOutputStream());
        }
    }

    private void sendUnscaled(CoverArtRequest coverArtRequest, HttpServletResponse response) throws IOException {
        Pair<InputStream, String> imageInputStreamWithType = getImageInputStreamWithType(
                coverArtService.getFullPath(coverArtRequest.coverArt));

        try (InputStream in = imageInputStreamWithType.getLeft()) {
            response.setContentType(imageInputStreamWithType.getRight());
            IOUtils.copy(in, response.getOutputStream());
        }
    }

    private Path getCachedImage(CoverArtRequest request, int size) throws IOException {
        String hash = DigestUtils.md5Hex(request.getKey());
        String encoding = request.coverArt != null ? "jpeg" : "png";
        Path cachedImage = getImageCacheDirectory(size).resolve(hash + "." + encoding);

        // Synchronize to avoid concurrent writing to the same file.
        synchronized (hash.intern()) {

            // Is cache missing or obsolete?
            if (!Files.exists(cachedImage) || request.lastModified().isAfter(FileUtil.lastModified(cachedImage))) {
//                LOG.info("Cache MISS - " + request + " (" + size + ")");
                ImageWriter writer = null;

                try (OutputStream os = Files.newOutputStream(cachedImage);
                        BufferedOutputStream bos = new BufferedOutputStream(os);
                        ImageOutputStream out = ImageIO.createImageOutputStream(bos)) {
                    semaphore.acquire();
                    BufferedImage image = request.createImage(size);
                    if (image == null) {
                        throw new Exception("Unable to decode image.");
                    }
                    writer = ImageIO.getImageWritersByFormatName(encoding).next();

                    float quality = (float) (settingsService.getCoverArtQuality() / 100.0);
                    ImageWriteParam params = writer.getDefaultWriteParam();
                    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(quality); // default is 0.75

                    writer.setOutput(out);
                    writer.write(null, new IIOImage(image, null, null), params);

                } catch (Throwable x) {
                    // Delete corrupt (probably empty) thumbnail cache.
                    LOG.warn("Failed to create thumbnail for {}", request, x);
                    FileUtil.delete(cachedImage);
                    throw new IOException("Failed to create thumbnail for " + request + ". " + x.getMessage());
                } finally {
                    if (writer != null) {
                        writer.dispose();
                        writer = null;
                    }
                    semaphore.release();
                }
            } else {
//                LOG.info("Cache HIT - " + request + " (" + size + ")");
            }
            return cachedImage;
        }
    }

    /**
     * Returns an input stream to the image in the given file.  If the file is an audio file,
     * the embedded album art is returned.
     */
    private InputStream getImageInputStream(CoverArt art) throws IOException {
        return getImageInputStreamWithType(coverArtService.getFullPath(art)).getLeft();
    }

    /**
     * Returns an input stream to the image in the given file.  If the file is an audio file,
     * the embedded album art is returned. In addition returns the mime type
     */
    private Pair<InputStream, String> getImageInputStreamWithType(Path file) throws IOException {
        InputStream is;
        String mimeType;
        if (jaudiotaggerParser.isApplicable(file)) {
            LOG.trace("Using Jaudio Tagger for reading artwork from {}", file);
            try {
                LOG.trace("Reading artwork from file {}", file);
                Artwork artwork = JaudiotaggerParser.getArtwork(file);
                is = new ByteArrayInputStream(artwork.getBinaryData());
                mimeType = artwork.getMimeType();
            } catch (Exception e) {
                LOG.debug("Could not read artwork from file {}", file);
                throw new RuntimeException(e);
            }
        } else {
            is = new BufferedInputStream(Files.newInputStream(file));
            mimeType = StringUtil.getMimeType(MoreFiles.getFileExtension(file));
        }
        return Pair.of(is, mimeType);
    }

    private InputStream getImageInputStreamForVideo(MediaFile mediaFile, int width, int height, int offset) throws Exception {
        VideoTranscodingSettings videoSettings = new VideoTranscodingSettings(width, height, offset, 0);
        TranscodingService.Parameters parameters = new TranscodingService.Parameters(mediaFile, videoSettings);
        String command = settingsService.getVideoImageCommand();
        parameters.setTranscoding(new Transcoding(null, null, null, null, command, null, null, false));
        return transcodingService.getTranscodedInputStream(parameters);
    }

    private synchronized Path getImageCacheDirectory(int size) {
        Path dir = SettingsService.getAirsonicHome().resolve("thumbs").resolve(String.valueOf(size));
        if (!Files.exists(dir)) {
            try {
                dir = Files.createDirectories(dir);
                LOG.info("Created thumbnail cache {}", dir);
            } catch (Exception e) {
                LOG.error("Failed to create thumbnail cache {}", dir, e);
            }
        }

        return dir;
    }

    public static BufferedImage scale(BufferedImage image, int width, int height) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage thumb = image;

        // For optimal results, use step by step bilinear resampling - halfing the size at each step.
        do {
            w /= 2;
            h /= 2;
            if (w < width) {
                w = width;
            }
            if (h < height) {
                h = height;
            }

            BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(thumb, 0, 0, temp.getWidth(), temp.getHeight(), null);
            g2.dispose();

            thumb = temp;
        } while (w != width);

        return thumb;
    }


    private abstract class CoverArtRequest {
        protected CoverArt coverArt;
        protected Supplier<String> keyGenerator;
        protected Supplier<Instant> lastModifiedGenerator;

        private CoverArtRequest() {
        }

        private CoverArtRequest(CoverArt coverArt, Supplier<String> keyGenerator, Supplier<Instant> lastModifiedGenerator) {
            this.coverArt = CoverArt.NULL_ART.equals(coverArt) ? null : coverArt;
            this.keyGenerator = keyGenerator;
            this.lastModifiedGenerator = lastModifiedGenerator;
        }

        public String getKey() {
            return Optional.ofNullable(coverArt).map(c -> coverArt.getFolderId() + "/" + coverArt.getPath())
                    .orElseGet(keyGenerator);
        }

        public Instant lastModified() {
            return Optional.ofNullable(coverArt).map(c -> FileUtil.lastModified(coverArtService.getFullPath(c)))
                    .orElseGet(lastModifiedGenerator);
        }

        public BufferedImage createImage(int size) {
            if (coverArt != null) {
                try (InputStream in = getImageInputStream(coverArt)) {
                    String reason = null;
                    if (in == null) {
                        reason = "getImageInputStream";
                    } else {
                        BufferedImage bimg = ImageIO.read(in);
                        if (bimg == null) {
                            reason = "ImageIO.read";
                        } else {
                            return scale(bimg, size, size);
                        }
                    }
                    LOG.warn("Failed to process cover art {}: {} failed", coverArt, reason);
                } catch (Throwable x) {
                    LOG.warn("Failed to process cover art {}", coverArt, x);
                }
            }
            return createAutoCover(size, size);
        }

        protected BufferedImage createAutoCover(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            AutoCover autoCover = new AutoCover(graphics, getKey(), getArtist(), getAlbum(), width, height);
            autoCover.paintCover();
            graphics.dispose();
            return image;
        }

        public abstract String getAlbum();

        public abstract String getArtist();
    }

    private class ArtistCoverArtRequest extends CoverArtRequest {

        private final Artist artist;

        private ArtistCoverArtRequest(Artist artist) {
            super(coverArtService.get(EntityType.ARTIST, artist.getId()),
                () -> ARTIST_COVERART_PREFIX + artist.getId(),
                () -> artist.getLastScanned());
            this.artist = artist;
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return artist.getName();
        }

        @Override
        public String toString() {
            return "Artist " + artist.getId() + " - " + artist.getName();
        }
    }

    private class AlbumCoverArtRequest extends CoverArtRequest {

        private final Album album;

        private AlbumCoverArtRequest(Album album) {
            super(coverArtService.get(EntityType.ALBUM, album.getId()),
                () -> ALBUM_COVERART_PREFIX + album.getId(),
                () -> album.getLastScanned());
            this.album = album;
        }

        @Override
        public String getAlbum() {
            return album.getName();
        }

        @Override
        public String getArtist() {
            return album.getArtist();
        }

        @Override
        public String toString() {
            return "Album " + album.getId() + " - " + album.getName();
        }
    }

    private class PlaylistCoverArtRequest extends CoverArtRequest {

        private final Playlist playlist;

        private PlaylistCoverArtRequest(Playlist playlist) {
            super(null, () -> PLAYLIST_COVERART_PREFIX + playlist.getId(), () -> playlist.getChanged());
            this.playlist = playlist;
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return playlist.getName();
        }

        @Override
        public String toString() {
            return "Playlist " + playlist.getId() + " - " + playlist.getName();
        }

        @Override
        public BufferedImage createImage(int size) {
            List<MediaFile> albums = getRepresentativeAlbums();
            if (albums.isEmpty()) {
                return createAutoCover(size, size);
            }
            if (albums.size() < 4) {
                return new MediaFileCoverArtRequest(albums.get(0)).createImage(size);
            }

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();

            int half = size / 2;
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(0)).createImage(half), null, 0, 0);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(1)).createImage(half), null, half, 0);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(2)).createImage(half), null, 0, half);
            graphics.drawImage(new MediaFileCoverArtRequest(albums.get(3)).createImage(half), null, half, half);
            graphics.dispose();
            return image;
        }

        private List<MediaFile> getRepresentativeAlbums() {
            return playlistService.getFilesInPlaylist(playlist.getId())
                    .parallelStream()
                    .map(mediaFileService::getParentOf)
                    .filter(album -> album != null && !mediaFileService.isRoot(album))
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    private class PodcastCoverArtRequest extends CoverArtRequest {

        private final PodcastChannel channel;

        PodcastCoverArtRequest(PodcastChannel channel) {
            super(null, () -> PODCAST_COVERART_PREFIX + channel.getId(), () -> Instant.ofEpochMilli(-1));
            this.channel = channel;
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return channel.getTitle() != null ? channel.getTitle() : channel.getUrl();
        }
    }

    private class MediaFileCoverArtRequest extends CoverArtRequest {
        private final MediaFile dir;
        private final Integer proxyId;

        private MediaFileCoverArtRequest(MediaFile mediaFile, Integer proxyId) {
            super(coverArtService.get(EntityType.MEDIA_FILE, mediaFile.getId()),
                () -> mediaFile.getFolderId() + "/" + mediaFile.getPath(),
                () -> mediaFile.getChanged());
            this.dir = mediaFile;
            this.proxyId = proxyId;
        }

        private MediaFileCoverArtRequest(MediaFile mediaFile) {
            this(mediaFile, null);
        }

        @Override
        public String getAlbum() {
            return dir.getName();
        }

        @Override
        public String getArtist() {
            return dir.getAlbumArtist() != null ? dir.getAlbumArtist() : dir.getArtist();
        }

        @Override
        public String toString() {
            return "Media file " + dir.getId() + " - " + dir + (proxyId == null ? "" : " (Proxy for " + proxyId + ")");
        }
    }

    private class VideoCoverArtRequest extends CoverArtRequest {

        private final MediaFile mediaFile;
        private final int offset;

        private VideoCoverArtRequest(MediaFile mediaFile, int offset) {
            this.mediaFile = mediaFile;
            this.offset = offset;
            keyGenerator = () -> mediaFile.getFolderId() + "/" + mediaFile.getPath() + "/" + offset;
            lastModifiedGenerator = () -> mediaFile.getChanged();
        }

        @Override
        public BufferedImage createImage(int size) {
            int height = size;
            int width = height * 16 / 9;
            try (InputStream in = getImageInputStreamForVideo(mediaFile, width, height, offset)) {
                BufferedImage result = ImageIO.read(in);
                if (result != null) {
                    return result;
                }
                LOG.warn("Failed to process cover art for {}: {}", mediaFile, result);
            } catch (Throwable x) {
                LOG.warn("Failed to process cover art for {}", mediaFile, x);
            }
            return createAutoCover(width, height);
        }

        @Override
        public String getAlbum() {
            return null;
        }

        @Override
        public String getArtist() {
            return mediaFile.getName();
        }

        @Override
        public String toString() {
            return "Video file " + mediaFile.getId() + " - " + mediaFile;
        }
    }

    static class AutoCover {

        private final static int[] COLORS = {0x33B5E5, 0xAA66CC, 0x99CC00, 0xFFBB33, 0xFF4444};
        private final Graphics2D graphics;
        private final String artist;
        private final String album;
        private final int width;
        private final int height;
        private final Color color;

        AutoCover(Graphics2D graphics, String key, String artist, String album, int width, int height) {
            this.graphics = graphics;
            this.artist = artist;
            this.album = album;
            this.width = width;
            this.height = height;

            int hash = key.hashCode();
            int rgb = COLORS[Math.abs(hash) % COLORS.length];
            this.color = new Color(rgb);
        }

        public void paintCover() {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            graphics.setPaint(color);
            graphics.fillRect(0, 0, width, height);

            int y = height * 2 / 3;
            graphics.setPaint(new GradientPaint(0, y, new Color(82, 82, 82), 0, height, Color.BLACK));
            graphics.fillRect(0, y, width, height / 3);

            graphics.setPaint(Color.WHITE);
            float fontSize = 3.0f + height * 0.07f;
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, (int) fontSize);
            graphics.setFont(font);

            if (album != null) {
                graphics.drawString(album, width * 0.05f, height * 0.6f);
            }
            if (artist != null) {
                graphics.drawString(artist, width * 0.05f, height * 0.8f);
            }

            int borderWidth = height / 50;
            graphics.fillRect(0, 0, borderWidth, height);
            graphics.fillRect(width - borderWidth, 0, height - borderWidth, height);
            graphics.fillRect(0, 0, width, borderWidth);
            graphics.fillRect(0, height - borderWidth, width, height);
        }
    }
}
