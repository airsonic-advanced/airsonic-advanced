package org.airsonic.player.ajax;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.util.StringUtil;

import java.time.Instant;
import java.util.Locale;

public class MediaFileEntry {
    private final int id;
    private final Integer trackNumber;
    private final Integer discNumber;
    private final String title;
    private String artist;
    private String albumArtist;
    private String album;
    private final String genre;
    private final Integer year;
    private final String bitRate;
    private final String dimensions;
    private final Double duration;
    private final String format;
    private String contentType;
    private final String entryType;
    private final String fileSize;
    private final int playCount;
    private final Instant lastPlayed;
    private final Instant created;
    private final Instant changed;
    private final Instant lastScanned;
    private final boolean starred;
    private final boolean present;
    private final String albumUrl;
    private final String streamUrl;
    private final String remoteStreamUrl;
    private final String coverArtUrl;
    private final String remoteCoverArtUrl;

    public MediaFileEntry(
            int id,
            Integer trackNumber,
            Integer discNumber,
            String title,
            String artist,
            String albumArtist,
            String album,
            String genre,
            Integer year,
            String bitRate,
            String dimensions,
            Double duration,
            String format,
            String contentType,
            String entryType,
            String fileSize,
            int playCount,
            Instant lastPlayed,
            Instant created,
            Instant changed,
            Instant lastScanned,
            boolean starred,
            boolean present,
            String albumUrl,
            String streamUrl,
            String remoteStreamUrl,
            String coverArtUrl,
            String remoteCoverArtUrl) {
        this.id = id;
        this.trackNumber = trackNumber;
        this.discNumber = discNumber;
        this.title = title;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.album = album;
        this.genre = genre;
        this.year = year;
        this.bitRate = bitRate;
        this.dimensions = dimensions;
        this.duration = duration;
        this.format = format;
        this.contentType = contentType;
        this.entryType = entryType;
        this.fileSize = fileSize;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.created = created;
        this.changed = changed;
        this.lastScanned = lastScanned;
        this.starred = starred;
        this.present = present;
        this.albumUrl = albumUrl;
        this.streamUrl = streamUrl;
        this.remoteStreamUrl = remoteStreamUrl;
        this.coverArtUrl = coverArtUrl;
        this.remoteCoverArtUrl = remoteCoverArtUrl;
    }

    public int getId() {
        return id;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public Integer getYear() {
        return year;
    }

    public String getBitRate() {
        return bitRate;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getEntryType() {
        return entryType;
    }

    public Double getDuration() {
        return duration;
    }

    public String getFormat() {
        return format;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public boolean getStarred() {
        return starred;
    }

    public boolean getPresent() {
        return present;
    }

    public String getAlbumUrl() {
        return albumUrl;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getRemoteStreamUrl() {
        return remoteStreamUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public String getRemoteCoverArtUrl() {
        return remoteCoverArtUrl;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getPlayCount() {
        return playCount;
    }

    public Instant getLastPlayed() {
        return lastPlayed;
    }

    public Instant getCreated() {
        return created;
    }

    public Instant getChanged() {
        return changed;
    }

    public Instant getLastScanned() {
        return lastScanned;
    }

    public static MediaFileEntry fromMediaFile(MediaFile file, Locale locale, boolean starred, boolean folderAccess, String streamUrl, String remoteStreamUrl, String remoteCoverArtUrl) {
        return new MediaFileEntry(file.getId(), file.getTrackNumber(), file.getDiscNumber(), file.getName(),
                file.getArtist(), file.getAlbumArtist(), file.getAlbumName(), file.getGenre(), file.getYear(), formatBitRate(file),
                (file.getWidth() != null && file.getHeight() != null) ? file.getWidth() + "x" + file.getHeight() : null,
                file.getDuration(), file.getFormat(), StringUtil.getMimeType(file.getFormat()), file.getMediaType().toString(),
                StringUtil.formatBytes(file.getFileSize(), locale == null ? Locale.ENGLISH : locale),
                file.getPlayCount(), file.getLastPlayed(), file.getCreated(), file.getChanged(), file.getLastScanned(),
                starred, file.isPresent() && folderAccess, "main.view?id=" + file.getId(), streamUrl, remoteStreamUrl,
                "coverArt.view?id=" + file.getId(), remoteCoverArtUrl);
    }

    private static String formatBitRate(MediaFile mediaFile) {
        if (mediaFile.getBitRate() == null) {
            return null;
        }
        if (mediaFile.isVariableBitRate()) {
            return "vbr " + mediaFile.getBitRate() + " Kbps";
        }
        return mediaFile.getBitRate() + " Kbps";
    }

}