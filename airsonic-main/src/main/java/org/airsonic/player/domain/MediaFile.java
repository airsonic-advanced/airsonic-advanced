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
package org.airsonic.player.domain;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A media file (audio, video or directory) with an assortment of its meta data.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class MediaFile {

    private Integer id;
    private String path;
    private Integer folderId;
    private MediaType mediaType;
    private String format;
    private String title;
    private String albumName;
    private String artist;
    private String albumArtist;
    private Integer discNumber;
    private Integer trackNumber;
    private Integer year;
    private String genre;
    private Integer bitRate;
    private boolean variableBitRate;
    private Double duration;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String coverArtPath;
    private String parentPath;
    private int playCount;
    private Instant lastPlayed;
    private String comment;
    private Instant created;
    private Instant changed;
    private Instant lastScanned;
    private Instant starredDate;
    private Instant childrenLastUpdated;
    private boolean present;
    private int version;
    private String musicBrainzReleaseId;
    private String musicBrainzRecordingId;

    public MediaFile(Integer id, String path, Integer folderId, MediaType mediaType, String format, String title,
                     String albumName, String artist, String albumArtist, Integer discNumber, Integer trackNumber, Integer year, String genre, Integer bitRate,
                     boolean variableBitRate, Double duration, Long fileSize, Integer width, Integer height, String coverArtPath,
                     String parentPath, int playCount, Instant lastPlayed, String comment, Instant created, Instant changed, Instant lastScanned,
                     Instant childrenLastUpdated, boolean present, int version, String musicBrainzReleaseId, String musicBrainzRecordingId) {
        this.id = id;
        this.path = path;
        this.folderId = folderId;
        this.mediaType = mediaType;
        this.format = format;
        this.title = title;
        this.albumName = albumName;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.discNumber = discNumber;
        this.trackNumber = trackNumber;
        this.year = year;
        this.genre = genre;
        this.bitRate = bitRate;
        this.variableBitRate = variableBitRate;
        this.duration = duration;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.coverArtPath = coverArtPath;
        this.parentPath = parentPath;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.comment = comment;
        this.created = created;
        this.changed = changed;
        this.lastScanned = lastScanned;
        this.childrenLastUpdated = childrenLastUpdated;
        this.present = present;
        this.version = version;
        this.musicBrainzReleaseId = musicBrainzReleaseId;
        this.musicBrainzRecordingId = musicBrainzRecordingId;
    }

    public MediaFile() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Path getRelativePath() {
        return Paths.get(path);
    }

    public Path getFullPath(Path relativeMediaFolderPath) {
        return relativeMediaFolderPath.resolve(path);
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public boolean isVideo() {
        return mediaType == MediaType.VIDEO;
    }

    public boolean isAudio() {
        return mediaType == MediaType.MUSIC || mediaType == MediaType.AUDIOBOOK || mediaType == MediaType.PODCAST;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isDirectory() {
        return !isFile();
    }

    public boolean isFile() {
        return mediaType != MediaType.DIRECTORY && mediaType != MediaType.ALBUM;
    }

    public boolean isAlbum() {
        return mediaType == MediaType.ALBUM;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String album) {
        this.albumName = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getName() {
        return title != null ? title : isDirectory() ? FilenameUtils.getName(path) : FilenameUtils.getBaseName(path);
    }

    public Integer getDiscNumber() {
        return discNumber;
    }

    public void setDiscNumber(Integer discNumber) {
        this.discNumber = discNumber;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getBitRate() {
        return bitRate;
    }

    public void setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
    }

    public boolean isVariableBitRate() {
        return variableBitRate;
    }

    public void setVariableBitRate(boolean variableBitRate) {
        this.variableBitRate = variableBitRate;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getCoverArtPath() {
        return coverArtPath;
    }

    public Path getRelativeCoverArtPath() {
        return coverArtPath == null ? null : Paths.get(coverArtPath);
    }

    public Path getFullCoverArtPath(Path relativeMediaFolderPath) {
        return coverArtPath == null ? null : relativeMediaFolderPath.resolve(coverArtPath);
    }

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }


    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public Instant getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Instant lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getChanged() {
        return changed;
    }

    public void setChanged(Instant changed) {
        this.changed = changed;
    }

    public Instant getLastScanned() {
        return lastScanned;
    }

    public void setLastScanned(Instant lastScanned) {
        this.lastScanned = lastScanned;
    }

    public Instant getStarredDate() {
        return starredDate;
    }

    public void setStarredDate(Instant starredDate) {
        this.starredDate = starredDate;
    }

    public String getMusicBrainzReleaseId() {
        return musicBrainzReleaseId;
    }

    public void setMusicBrainzReleaseId(String musicBrainzReleaseId) {
        this.musicBrainzReleaseId = musicBrainzReleaseId;
    }

    public String getMusicBrainzRecordingId() {
        return musicBrainzRecordingId;
    }

    public void setMusicBrainzRecordingId(String musicBrainzRecordingId) {
        this.musicBrainzRecordingId = musicBrainzRecordingId;
    }

    /**
     * Returns when the children was last updated in the database.
     */
    public Instant getChildrenLastUpdated() {
        return childrenLastUpdated;
    }

    public void setChildrenLastUpdated(Instant childrenLastUpdated) {
        this.childrenLastUpdated = childrenLastUpdated;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MediaFile other = (MediaFile) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (!folderId.equals(other.folderId))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, folderId);
    }

    @Override
    public String toString() {
        return getName();
    }

    public static List<Integer> toIdList(List<MediaFile> from) {
        return from.stream().map(toId()).collect(Collectors.toList());
    }

    public static Function<MediaFile, Integer> toId() {
        return from -> from.getId();
    }

    public static enum MediaType {
        MUSIC,
        PODCAST,
        AUDIOBOOK,
        VIDEO,
        DIRECTORY,
        ALBUM
    }
}
