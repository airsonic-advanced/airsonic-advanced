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

import com.google.common.io.MoreFiles;
import org.airsonic.player.ajax.MediaFileEntry;
import org.airsonic.player.dao.AlbumDao;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.domain.*;
import org.airsonic.player.domain.CoverArt.EntityType;
import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder.Type;
import org.airsonic.player.i18n.LocaleResolver;
import org.airsonic.player.service.metadata.JaudiotaggerParser;
import org.airsonic.player.service.metadata.MetaData;
import org.airsonic.player.service.metadata.MetaDataParser;
import org.airsonic.player.service.metadata.MetaDataParserFactory;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toList;

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
    private CoverArtService coverArtService;
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

    public MediaFile getMediaFile(String relativePath, Integer folderId) {
        return getMediaFile(relativePath, mediaFolderService.getMusicFolderById(folderId));
    }

    public MediaFile getMediaFile(String relativePath, MusicFolder folder) {
        return getMediaFile(Paths.get(relativePath), folder);
    }

    public MediaFile getMediaFile(Path relativePath, MusicFolder folder) {
        return getMediaFile(relativePath, folder, settingsService.isFastCacheEnabled());
    }

    public MediaFile getMediaFile(String relativePath, Integer folderId, boolean minimizeDiskAccess) {
        return getMediaFile(Paths.get(relativePath), mediaFolderService.getMusicFolderById(folderId), minimizeDiskAccess);
    }

    /**
     * Does not retrieve indexed tracks!
     */
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

        // Not found in database, must read and persist from disk.
        return createAndUpdateMediaFile(relativePath, folder, null, true, true);
    }

    @Cacheable(cacheNames = "mediaFileIdCache", condition = "#root.target.memoryCacheEnabled", unless = "#result == null")
    public MediaFile getMediaFile(int id) {
        MediaFile mediaFile = mediaFileDao.getMediaFile(id);
        if (mediaFile == null) {
            return null;
        }

        return checkLastModified(mediaFile, mediaFolderService.getMusicFolderById(mediaFile.getFolderId()), settingsService.isFastCacheEnabled());
    }

    public List<MediaFile> getMediaFilesByRelativePath(Path relativePath) {
        return mediaFileDao.getMediaFilesByRelativePath(relativePath.toString());
    }

    public MediaFile getParentOf(MediaFile mediaFile) {
        return getParentOf(mediaFile, settingsService.isFastCacheEnabled());
    }

    public MediaFile getParentOf(MediaFile mediaFile, boolean minimizeDiskAccess) {
        if (mediaFile.getParentPath() == null) {
            return null;
        }
        return getMediaFile(mediaFile.getParentPath(), mediaFile.getFolderId(), minimizeDiskAccess);
    }

    private MediaFile checkLastModified(MediaFile mediaFile, MusicFolder folder, boolean minimizeDiskAccess) {
        if (mediaFile.isIndexedTrack()) {
            // check parent
            MediaFile parent = mediaFileDao.getMediaFile(mediaFile.getPath(), mediaFile.getFolderId());
            MediaFile checkedParent = checkLastModified(parent, folder, minimizeDiskAccess);
            if (parent != checkedParent) { //has to be equality operator
                // stuff's changed in the parent or index
                return mediaFileDao.getMediaFile(mediaFile.getId());
            } else {
                // nothing's changed, return file as is
                return mediaFile;
            }
        }
        if (minimizeDiskAccess || (mediaFile.getVersion() >= MediaFileDao.VERSION
                && !settingsService.getFullScan()
                && !mediaFile.getChanged().isBefore(FileUtil.lastModified(mediaFile.getFullPath(folder.getPath())))
                && (!mediaFile.hasIndex() || !mediaFile.getChanged().isBefore(FileUtil.lastModified(mediaFile.getFullIndexPath(folder.getPath())))))) {
            LOG.debug("Detected unmodified file (id {}, path {} in folder {} ({}))", mediaFile.getId(), mediaFile.getPath(), folder.getId(), folder.getName());
            return mediaFile;
        }
        LOG.debug("Updating database file from disk (id {}, path {} in folder {} ({}))", mediaFile.getId(), mediaFile.getPath(), folder.getId(), folder.getName());
        mediaFile = createAndUpdateMediaFile(mediaFile.getRelativePath(), folder, mediaFile, false, true);
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
        return getChildrenOf(parent, true, includeDirectories, sort).stream()
                .filter(this::showMediaFile)
                .collect(toList());
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
            Stream<MediaFile> nonIndexed = mediaFileDao.getChildrenOf(parent.getPath(), parent.getFolderId(), true, false).parallelStream()
                    .map(x -> checkLastModified(x, folder, minimizeDiskAccess));

            // invoke after nonIndexed go through checkLastModified so that any changes in indexed are reflected in the DB prior to retrieval
            Stream<MediaFile> indexed = StreamSupport.stream(
                () -> mediaFileDao.getChildrenOf(parent.getPath(), parent.getFolderId(), true, true).spliterator(),
                Spliterator.SIZED, true);

            resultStream = Stream.concat(nonIndexed, indexed).filter(x -> includeMediaFile(x, folder));
        }

        resultStream = resultStream.filter(x -> (includeDirectories && x.isDirectory()) || (includeFiles && x.isFile()));

        if (sort) {
            resultStream = resultStream.sorted(new MediaFileComparator(settingsService.isSortAlbumsByYear()));
        }

        return resultStream.collect(toList());
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
        if (!parent.getChildrenLastUpdated().isBefore(parent.getChanged())) {
            return null;
        }

        // only get nonindexed files (indexed files are marked in their own method)
        Map<Pair<String, BigDecimal>, MediaFile> storedChildrenMap = mediaFileDao
                .getChildrenOf(parent.getPath(), parent.getFolderId(), false, false).parallelStream()
                .collect(toConcurrentMap(i -> Pair.of(i.getPath(), i.getStartPosition()), i -> i));
        MusicFolder folder = mediaFolderService.getMusicFolderById(parent.getFolderId());

        try (Stream<Path> children = Files.list(parent.getFullPath(folder.getPath()))) {
            List<Path> indexFiles = new ArrayList<>();
            List<MediaFile> nonIndexedResult = children.parallel()
                    .filter(x -> includeMediaFile(x) || isPossibleIndexFile(x))
                    .filter(x -> securityService.getMusicFolderForFile(x, true, true).getId().equals(parent.getFolderId()))
                    .map(x -> {
                        MediaFile media = null;
                        if (isPossibleIndexFile(x)) {
                            indexFiles.add(x);
                        }
                        if (includeMediaFile(x)) {
                            Path relativePath = folder.getPath().relativize(x);

                            // deal with non indexed files
                            media = storedChildrenMap.remove(Pair.of(relativePath.toString(), MediaFile.NOT_INDEXED));
                            if (media == null) {
                                // don't look for index files or create indexed tracks yet
                                media = createAndUpdateMediaFile(relativePath, folder, null, false, false);
                            } else {
                                media = checkLastModified(media, folder, false); // has to be false, only time it's called
                            }
                        }

                        return media;
                    })
                    .filter(Objects::nonNull)
                    .collect(toList());

            // deal with indexed files
            List<MediaFile> indexedResult = updateIndexFiles(indexFiles, folder);

            // Delete children that no longer exist on disk.
            mediaFileDao.deleteMediaFilesWithIndexedTracks(storedChildrenMap.keySet(), parent.getFolderId());

            // Update timestamp in parent.
            parent.setChildrenLastUpdated(parent.getChanged());
            parent.setPresent(true);
            updateMediaFile(parent);

            return Stream.of(nonIndexedResult, indexedResult).flatMap(List::stream).collect(toList());
        } catch (IOException e) {
            LOG.warn("Could not retrieve and update all the children for {} in folder {}. Will skip", parent.getPath(), folder.getId(), e);

            return null;
        }
    }

    public List<MediaFile> updateIndexFiles(List<Path> indexFiles, MusicFolder folder) {
        return indexFiles.stream().map(indexPath -> {
            Path relativeIndexPath = folder.getPath().relativize(indexPath);

            // get corresponding mediafile from db
            String baseName = FilenameUtils.getBaseName(indexPath.toString());
            Path relativePartialPath = relativeIndexPath.resolveSibling(baseName);
            List<MediaFile> indexParents = mediaFileDao.getPartialMatchesByPath(relativePartialPath.toString() + ".", folder.getId()).stream()
                    .filter(x -> StringUtils.equals(baseName, FilenameUtils.getBaseName(x.getPath())))
                    .filter(x -> !x.isIndexedTrack())
                    .collect(toList());

            if (indexParents.size() != 1) {
                LOG.warn("Found {} file(s) corresponding to index file: {}", indexParents.size(), indexPath);
            }

            return indexParents.stream().map(indexParent -> {
                var added = false;
                // add index path to parent file
                if (StringUtils.isBlank(indexParent.getIndexPath())) {
                    indexParent.setIndexPath(relativeIndexPath.toString());
                    Instant modified = FileUtil.lastModified(indexParent.getFullIndexPath(folder.getPath()));
                    modified = modified.isAfter(indexParent.getChanged()) ? modified : indexParent.getChanged();
                    indexParent.setChanged(modified);
                    updateMediaFile(indexParent);
                    added = true;
                }
                return Pair.of(indexParent, added);
            }).map(e -> {
                if (e.getValue()) {
                    return createIndexedTracks(e.getKey(), folder);
                } else {
                    return mediaFileDao.getMediaFiles(e.getKey().getPath(), e.getKey().getFolderId(), null).stream()
                            .filter(MediaFile::isIndexedTrack).collect(toList());
                }
            });
        }).flatMap(i -> i).flatMap(List::stream).collect(toList());
    }

    /**
     * show specific file types in player and API
     *
     * <pre>
     * |                | hideIndexed=true | hideIndexed=false |
     * |----------------|------------------|-------------------|
     * | reg nonindexed |     true         |      true         |
     * | index parent   |     false        |      true         |
     * | indexed        |     true         |      true         |
     * </pre>
     */
    public boolean showMediaFile(MediaFile media) {
        return !media.hasIndex() || media.isIndexedTrack() || !settingsService.getHideIndexedFiles();
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

    private boolean isPossibleIndexFile(Path candidate) {
        String suffix = FilenameUtils.getExtension(candidate.toString()).toLowerCase();
        return settingsService.getIndexFileTypesSet().contains(suffix) && Files.isRegularFile(candidate);
    }

    private String getIndexFile(MediaFile candidate, MusicFolder folder) {
        Path filePath = candidate.getFullPath(folder.getPath());
        String fileName = FilenameUtils.getBaseName(candidate.getPath());
        return settingsService.getIndexFileTypesSet().stream()
                .map(suffix -> filePath.resolveSibling(fileName + "." + suffix))
                .filter(Files::exists)
                .findFirst()
                .map(i -> folder.getPath().relativize(i))
                .map(Path::toString)
                .orElse(null);
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
        Instant now = Instant.now();
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
        mediaFile.setLastScanned(now);
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1)); //distant past, can't use Instant.MIN due to HSQL incompatibility
        mediaFile.setCreated(existingFile == null ? now : existingFile.getCreated());
        mediaFile.setMediaType(MediaFile.MediaType.DIRECTORY);
        mediaFile.setPresent(true);
        mediaFile.setIndexPath(existingFile == null ? null : existingFile.getIndexPath());
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
            mediaFile.setMediaType(getMediaType(mediaFile, folder));
        } else {

            // Is this an album?
            if (!isRoot(mediaFile)) {
                try (Stream<Path> stream = Files.list(file)) {
                    List<Path> children = stream.parallel().collect(Collectors.toList());
                    Path firstChild = children.parallelStream()
                            .filter(this::includeMediaFile)
                            .filter(Files::isRegularFile)
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
                            // placeholder to be persisted later
                            mediaFile.setArt(new CoverArt(-1, EntityType.MEDIA_FILE, folder.getPath().relativize(coverArt).toString(), folder.getId(), false));
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

    private MediaFile createIndexedTrack(CueSheet cueSheet, TrackData trackData, MediaFile indexedParent, MusicFolder folder, MediaFile existingFile, BigDecimal startPosition, BigDecimal nextStart) {
        long wholeFileSize = indexedParent.getFileSize() != null ? indexedParent.getFileSize() : 0;
        double wholeFileDuration = indexedParent.getDuration() != null ? indexedParent.getDuration() : 0.0;
        MediaFile mediaFile = new MediaFile();
        mediaFile.setPath(indexedParent.getPath());
        mediaFile.setAlbumArtist(cueSheet.getPerformer());
        mediaFile.setAlbumName(cueSheet.getTitle());
        mediaFile.setTitle(trackData.getTitle());
        mediaFile.setArtist(trackData.getPerformer());
        mediaFile.setParentPath(indexedParent.getParentPath());
        mediaFile.setFolderId(indexedParent.getFolderId());
        mediaFile.setChanged(indexedParent.getChanged());
        mediaFile.setLastScanned(Instant.now());
        mediaFile.setChildrenLastUpdated(Instant.ofEpochMilli(1)); // distant past
        mediaFile.setCreated(indexedParent.getCreated());
        mediaFile.setPresent(true);
        mediaFile.setTrackNumber(trackData.getNumber());
        mediaFile.setDiscNumber(indexedParent.getDiscNumber());
        mediaFile.setGenre(indexedParent.getGenre());
        mediaFile.setYear(indexedParent.getYear());
        mediaFile.setBitRate(indexedParent.getBitRate());
        mediaFile.setVariableBitRate(indexedParent.isVariableBitRate());
        mediaFile.setHeight(indexedParent.getHeight());
        mediaFile.setWidth(indexedParent.getWidth());
        mediaFile.setFormat(indexedParent.getFormat());
        mediaFile.setMediaType(indexedParent.getMediaType());
        mediaFile.setMusicBrainzReleaseId(indexedParent.getMusicBrainzReleaseId());
        mediaFile.setMusicBrainzRecordingId(indexedParent.getMusicBrainzRecordingId());
        mediaFile.setStartPosition(startPosition);
        mediaFile.setDuration(nextStart.subtract(startPosition).doubleValue());
        mediaFile.setFileSize((long) (mediaFile.getDuration() / wholeFileDuration * wholeFileSize)); // approximate
        mediaFile.setPlayCount(existingFile == null ? 0 : existingFile.getPlayCount());
        mediaFile.setLastPlayed(existingFile == null ? null : existingFile.getLastPlayed());
        mediaFile.setComment(existingFile == null ? null : existingFile.getComment());
        mediaFile.setIndexPath(existingFile == null ? indexedParent.getIndexPath() : existingFile.getIndexPath());
        mediaFile.setId(existingFile == null ? null : existingFile.getId());

        return mediaFile;
    }

    private BigDecimal getCuePosition(TrackData trackData) {
        Position currentPosition = trackData.getIndices().get(0).getPosition();
        // convert CUE timestamp (minutes:seconds:frames, 75 frames/second) to fractional seconds
        double val = currentPosition.getMinutes() * 60 + currentPosition.getSeconds() + (currentPosition.getFrames() / 75.0);
        // round to 9 decimal places for nano accuracy
        return BigDecimal.valueOf(val).setScale(9);
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
        createAndUpdateMediaFile(mediaFile.getRelativePath(), folder, mediaFile, true, true);
    }

    private MediaFile createAndUpdateMediaFile(Path relativePath, MusicFolder folder, MediaFile existingFile, boolean searchForIndex, boolean createIndexed) {
        MediaFile mediaFile = createMediaFile(relativePath, folder, existingFile);
        if (searchForIndex && mediaFile.isFile() && !mediaFile.hasIndex()) {
            mediaFile.setIndexPath(getIndexFile(mediaFile, folder));
        }

        if (mediaFile.hasIndex()) {
            Instant modified = FileUtil.lastModified(mediaFile.getFullIndexPath(folder.getPath()));
            modified = modified.isAfter(mediaFile.getChanged()) ? modified : mediaFile.getChanged();
            mediaFile.setChanged(modified);
        }

        updateMediaFile(mediaFile);

        if (createIndexed && mediaFile.hasIndex()) {
            createIndexedTracks(mediaFile, folder);
        }

        return mediaFile;
    }

    private List<MediaFile> createIndexedTracks(MediaFile parentIndexedFile, MusicFolder folder) {
        var storedChildrenMap = mediaFileDao
                .getMediaFiles(parentIndexedFile.getPath(), parentIndexedFile.getFolderId(), null).stream()
                .filter(MediaFile::isIndexedTrack)
                .collect(toConcurrentMap(i -> Pair.of(i.getPath(), i.getStartPosition()), i -> i));

        CueSheet cue = getCueSheet(parentIndexedFile.getFullIndexPath(folder.getPath()));
        List<MediaFile> res = emptyList();
        if (cue == null) {
            parentIndexedFile.setIndexPath(null);
            parentIndexedFile.setChanged(FileUtil.lastModified(parentIndexedFile.getFullPath(folder.getPath())));
            updateMediaFile(parentIndexedFile);
        } else {
            res = updateIndexedTracks(cue, parentIndexedFile, folder, storedChildrenMap);
        }
        mediaFileDao.deleteMediaFilesWithIndexedTracks(storedChildrenMap.keySet(), parentIndexedFile.getFolderId());

        return res;
    }

    private List<MediaFile> updateIndexedTracks(CueSheet cueSheet, MediaFile indexParent, MusicFolder folder, Map<Pair<String, BigDecimal>, MediaFile> storedChildrenMap) {
        List<MediaFile> children = new ArrayList<>();

        try {
            double wholeFileDuration = indexParent.getDuration() != null ? indexParent.getDuration() : 0.0;

            for (int i = 0; i < cueSheet.getAllTrackData().size(); i++) {
                TrackData trackData = cueSheet.getAllTrackData().get(i);
                BigDecimal startPosition = getCuePosition(trackData);
                BigDecimal nextStart = BigDecimal.valueOf(wholeFileDuration);
                if (i != cueSheet.getAllTrackData().size() - 1) {
                    nextStart = getCuePosition(cueSheet.getAllTrackData().get(i + 1));
                }
                MediaFile media = storedChildrenMap.remove(Pair.of(indexParent.getPath(), startPosition));
                media = createIndexedTrack(cueSheet, trackData, indexParent, folder, media, startPosition, nextStart);
                updateMediaFile(media);

                children.add(media);
            }
        } catch (Exception e) {
            LOG.warn("Error creating indexed files", e);
        }

        return children;
    }

    @CacheEvict(cacheNames = { "mediaFilePathCache", "mediaFileIdCache" }, allEntries = true)
    public void setMemoryCacheEnabled(boolean memoryCacheEnabled) {
        this.memoryCacheEnabled = memoryCacheEnabled;
    }

    public boolean getMemoryCacheEnabled() {
        return memoryCacheEnabled;
    }

    /**
     * Returns a parsed CueSheet for the given index
     */
    private CueSheet getCueSheet(Path fullIndexPath) {
        try {
            // is this an embedded cuesheet (currently only supports FLAC+CUE)?
            if (StringUtils.equalsIgnoreCase("flac", MoreFiles.getFileExtension(fullIndexPath))) {
                return FLACReader.getCueSheet(fullIndexPath);
            } else {
                return CueParser.parse(fullIndexPath, Util.detectCharset(fullIndexPath));
            }
        } catch (IOException e) {
            LOG.warn("Error getting cuesheet for {}", fullIndexPath);
            return null;
        }
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
        // Look for embedded images in audiofiles.
        return candidates.stream()
                .filter(parser::isApplicable)
                .filter(JaudiotaggerParser::isImageAvailable)
                .findFirst()
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

        // persist cover art if not overridden
        coverArtService.persistIfNeeded(mediaFile);
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
