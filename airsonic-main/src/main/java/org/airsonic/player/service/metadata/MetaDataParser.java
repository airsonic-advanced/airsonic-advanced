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

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.MediaFolderService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jaudiotagger.tag.reference.GenreTypes;

import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses meta data from media files.
 *
 * @author Sindre Mehus
 */
public abstract class MetaDataParser {

    protected static final Pattern GENRE_PATTERN = Pattern.compile("\\((\\d+)\\).*");
    protected static final Pattern TRACK_NUMBER_PATTERN = Pattern.compile("(\\d+)/\\d+");
    protected static final Pattern YEAR_NUMBER_PATTERN = Pattern.compile("(\\d{4}).*");

    /**
     * Parses meta data for the given file.
     *
     * @param file The file to parse.
     * @return Meta data for the file, never null.
     */
    public MetaData getMetaData(Path file) {

        MetaData metaData = getRawMetaData(file);
        String artist = metaData.getArtist();
        String albumArtist = metaData.getAlbumArtist();
        String album = metaData.getAlbumName();
        String title = metaData.getTitle();

        if (artist == null) {
            artist = guessArtist(file);
        }
        if (albumArtist == null) {
            albumArtist = guessArtist(file);
        }
        if (album == null) {
            album = guessAlbum(file, artist);
        }
        if (title == null) {
            title = guessTitle(file);
        }

        title = removeTrackNumberFromTitle(title, metaData.getTrackNumber());
        metaData.setArtist(artist);
        metaData.setAlbumArtist(albumArtist);
        metaData.setAlbumName(album);
        metaData.setTitle(title);

        return metaData;
    }

    /**
     * Parses meta data for the given file. No guessing or reformatting is done.
     *
     *
     * @param file The file to parse.
     * @return Meta data for the file.
     */
    public abstract MetaData getRawMetaData(Path file);

    /**
     * Updates the given file with the given meta data.
     *
     * @param file     The file to update.
     * @param metaData The new meta data.
     */
    public abstract void setMetaData(MediaFile file, MetaData metaData);

    /**
     * Returns whether this parser is applicable to the given file.
     *
     * @param path The path to file in question.
     * @return Whether this parser is applicable to the given file.
     */
    public abstract boolean isApplicable(Path path);

    /**
     * Returns whether this parser supports tag editing (using the {@link #setMetaData} method).
     *
     * @return Whether tag editing is supported.
     */
    public abstract boolean isEditingSupported();

    abstract MediaFolderService getMediaFolderService();

    /**
     * Guesses the artist for the given file.
     */
    String guessArtist(Path file) {
        Path parent = file.getParent();
        if (isRoot(parent)) {
            return null;
        }
        Path grandParent = parent.getParent();
        return isRoot(grandParent) ? null : grandParent.getFileName().toString();
    }

    /**
     * Guesses the album for the given file.
     *
     * TODO: public as it is used in EditTagsController
     */
    public String guessAlbum(Path file, String artist) {
        Path parent = file.getParent();
        String album = isRoot(parent) ? null : parent.getFileName().toString();
        if (artist != null && album != null) {
            album = album.replace(artist + " - ", "");
        }
        return album;
    }

    /**
     * Guesses the title for the given file.
     *
     * TODO: public as it is used in EditTagsController
     */
    public static String guessTitle(Path file) {
        return StringUtils.trim(FilenameUtils.getBaseName(file.toString()));
    }

    private boolean isRoot(Path file) {
        return getMediaFolderService().getAllMusicFolders(false, true).parallelStream()
                .anyMatch(folder -> file.equals(folder.getPath()));
    }

    /**
     * Returns all tags supported by id3v1.
     */
    public static SortedSet<String> getID3V1Genres() {
        return new TreeSet<String>(GenreTypes.getInstanceOf().getAlphabeticalValueList());
    }

    /**
     * Sometimes the genre is returned as "(17)" or "(17)Rock", instead of "Rock".  This method
     * maps the genre ID to the corresponding text.
     */
    String mapGenre(String genre) {
        if (genre == null) {
            return null;
        }
        Matcher matcher = GENRE_PATTERN.matcher(genre);
        if (matcher.matches()) {
            int genreId = Integer.parseInt(matcher.group(1));
            if (genreId >= 0 && genreId < GenreTypes.getInstanceOf().getSize()) {
                return GenreTypes.getInstanceOf().getValueForId(genreId);
            }
        }
        return genre;
    }

    /**
     * Parses the track/disc number from the given string.  Also supports
     * numbers on the form "4/12".
     */
    Integer parseTrackNumber(String trackNumber) {
        if (trackNumber == null) {
            return null;
        }

        Integer result = null;

        try {
            result = Integer.valueOf(trackNumber);
        } catch (NumberFormatException x) {
            Matcher matcher = TRACK_NUMBER_PATTERN.matcher(trackNumber);
            if (matcher.matches()) {
                try {
                    result = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (Integer.valueOf(0).equals(result)) {
            return null;
        }
        return result;
    }

    Integer parseYear(String year) {
        if (year == null) {
            return null;
        }

        Integer result = null;

        try {
            result = Integer.valueOf(year);
        } catch (NumberFormatException x) {
            Matcher matcher = YEAR_NUMBER_PATTERN.matcher(year);
            if (matcher.matches()) {
                try {
                    result = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (Integer.valueOf(0).equals(result)) {
            return null;
        }
        return result;
    }

    Integer parseInteger(String s) {
        s = StringUtils.trimToNull(s);
        if (s == null) {
            return null;
        }
        try {
            Integer result = Integer.valueOf(s);
            if (Integer.valueOf(0).equals(result)) {
                return null;
            }
            return result;
        } catch (NumberFormatException x) {
            return null;
        }
    }

    Integer parseIntegerPattern(String str, Pattern pattern) {
        str = StringUtils.trimToNull(str);

        if (str == null) {
            return null;
        }

        Integer result = null;

        try {
            result = Integer.valueOf(str);
        } catch (NumberFormatException x) {
            if (pattern == null) {
                return null;
            }
            Matcher matcher = pattern.matcher(str);
            if (matcher.matches()) {
                try {
                    result = Integer.valueOf(matcher.group(1));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (Integer.valueOf(0).equals(result)) {
            return null;
        }
        return result;
    }

    /**
     * Removes any prefixed track number from the given title string.
     *
     * @param title       The title with or without a prefixed track number, e.g., "02 - Back In Black".
     * @param trackNumber If specified, this is the "true" track number.
     * @return The title with the track number removed, e.g., "Back In Black".
     */
    String removeTrackNumberFromTitle(String title, Integer trackNumber) {
        title = title.trim();

        // Don't remove numbers if true track number is missing, or if title does not start with it.
        if (trackNumber == null || !title.matches("0?" + trackNumber + "[. -].*")) {
            return title;
        }

        String result = title.replaceFirst("^\\d{2}[. -]+", "");
        return result.isEmpty() ? title : result;
    }
}
