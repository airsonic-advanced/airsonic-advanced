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
package org.airsonic.player.service;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.airsonic.player.ajax.MediaFileEntry;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.i18n.LocaleResolver;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.airsonic.player.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.digitalmediaserver.cuelib.CueParser;
import org.digitalmediaserver.cuelib.CueSheet;
import org.digitalmediaserver.cuelib.Position;
import org.digitalmediaserver.cuelib.TrackData;
import org.digitalmediaserver.cuelib.io.FLACReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides services for instantiating and caching media files and cover art.
 *
 * @author Sindre Mehus
 */
@Service
public class MediaFileService {

    private static final Logger LOG = LoggerFactory.getLogger(MediaFileService.class);

    @Autowired
    private SecurityService securityService;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private MediaFileDao mediaFileDao;
    @Autowired
    private MediaFolderService mediaFolderService;
    @Autowired
    private AlbumDao albumDao;
    @Autowired
    private JaudiotaggerParser parser;
    @Autowired
    private MetaDataParserFactory metaDataParserFactory;
    @Autowired
    private LocaleResolver localeResolver;
    private boolean memoryCacheEnabled = true;

    public MediaFile getMediaFile(String pathName) {
        return getMediaFile(Paths.get(pathName));
    }

    public MediaFile getMediaFile(Path fullPath) {
        return getMediaFile(fullPath, settingsService.isFastCacheEnabled());
    }

    // This may be an expensive op
    public MediaFile getMediaFile(Path fullPath, boolean minimizeDiskAccess) {
        MusicFolder folder = securityService.getMusicFolderForFile(fullPath, true, true);
        if (folder == null) {
            // can't look outside folders and not present in folder
            return null;
        }
        try {
            Path relativePath = folder.getPath().relativize(fullPath);
            return getMediaFile(relativePath, folder, minimizeDiskAccess);
        } catch (Exception e) {
            // ignore
            return null;
        }
    }

    public MediaFile getMediaFile(String relativePath, MusicFolder folder) {
        return getMediaFile(Paths.get(relativePath), folder);
    }

    public MediaFile getMediaFile(Path relativePath, MusicFolder folder) {
        return getMediaFile(relativePath, folder, settingsService.isFastCacheEnabled());
    }

    @Cacheable(cacheNames = "mediaFilePathCache", key = "#relativePath.toString().concat('-').concat(#folder.id)", condition = "#root.target.memoryCacheEnabled", unless = "#result == null")
    public MediaFile getMediaFile(Path relativePath, MusicFolder folder, boolean minimizeDiskAccess) {
        // Look in database.
        MediaFile result = mediaFileDao.getMediaFile(relativePath.toString(), folder.getId());
        if (result != null) {
            result = checkLastModified(result, folder, minimizeDiskAccess);
            return result;
        }

        if (!Files.exists(folder.getPath().resolve(relativePath))) {
            return null;
        }

        // Not found in database, must read from disk.
        result = createMediaFile(relativePath, folder, null);

        // Put in database.
        updateMediaFile(result);

        return result;
    }

    @Cacheable(cacheNames = "mediaFileIdCache", condition = "#root.target.memoryCacheEnabled", unless = "#result == null")
    public MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }

        return checkLastModified(mediaFile, mediaFolderService.getMusicFolderById(mediaFile.getFolderId()), settingsService.isFastCacheEnabled());
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPath(), mediaFolderService.getMusicFolderById(mediaFile.getFolderId()));
    }

    private MediaFile checkLastModified(MediaFile mediaFile, MusicFolder folder, boolean minimizeDiskAccess) {
        if (minimizeDiskAccess || (mediaFile.getVersion() >= MediaFileDao.VERSION
                && !settingsService.getFullScan()
                && mediaFile.getChanged().compareTo(FileUtil.lastModified(mediaFile.getFullPath(folder.getPath()))) > -1)) {
            LOG.debug("Detected unmodified file (id {}, path {} in folder {} ({}))", mediaFile.getId(), mediaFile.getPath(), folder.getId(), folder.getName());
            return mediaFile;
        }
        LOG.debug("Updating database file from disk (id {}, path {} in folder {} ({}))", mediaFile.getId(), mediaFile.getPath(), folder.getId(), folder.getName());
        mediaFile = createMediaFile(mediaFile.getRelativePath(), folder, mediaFile);
        updateMediaFile(mediaFile);
        return mediaFile;
    }

    /**
     * Returns all user-visible media files that are children of a given media file
     *
     * visibility depends on the return value of showMediaFile(mediaFile)
     *
     * @param sort               Whether to sort files in the same directory
     * @return All children media files which pass this::showMediaFile
     */
    public List<MediaFile> getVisibleChildrenOf(MediaFile parent, boolean includeDirectories, boolean sort) {
        return getChildrenOf(parent, true, includeDirectories, sort, settingsService.isFastCacheEnabled()).stream()
                .filter(this::showMediaFile)
                .collect(Collectors.toList());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort) {
        return getChildrenOf(parent, includeFiles, includeDirectories, sort, settingsService.isFastCacheEnabled());
    }

    /**
     * Returns all media files that are children of a given media file.
     *
     * @param includeFiles       Whether files should be included in the result.
     * @param includeDirectories Whether directories should be included in the result.
     * @param sort               Whether to sort files in the same directory.
     * @param minimizeDiskAccess Whether to refrain from checking for new or changed files
     * @return All children media files.
     */
    public List<MediaFile> getChildrenOf(MediaFile parent, boolean includeFiles, boolean includeDirectories, boolean sort, boolean minimizeDiskAccess) {

        if (!parent.isDirectory()) {
            return Collections.emptyList();
        }

        Stream<MediaFile> resultStream = null;

        // Make sure children are stored and up-to-date in the database.
        if (!minimizeDiskAccess) {
            resultStream = Optional.ofNullable(updateChildren(parent)).map(x -> x.parallelStream()).orElse(null);
        }

        if (resultStream == null) {
            MusicFolder folder = mediaFolderService.getMusicFolderById(parent.getFolderId());
            resultStream = mediaFileDao.getChildrenOf(parent.getPath(), parent.getFolderId(), true).parallelStream()
                    .map(x -> checkLastModified(x, folder, minimizeDiskAccess))
                    .filter(x -> includeMediaFile(x, folder));
        }

        resultStream = resultStream.filter(x -> (includeDirectories && x.isDirectory()) || (includeFiles && x.isFile()));

        if (sort) {
            resultStream = resultStream.sorted(new MediaFileComparator(settingsService.isSortAlbumsByYear()));
        }

        return resultStream.collect(Collectors.toList());
    }

    /**
     * Returns whether the given file is the root of a media folder.
     *
     * @see MusicFolder
     */
    public boolean isRoot(MediaFile mediaFile) {
        return StringUtils.isEmpty(mediaFile.getPath()) &&
                mediaFolderService.getAllMusicFolders(true, true).parallelStream()
                        .anyMatch(x -> mediaFile.getFolderId().equals(x.getId()));
    }

    /**
     * Returns all genres in the music collection.
     *
     * @param sortByAlbum Whether to sort by album count, rather than song count.
     * @return Sorted list of genres.
     */
    public List<Genre> getGenres(boolean sortByAlbum) {
        return mediaFileDao.getGenres(sortByAlbum);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostFrequentlyPlayedAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getMostRecentlyPlayedAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums in these folders.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count, List<MusicFolder> musicFolders) {
        return mediaFileDao.getNewestAlbums(offset, count, musicFolders);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param username     Returns albums starred by this user.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(int offset, int count, String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbums(offset, count, username, musicFolders);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums in these folders.
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabeticalAlbums(int offset, int count, boolean byArtist, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlphabeticalAlbums(offset, count, byArtist, musicFolders);
    }

    /**
     * Returns albums within a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param fromYear     The first year in the range.
     * @param toYear       The last year in the range.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the year range.
     */
    public List<MediaFile> getAlbumsByYear(int offset, int count, int fromYear, int toYear, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByYear(offset, count, fromYear, toYear, musicFolders);
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums in these folders.
     * @return Albums in the genre.
     */
    public List<MediaFile> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumsByGenre(offset, count, genre, musicFolders);
    }

    /**
     * Returns random songs for the given parent.
     *
     * @param parent The parent.
     * @param count  Max number of songs to return.
     * @return Random songs.
     */
    public List<MediaFile> getRandomSongsForParent(MediaFile parent, int count) {
        List<MediaFile> children = getDescendantsOf(parent, false);
        removeVideoFiles(children);

        if (children.isEmpty()) {
            return children;
        }
        Collections.shuffle(children);
        return children.subList(0, Math.min(count, children.size()));
    }

    /**
     * Returns random songs matching search criteria.
     *
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, String username) {
        return mediaFileDao.getRandomSongs(criteria, username);
    }

    /**
     * Removes video files from the given list.
     */
    public void removeVideoFiles(List<MediaFile> files) {
        files.removeIf(MediaFile::isVideo);
    }

    public Instant getMediaFileStarredDate(int id, String username) {
        return mediaFileDao.getMediaFileStarredDate(id, username);
    }

    public void populateStarredDate(List<MediaFile> mediaFiles, String username) {
        for (MediaFile mediaFile : mediaFiles) {
            populateStarredDate(mediaFile, username);
        }
    }

    public void populateStarredDate(MediaFile mediaFile, String username) {
        Instant starredDate = mediaFileDao.getMediaFileStarredDate(mediaFile.getId(), username);
        mediaFile.setStarredDate(starredDate);
    }

    private List<MediaFile> updateChildren(MediaFile parent) {

        // Check timestamps.
        if (parent.getChildrenLastUpdated().compareTo(parent.getChanged()) >= 0) {
            return null;
        }

        // do not get (non-existing) children for indexed tracks to avoid map key clashes
        Map<String, MediaFile> storedChildrenMap = mediaFileDao.getChildrenOf(parent.getPath(), parent.getFolderId(), false, true).parallelStream().collect(Collectors.toConcurrentMap(i -> i.getPath(), i -> i));
        MusicFolder folder = mediaFolderService.getMusicFolderById(parent.getFolderId());

        try (Stream<Path> children = Files.list(parent.getFullPath(folder.getPath()))) {
            List<MediaFile> result = children.parallel()
                    .filter(this::includeMediaFile)
                    .filter(x -> securityService.getMusicFolderForFile(x, true, true).getId().equals(parent.getFolderId()))
                    .map(x -> folder.getPath().relativize(x))
                    .map(x -> {
                        List<MediaFile> tracks = new ArrayList<>();
                        MediaFile media = storedChildrenMap.remove(x.toString());
                        if (media == null) {
                            media = createMediaFile(x, folder, null);
                            // Add children that are not already stored.
                            updateMediaFile(media);
                        } else {
                            media = checkLastModified(media, folder, false); // has to be false, only time it's called
                        }

                        tracks.add(media);

                        if (media.hasIndex()) {
                            List<MediaFile> cueTracks = createIndexedTracks(media, folder);
                            cueTracks.forEach(cueTrack -> {
                                updateMediaFile(cueTrack);
                                tracks.add(cueTrack);
                            });
                        }
                        return tracks;
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            // Delete children that no longer exist on disk.
            mediaFileDao.deleteMediaFiles(storedChildrenMap.keySet(), parent.getFolderId());

            // Update timestamp in parent.
            parent.setChildrenLastUpdated(parent.getChanged());
            parent.setPresent(true);
            updateMediaFile(parent);

            return result;
        } catch (IOException e) {
            LOG.warn("Could not retrieve and update all the children for {} in folder {}. Will skip", parent.getPath(), folder.getId());

            return null;
        }
    }

    /**
     * hide specific file types in player and API
     */
    public boolean showMediaFile(MediaFile media) {
        return !(settingsService.getHideIndexedFiles() && media.hasIndex());
    }

    public boolean includeMediaFile(MediaFile candidate, MusicFolder folder) {
        return includeMediaFile(candidate.getFullPath(folder.getPath()));
    }

    public boolean includeMediaFile(Path candidate) {
        String suffix = FilenameUtils.getExtension(candidate.toString()).toLowerCase();
        return (!isExcluded(candidate) && (Files.isDirectory(candidate) || isAudioFile(suffix) || isVideoFile(suffix)));
    }

    private boolean isAudioFile(String suffix) {
        return settingsService.getMusicFileTypesSet().contains(suffix.toLowerCase());
    }

    private boolean isVideoFile(String suffix) {
        return settingsService.getVideoFileTypesSet().contains(suffix.toLowerCase());
    }

    /**
     * Returns whether the given file is excluded.
     *
     * @param file The child file in question.
     * @return Whether the child file is excluded.
     */
    private boolean isExcluded(Path file) {
        if (settingsService.getIgnoreSymLinks() && Files.isSymbolicLink(file)) {
            LOG.info("excluding symbolic link {}", file);
            return true;
        }
        String name = file.getFileName().toString();
        if (settingsService.getExcludePattern() != null && settingsService.getExcludePattern().matcher(name).find()) {
            LOG.info("excluding file which matches exclude pattern {}: {}", settingsService.getExcludePatternString(), file.toString());
            return true;
        }

        // Exclude all hidden files starting with a single "." or "@eaDir" (thumbnail dir created on Synology devices).
        return (name.startsWith(".") && !name.startsWith("..")) || name.startsWith("@eaDir") || "Thumbs.db".equals(name);
    }

    private MediaFile createMediaFile(Path relativePath, MusicFolder folder, MediaFile existingFile) {
        Path file = folder.getPath().resolve(relativePath);
        if (!Files.exists(file)) {
            if (existingFile != null) {
                existingFile.setPresent(false);
                existingFile.setChildrenLastUpdated(Instant.ofEpochMilli(1));
            }
            return existingFile;
        }

        MediaFile mediaFile = new MediaFile();
        Instant lastModified = FileUtil.lastModified(file);
        mediaFile.setPath(relativePath.toString());
        mediaFile.setFolderId(folder.getId());
        //sanity check
        MusicFolder folderActual = securityService.getMusicFolderForFile(file, true, true);
        if (!folderActual.getId().equals(folder.getId())) {
            LOG.warn("Inconsistent Mediafile folder for media file with path: {}, folder id should be {} and is instead {}", file, folderActual.getId(), folder.getId());
        }
        // distinguish between null (no parent, like root folder), "" (root parent), and else
        String parentPath = null;
        if (StringUtils.isNotEmpty(relativePath.toString())) {
            parentPath = relativePath.getParent() == null ? "" : relativePath.getParent().toString();
        }
        mediaFile.setParentPath(parentPath);
        mediaFile.setChanged(lastModified);
        mediaFile.setLastScanned(Instant.now());
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1)); //distant past, can't use Instant.MIN due to HSQL incompatibility
        mediaFile.setCreated(lastModified);
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);
        mediaFile.setId(existingFile == null ? null : existingFile.getId());

        if (Files.isRegularFile(file)) {

            MetaDataParser parser = metaDataParserFactory.getParser(file);
            if (parser != null) {
                MetaData metaData = parser.getMetaData(file);
                mediaFile.setArtist(metaData.getArtist());
                mediaFile.setAlbumArtist(metaData.getAlbumArtist());
                mediaFile.setAlbumName(metaData.getAlbumName());
                mediaFile.setTitle(metaData.getTitle());
                mediaFile.setDiscNumber(metaData.getDiscNumber());
                mediaFile.setTrackNumber(metaData.getTrackNumber());
                mediaFile.setGenre(metaData.getGenre());
                mediaFile.setYear(metaData.getYear());
                mediaFile.setDuration(metaData.getDuration());
                mediaFile.setBitRate(metaData.getBitRate());
                mediaFile.setVariableBitRate(metaData.getVariableBitRate());
                mediaFile.setHeight(metaData.getHeight());
                mediaFile.setWidth(metaData.getWidth());
                mediaFile.setMusicBrainzReleaseId(metaData.getMusicBrainzReleaseId());
                mediaFile.setMusicBrainzRecordingId(metaData.getMusicBrainzRecordingId());
            }
            String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(mediaFile.getPath())));
            mediaFile.setFormat(format);
            mediaFile.setFileSize(FileUtil.size(file));
            getCuePath(relativePath, folder).ifPresent(cuePath -> {
                mediaFile.setIndexPath(folder.getPath().relativize(cuePath).toString());
            });
            mediaFile.setMediaType(getMediaType(mediaFile, folder));

        } else {

            // Is this an album?
            if (!isRoot(mediaFile)) {
                try (Stream<Path> stream = Files.list(file)) {
                    List<Path> children = stream.parallel().collect(Collectors.toList());
                    Path firstChild = children.parallelStream()
                            .filter(x -> includeMediaFile(x))
                            .filter(x -> Files.isRegularFile(x))
                            .findFirst().orElse(null);

                    if (firstChild != null) {
                        mediaFile.setMediaType(MediaFile.MediaType.ALBUM);

                        // Guess artist/album name, year and genre.
                        MetaDataParser parser = metaDataParserFactory.getParser(firstChild);
                        if (parser != null) {
                            MetaData metaData = parser.getMetaData(firstChild);
                            mediaFile.setArtist(metaData.getAlbumArtist());
                            mediaFile.setAlbumName(metaData.getAlbumName());
                            mediaFile.setYear(metaData.getYear());
                            mediaFile.setGenre(metaData.getGenre());
                        }

                        // Look for cover art.
                        Path coverArt = findCoverArt(children);
                        if (coverArt != null) {
                            mediaFile.setCoverArtPath(folder.getPath().relativize(coverArt).toString());
                        }
                    } else {
                        mediaFile.setArtist(file.getFileName().toString());
                    }

                } catch (IOException e) {
                    LOG.warn("Could not retrieve children for {}.", file.toString(), e);

                    mediaFile.setArtist(file.getFileName().toString());
                }
            } else {
                // root folders need to have a title
                mediaFile.setTitle(folder.getName());
            }
        }

        return mediaFile;
    }

    private List<MediaFile> createIndexedTracks(MediaFile media, MusicFolder folder) {
        try {
            List<MediaFile> children = new ArrayList<>();
            Path audioFile = media.getFullPath(folder.getPath());
            MetaDataParser parser = metaDataParserFactory.getParser(audioFile);
            MetaData metaData = null;
            if (parser != null) {
                metaData = parser.getMetaData(audioFile);
            }
            long wholeFileSize = Files.size(audioFile);
            double wholeFileLength = 0.0; //todo: find sound length without metadata
            if (metaData != null) {
                wholeFileLength = (metaData.getDuration() != null) ? metaData.getDuration() : 0.0;
            }

            CueSheet cueSheet = getCueSheet(media, folder);
            if (cueSheet != null) {
                for (int i = 0; i < cueSheet.getAllTrackData().size(); i++) {
                    TrackData trackData = cueSheet.getAllTrackData().get(i);
                    MediaFile mediaFile = new MediaFile();
                    mediaFile.setPath(media.getPath().toString());
                    mediaFile.setAlbumArtist(cueSheet.getPerformer());
                    mediaFile.setAlbumName(cueSheet.getTitle());
                    mediaFile.setTitle(trackData.getTitle());
                    mediaFile.setArtist(trackData.getPerformer());
                    mediaFile.setParentPath(media.getParentPath());
                    Instant lastModified = FileUtil.lastModified(audioFile);
                    mediaFile.setFolderId(media.getFolderId());
                    mediaFile.setChanged(lastModified);
                    mediaFile.setLastScanned(Instant.now());
                    mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1)); //distant past
                    mediaFile.setCreated(lastModified);
                    mediaFile.setPresent(true);
                    mediaFile.setTrackNumber(trackData.getNumber());

                    if (metaData != null) {
                        mediaFile.setDiscNumber(metaData.getDiscNumber());
                        mediaFile.setGenre(metaData.getGenre());
                        mediaFile.setYear(metaData.getYear());
                        mediaFile.setBitRate(metaData.getBitRate());
                        mediaFile.setVariableBitRate(metaData.getVariableBitRate());
                        mediaFile.setHeight(metaData.getHeight());
                        mediaFile.setWidth(metaData.getWidth());
                    }

                    String format = StringUtils.trimToNull(StringUtils.lowerCase(FilenameUtils.getExtension(audioFile.toString())));
                    mediaFile.setFormat(format);

                    Position currentPosition = trackData.getIndices().get(0).getPosition();
                    // convert CUE timestamp (minutes:seconds:frames, 75 frames/second) to fractional seconds
                    double currentStart = currentPosition.getMinutes() * 60 + currentPosition.getSeconds() + (currentPosition.getFrames() / 75);
                    mediaFile.setStartPosition(currentStart);

                    double nextStart = 0.0;
                    if (cueSheet.getAllTrackData().size() - 1 != i) {
                        Position nextPosition = cueSheet.getAllTrackData().get(i + 1).getIndices().get(0).getPosition();
                        nextStart = nextPosition.getMinutes() * 60 + nextPosition.getSeconds() + (nextPosition.getFrames() / 75);
                    } else {
                        nextStart = wholeFileLength;
                    }

                    mediaFile.setDuration(nextStart - currentStart);
                    mediaFile.setFileSize((long) (mediaFile.getDuration() / wholeFileLength * wholeFileSize)); //approximate
                    MediaFile existingFile = mediaFileDao.getMediaFile(mediaFile.getPath(), folder.getId(), currentStart);
                    mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
                    mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
                    mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
                    mediaFile.setMediaType(getMediaType(mediaFile, folder));

                    children.add(mediaFile);
                }
            }
            return children;
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private MediaFile.MediaType getMediaType(MediaFile mediaFile, MusicFolder folder) {
        if (folder.getType() == Type.PODCAST) {
            return MediaType.PODCAST;
        }
        if (isVideoFile(mediaFile.getFormat())) {
            return MediaFile.MediaType.VIDEO;
        }
        String path = mediaFile.getPath().toLowerCase();
        String genre = StringUtils.trimToEmpty(mediaFile.getGenre()).toLowerCase();
        if (path.contains("podcast") || genre.contains("podcast") || path.contains("netcast") || genre.contains("netcast")) {
            return MediaFile.MediaType.PODCAST;
        }
        if (path.contains("audiobook") || genre.contains("audiobook")
                || path.contains("audio book") || genre.contains("audio book")
                || path.contains("audio/book") || path.contains("audio\\book")) {
            return MediaFile.MediaType.AUDIOBOOK;
        }

        return MediaFile.MediaType.MUSIC;
    }

    public void refreshMediaFile(MediaFile mediaFile, MusicFolder folder) {
        mediaFile = createMediaFile(mediaFile.getRelativePath(), folder, mediaFile);
        updateMediaFile(mediaFile);
    }

    @CacheEvict(cacheNames = { "mediaFilePathCache", "mediaFileIdCache" }, allEntries = true)
    public void setMemoryCacheEnabled(boolean memoryCacheEnabled) {
        this.memoryCacheEnabled = memoryCacheEnabled;
    }

    public boolean getMemoryCacheEnabled() {
        return memoryCacheEnabled;
    }

    /**
     * Returns an Optional Path to a CUE file for the given media file
     * matches on file name minus extension
     * if no separate CUE file is found it looks for an embedded cuesheet (currently only supports FLAC) and
     * returns the resolvedPath for the mediaFile if one is found
     */
    private Optional<Path> getCuePath(Path path, MusicFolder folder) {
        Path resolvedPath = folder.getPath().resolve(path);
        Optional<Path> result = Optional.empty();

        try (Stream<Path> list = Files.list(resolvedPath.getParent())) {
            result = list
                .filter(p -> !Files.isDirectory(p))
                .filter(f -> "cue".equalsIgnoreCase(FilenameUtils.getExtension(f.toString()))
                             && FilenameUtils.getBaseName(f.toString()).equals(FilenameUtils.getBaseName(resolvedPath.toString())))
                .findFirst();

            // look for embedded cuesheet in FLAC
            if (result.isEmpty() && ("flac".equalsIgnoreCase(FilenameUtils.getExtension(resolvedPath.toString())) && (FLACReader.getCueSheet(resolvedPath) != null))) {
                result = Optional.of(resolvedPath);
            }
        } catch (IOException e) {
            LOG.warn("getCuePath {} {}", path, folder, e);
        }

        return result;
    }

    /**
     * Returns a parsed CueSheet for the given mediaFile
     */
    private CueSheet getCueSheet(MediaFile media, MusicFolder folder) {
        try {
            // is this an embedded cuesheet (currently only supports FLAC+CUE)?
            if (Objects.equals(media.getIndexPath(), media.getPath())) {
                return FLACReader.getCueSheet(media.getFullPath(folder.getPath()));
            } else {
                Charset cs = Charset.forName("UTF-8"); // default to UTF-8
                Path indexPath = media.getFullIndexPath(folder.getPath());

                // attempt to detect encoding for cueFile, fallback to UTF-8
                int THRESHOLD = 35; // 0-100, the higher the more certain the guess
                CharsetDetector cd = new CharsetDetector();
                try (FileInputStream fis = new FileInputStream(indexPath.toFile());
                     BufferedInputStream bis = new BufferedInputStream(fis);) {
                    cd.setText(bis);
                    CharsetMatch cm = cd.detect();
                    if (cm != null && cm.getConfidence() > THRESHOLD) {
                        cs = Charset.forName(cm.getName());
                    }
                } catch (IOException e) {
                    LOG.warn("Defaulting to UTF-8 for cuesheet {}", indexPath);
                }

                return CueParser.parse(indexPath, cs);
            }
        } catch (IOException e) {
            LOG.warn("Error getting cuesheet for {} {}", media, folder);
            return null;
        }
    }

    /**
     * Returns a cover art image for the given media file.
     */
    public Path getCoverArt(MediaFile mediaFile) {
        if (mediaFile.getRelativeCoverArtPath() != null) {
            return mediaFile.getRelativeCoverArtPath();
        }
        MediaFile parent = getParentOf(mediaFile);
        return parent == null ? null : parent.getRelativeCoverArtPath();
    }

    /**
     * Finds a cover art image for the given directory, by looking for it on the disk.
     */
    private Path findCoverArt(Collection<Path> candidates) {
        Path candidate = null;
        var coverArtSource = settingsService.getCoverArtSource();
        switch (coverArtSource) {
            case TAGFILE:
                candidate = findTagCover(candidates);
                if (candidate != null) {
                    return candidate;
                } else {
                    return findFileCover(candidates);
                }
            case FILE:
                return findFileCover(candidates);
            case TAG:
                return findTagCover(candidates);
            case FILETAG:
            default:
                candidate = findFileCover(candidates);
                if (candidate != null) {
                    return candidate;
                } else {
                    return findTagCover(candidates);
                }
        }
    }

    private Path findFileCover(Collection<Path> candidates) {
        for (String mask : settingsService.getCoverArtFileTypesSet()) {
            Path cand = candidates.parallelStream().filter(c -> {
                String candidate = c.getFileName().toString().toLowerCase();
                return candidate.endsWith(mask) && !candidate.startsWith(".") && Files.isRegularFile(c);
            }).findAny().orElse(null);

            if (cand != null) {
                return cand;
            }
        }
        return null;
    }

    private Path findTagCover(Collection<Path> candidates) {
        // Look for embedded images in audiofiles. (Only check first audio file encountered).
        return candidates.parallelStream()
                .filter(parser::isApplicable).findFirst()
                .filter(JaudiotaggerParser::isImageAvailable)
                .orElse(null);
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    /**
     * Returns all media files that are children, grand-children etc of a given media file.
     * Directories are not included in the result.
     *
     * @param sort Whether to sort files in the same directory.
     * @return All descendant music files.
     */
    public List<MediaFile> getDescendantsOf(MediaFile ancestor, boolean sort) {

        if (ancestor.isFile()) {
            return Arrays.asList(ancestor);
        }

        List<MediaFile> result = new ArrayList<MediaFile>();

        for (MediaFile child : getChildrenOf(ancestor, true, true, sort)) {
            if (child.isDirectory()) {
                result.addAll(getDescendantsOf(child, sort));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public void setMetaDataParserFactory(MetaDataParserFactory metaDataParserFactory) {
        this.metaDataParserFactory = metaDataParserFactory;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "mediaFilePathCache", key = "#mediaFile.path.concat('-').concat(#mediaFile.folderId)"),
        @CacheEvict(cacheNames = "mediaFileIdCache", key = "#mediaFile.id", condition = "#mediaFile.id != null") })
    public void updateMediaFile(MediaFile mediaFile) {
        mediaFileDao.createOrUpdateMediaFile(mediaFile, file -> {
            // Copy values from obsolete table music_file_info if inserting for first time
            MusicFolder folder = mediaFolderService.getMusicFolderById(mediaFile.getFolderId());
            if (folder != null) {
                MediaFile musicFileInfo = mediaFileDao.getMusicFileInfo(file.getFullPath(folder.getPath()).toString());
                if (musicFileInfo != null) {
                    file.setComment(musicFileInfo.getComment());
                    file.setLastPlayed(musicFileInfo.getLastPlayed());
                    file.setPlayCount(musicFileInfo.getPlayCount());
                }
            }
        });
    }

    /**
     * Increments the play count and last played date for the given media file and its
     * directory and album.
     */
    public void incrementPlayCount(MediaFile file) {
        Instant now = Instant.now();
        file.setLastPlayed(now);
        file.setPlayCount(file.getPlayCount() + 1);
        updateMediaFile(file);

        MediaFile parent = getParentOf(file);
        if (!isRoot(parent)) {
            parent.setLastPlayed(now);
            parent.setPlayCount(parent.getPlayCount() + 1);
            updateMediaFile(parent);
        }

        Album album = albumDao.getAlbum(file.getAlbumArtist(), file.getAlbumName());
        if (album != null) {
            album.setLastPlayed(now);
            album.incrementPlayCount();
            albumDao.createOrUpdateAlbum(album);
        }
    }

    public List<MediaFileEntry> toMediaFileEntryList(List<MediaFile> files, String username, boolean calculateStarred, boolean calculateFolderAccess,
            Function<MediaFile, String> streamUrlGenerator, Function<MediaFile, String> remoteStreamUrlGenerator,
            Function<MediaFile, String> remoteCoverArtUrlGenerator) {
        Locale locale = Optional.ofNullable(username).map(localeResolver::resolveLocale).orElse(null);
        List<MediaFileEntry> entries = new ArrayList<>(files.size());
        for (MediaFile file : files) {
            String streamUrl = Optional.ofNullable(streamUrlGenerator).map(g -> g.apply(file)).orElse(null);
            String remoteStreamUrl = Optional.ofNullable(remoteStreamUrlGenerator).map(g -> g.apply(file)).orElse(null);
            String remoteCoverArtUrl = Optional.ofNullable(remoteCoverArtUrlGenerator).map(g -> g.apply(file)).orElse(null);

            boolean starred = calculateStarred && username != null && getMediaFileStarredDate(file.getId(), username) != null;
            boolean folderAccess = !calculateFolderAccess || username == null || securityService.isFolderAccessAllowed(file, username);
            entries.add(MediaFileEntry.fromMediaFile(file, locale, starred, folderAccess, streamUrl, remoteStreamUrl, remoteCoverArtUrl));
        }

        return entries;
    }

    public int getAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getAlbumCount(musicFolders);
    }

    public int getPlayedAlbumCount(List<MusicFolder> musicFolders) {
        return mediaFileDao.getPlayedAlbumCount(musicFolders);
    }

    public int getStarredAlbumCount(String username, List<MusicFolder> musicFolders) {
        return mediaFileDao.getStarredAlbumCount(username, musicFolders);
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public void setParser(JaudiotaggerParser parser) {
        this.parser = parser;
    }
}
