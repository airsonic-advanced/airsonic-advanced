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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A media file (audio, video or directory) with an assortment of its meta data.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class MediaFile {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFile.class);

    private int id;
    private String path;
    private String folder;
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
    private Integer startPosition;

    public MediaFile(int id, String path, String folder, MediaType mediaType, String format, String title,
                     String albumName, String artist, String albumArtist, Integer discNumber, Integer trackNumber, Integer year, String genre, Integer bitRate,
                     boolean variableBitRate, Double duration, Long fileSize, Integer width, Integer height, String coverArtPath,
                     String parentPath, int playCount, Instant lastPlayed, String comment, Instant created, Instant changed, Instant lastScanned,
                     Instant childrenLastUpdated, boolean present, int version, String musicBrainzReleaseId, String musicBrainzRecordingId,
                     Integer startPosition) {
        this.id = id;
        this.path = path;
        this.folder = folder;
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
        this.startPosition = startPosition;
    }

    public MediaFile() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public Path getFile() {
        // TODO: Optimize
        if (isSingleFile()) {
            return Paths.get(getSingleFileMediaPath());
        }
        return Paths.get(path);
    }

    public boolean exists() {
        return Files.exists(getFile());
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
        return MediaType.audioTypes().contains(mediaType.toString());
    }

    public boolean isSingleFile() {
        return mediaType == MediaType.MUSIC_SINGLE_FILE || mediaType == MediaType.AUDIOBOOK_SINGLE_FILE;
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
        return MediaType.playableTypes().contains(mediaType.toString());
    }

    public boolean isAlbum() {
        return MediaType.albumTypes().contains(mediaType.toString());
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
        if (isFile()) {
            return title != null ? title : FilenameUtils.getBaseName(path);
        }

        return FilenameUtils.getName(path);
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

    public void setCoverArtPath(String coverArtPath) {
        this.coverArtPath = coverArtPath;
    }


    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public Path getParentFile() {
        return getFile().getParent();
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

    public Integer getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
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
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    public Path getCoverArtFile() {
        // TODO: Optimize
        return coverArtPath == null ? null : Paths.get(coverArtPath);
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

    // concatenate file path with track start to create unique path for indexed media
    public void setSingleFileMediaPath(String mediaFilePath, int songStart) {
        setPath(mediaFilePath + ":" + songStart);
    }

    // return path without track start marker (if present)
    public String getSingleFileMediaPath() {
        try {
            String[] parts = FilenameUtils.getExtension(path).split(":");
            return FilenameUtils.getFullPath(path) + FilenameUtils.getBaseName(path) + FilenameUtils.EXTENSION_SEPARATOR + parts[parts.length - 2];
        } catch (ArrayIndexOutOfBoundsException e) {
            // normal file
            return path;
        }
    }

    public static enum MediaType {
        MUSIC,
        MUSIC_SINGLE_FILE,
        PODCAST,
        AUDIOBOOK,
        AUDIOBOOK_SINGLE_FILE,
        VIDEO,
        DIRECTORY,
        ALBUM,
        ALBUM_SINGLE_FILE;

        private static final List<String> ALBUM_TYPES = Arrays.asList(ALBUM.toString(),ALBUM_SINGLE_FILE.toString());
        private static final List<String> MUSIC_TYPES = Arrays.asList(MUSIC.toString(),MUSIC_SINGLE_FILE.toString());
        private static final List<String> AUDIO_TYPES = Arrays.asList(MUSIC.toString(),MUSIC_SINGLE_FILE.toString(),AUDIOBOOK.toString(),AUDIOBOOK_SINGLE_FILE.toString(),PODCAST.toString());
        private static final List<String> PLAYABLE_TYPES = Arrays.asList(MUSIC.toString(),MUSIC_SINGLE_FILE.toString(),AUDIOBOOK.toString(),AUDIOBOOK_SINGLE_FILE.toString(),PODCAST.toString(),VIDEO.toString());

        public static List<String> albumTypes() {
            return ALBUM_TYPES;
        }

        public static List<String> musicTypes() {
            return MUSIC_TYPES;
        }

        public static List<String> audioTypes() {
            return AUDIO_TYPES;
        }

        public static List<String> playableTypes() {
            return PLAYABLE_TYPES;
        }
    }
}
