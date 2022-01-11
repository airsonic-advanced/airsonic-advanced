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
package org.airsonic.player.service.metadata;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.MoreFiles;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SettingsService;
import org.apache.commons.lang.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.reference.PictureTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.logging.LogManager;

/**
 * Parses meta data from audio files using the Jaudiotagger library
 * (http://www.jthink.net/jaudiotagger/)
 *
 * @author Sindre Mehus
 */
@Service
@Order(0)
public class JaudiotaggerParser extends MetaDataParser {

    private static final Logger LOG = LoggerFactory.getLogger(JaudiotaggerParser.class);
    @Autowired
    private final SettingsService settingsService;

    public JaudiotaggerParser(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    static {
        try {
            LogManager.getLogManager().reset();
        } catch (Throwable x) {
            LOG.warn("Failed to turn off logging from Jaudiotagger.", x);
        }
    }

    /**
     * Parses meta data for the given music file. No guessing or reformatting is done.
     *
     *
     * @param file The music file to parse.
     * @return Meta data for the file.
     */
    @Override
    public MetaData getRawMetaData(Path file) {

        MetaData metaData = new MetaData();

        try {
            AudioFile audioFile = AudioFileIO.read(file.toFile());
            Tag tag = audioFile.getTag();
            if (tag != null) {
                metaData.setAlbumName(getTagField(tag, FieldKey.ALBUM));
                metaData.setTitle(getTagField(tag, FieldKey.TITLE));
                metaData.setYear(parseIntegerPattern(getTagField(tag, FieldKey.YEAR), YEAR_NUMBER_PATTERN));
                metaData.setGenre(mapGenre(getTagField(tag, FieldKey.GENRE)));
                metaData.setDiscNumber(parseIntegerPattern(getTagField(tag, FieldKey.DISC_NO), null));
                metaData.setTrackNumber(parseIntegerPattern(getTagField(tag, FieldKey.TRACK), TRACK_NUMBER_PATTERN));
                metaData.setMusicBrainzReleaseId(getTagField(tag, FieldKey.MUSICBRAINZ_RELEASEID));
                metaData.setMusicBrainzRecordingId(getTagField(tag, FieldKey.MUSICBRAINZ_TRACK_ID));

                metaData.setArtist(getTagField(tag, FieldKey.ARTIST));
                metaData.setAlbumArtist(getTagField(tag, FieldKey.ALBUM_ARTIST));

                if (StringUtils.isBlank(metaData.getArtist())) {
                    metaData.setArtist(metaData.getAlbumArtist());
                }
                if (StringUtils.isBlank(metaData.getAlbumArtist())) {
                    metaData.setAlbumArtist(metaData.getArtist());
                }

            }

            AudioHeader audioHeader = audioFile.getAudioHeader();
            if (audioHeader != null) {
                metaData.setVariableBitRate(audioHeader.isVariableBitRate());
                metaData.setBitRate((int) audioHeader.getBitRateAsNumber());
                metaData.setDuration(audioHeader.getPreciseTrackLength());
            }


        } catch (Throwable x) {
            LOG.warn("Error when parsing tags in {}", file, x);
        }

        return metaData;
    }

    private static String getTagField(Tag tag, FieldKey fieldKey) {
        try {
            return StringUtils.replace(StringUtils.trimToNull(tag.getFirst(fieldKey)), "\0", " ");
        } catch (Exception x) {
            // Ignored.
            return null;
        }
    }

    /**
     * Updates the given file with the given meta data.
     *
     * @param file     The music file to update.
     * @param metaData The new meta data.
     */
    @Override
    public void setMetaData(MediaFile file, MetaData metaData) {

        try {
            AudioFile audioFile = AudioFileIO.read(file.getFile().toFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            tag.setField(FieldKey.ARTIST, StringUtils.trimToEmpty(metaData.getArtist()));
            tag.setField(FieldKey.ALBUM, StringUtils.trimToEmpty(metaData.getAlbumName()));
            tag.setField(FieldKey.TITLE, StringUtils.trimToEmpty(metaData.getTitle()));
            tag.setField(FieldKey.GENRE, StringUtils.trimToEmpty(metaData.getGenre()));
            try {
                tag.setField(FieldKey.ALBUM_ARTIST, StringUtils.trimToEmpty(metaData.getAlbumArtist()));
            } catch (Exception x) {
                // Silently ignored. ID3v1 doesn't support album artist.
            }

            Integer track = metaData.getTrackNumber();
            if (track == null) {
                tag.deleteField(FieldKey.TRACK);
            } else {
                tag.setField(FieldKey.TRACK, String.valueOf(track));
            }

            Integer year = metaData.getYear();
            if (year == null) {
                tag.deleteField(FieldKey.YEAR);
            } else {
                tag.setField(FieldKey.YEAR, String.valueOf(year));
            }

            audioFile.commit();

        } catch (Throwable x) {
            LOG.warn("Failed to update tags for file {}", file, x);
            throw new RuntimeException("Failed to update tags for file " + file + ". " + x.getMessage(), x);
        }
    }

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Always true.
     */
    @Override
    public boolean isEditingSupported() {
        return true;
    }

    @Override
    SettingsService getSettingsService() {
        return settingsService;
    }

    private static Set<String> applicableFormats = ImmutableSet.of("mp3", "m4a", "m4b", "m4p", "aac", "ogg", "flac", "wav", "mpc", "mp+", "aif", "dsf", "aiff", "wma");

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param path The path to music file in question.
     * @return Whether this parser is applicable to the given file.
     */
    @Override
    public boolean isApplicable(Path path) {
        return Files.isRegularFile(path) && applicableFormats.contains(MoreFiles.getFileExtension(path).toLowerCase());
    }

    /**
     * Returns whether cover art image data is available in the given file.
     *
     * @param file The music file.
     * @return Whether cover art image data is available.
     */
    public boolean isImageAvailable(MediaFile file) {
        try {
            return getArtwork(file) != null;
        } catch (Throwable x) {
            LOG.info("Failed to find cover art tag in {}", file, x);
            return false;
        }
    }

    public Artwork getArtwork(MediaFile file) throws Exception {
        AudioFile audioFile = AudioFileIO.read(file.getFile().toFile());
        Tag tag = audioFile.getTag();
        Artwork artwork = null;
        if (tag != null) {
            Optional<Artwork> artworkOptional = tag.getArtworkList().stream().filter(art -> art.getPictureType() == PictureTypes.DEFAULT_ID).findAny();
            artwork = artworkOptional.orElse(tag.getFirstArtwork());
        }
        return artwork;
    }
}
