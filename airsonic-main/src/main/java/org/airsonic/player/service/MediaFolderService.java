package org.airsonic.player.service;

import com.google.common.collect.Streams;
import org.airsonic.player.dao.MediaFileDao;
import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Service
public class MediaFolderService {
    private static final Logger LOG = LoggerFactory.getLogger(MediaFolderService.class);

    @Autowired
    private MusicFolderDao musicFolderDao;
    @Autowired
    private MediaFileDao mediaFileDao;

    private List<MusicFolder> cachedMusicFolders;
    private final ConcurrentMap<String, List<MusicFolder>> cachedMusicFoldersPerUser = new ConcurrentHashMap<>();

    /**
     * Returns all music folders. Non-existing and disabled folders are not included.
     *
     * @return Possibly empty list of all music folders.
     */
    public List<MusicFolder> getAllMusicFolders() {
        return getAllMusicFolders(false, false);
    }

    /**
     * Returns all music folders.
     *
     * @param includeDisabled    Whether to include disabled folders.
     * @param includeNonExisting Whether to include non-existing folders.
     * @return Possibly empty list of all music folders.
     */
    public List<MusicFolder> getAllMusicFolders(boolean includeDisabled, boolean includeNonExisting) {
        if (cachedMusicFolders == null) {
            cachedMusicFolders = musicFolderDao.getAllMusicFolders();
        }

        return cachedMusicFolders.parallelStream()
                .filter(folder -> (includeDisabled || folder.isEnabled()) && (includeNonExisting || Files.exists(folder.getPath())))
                .collect(Collectors.toList());
    }

    public List<MusicFolder> getAllMusicFolders(boolean includeDisabled, boolean includeNonExisting, boolean includeDeleted) {
        return Streams.concat(
                getAllMusicFolders(includeDisabled, includeNonExisting).stream(),
                includeDeleted ? getDeletedMusicFolders().stream() : Stream.empty())
            .collect(toList());
    }

    /**
     * Returns all music folders a user have access to. Non-existing and disabled folders are not included.
     *
     * @return Possibly empty list of music folders.
     */
    public List<MusicFolder> getMusicFoldersForUser(String username) {
        return cachedMusicFoldersPerUser.computeIfAbsent(username, u -> {
            List<MusicFolder> result = musicFolderDao.getMusicFoldersForUser(u);
            result.retainAll(getAllMusicFolders(false, false));
            return result;
        });
    }

    /**
     * Returns all music folders a user have access to. Non-existing and disabled folders are not included.
     *
     * @param selectedMusicFolderId If non-null and positive and included in the list of allowed music folders, this methods returns a list of only this music folder.
     * @return Possibly empty list of music folders.
     */
    public List<MusicFolder> getMusicFoldersForUser(String username, Integer selectedMusicFolderId) {
        return getMusicFoldersForUser(username).stream()
                .filter(f -> selectedMusicFolderId == null || selectedMusicFolderId < 0 || f.getId().equals(selectedMusicFolderId))
                .collect(toList());
    }

    public void setMusicFoldersForUser(String username, Collection<Integer> musicFolderIds) {
        musicFolderDao.setMusicFoldersForUser(username, musicFolderIds);
        cachedMusicFoldersPerUser.remove(username);
    }

    public MusicFolder getMusicFolderById(Integer id) {
        return getMusicFolderById(id, false, false);
    }

    public MusicFolder getMusicFolderById(Integer id, boolean includeDisabled, boolean includeNonExisting) {
        return getAllMusicFolders(includeDisabled, includeNonExisting).parallelStream().filter(folder -> id.equals(folder.getId())).findAny().orElse(null);
    }

    public void createMusicFolder(MusicFolder musicFolder) {
        Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps = getMusicFolderPathOverlaps(musicFolder, getAllMusicFolders(true, true, true));

        // deny same path music folders
        if (!overlaps.getLeft().isEmpty()) {
            throw new IllegalArgumentException("Music folder with path " + musicFolder.getPath() + " overlaps with existing music folder path(s) (" + logMusicFolderOverlap(overlaps) + ") and can therefore not be created.");
        }

        musicFolderDao.createMusicFolder(musicFolder);

        // if new folder has ancestors, reassign portion of closest ancestor's tree to new folder
        if (!overlaps.getMiddle().isEmpty()) {
            MusicFolder ancestor = overlaps.getMiddle().get(0);
            musicFolderDao.reassignChildren(ancestor, musicFolder);
            clearMediaFileCache();
        }

        if (!overlaps.getRight().isEmpty()) {
            // if new folder has deleted descendants, integrate and true delete the descendants
            overlaps.getRight().stream()
                // deleted
                .filter(f -> f.getId() < 0)
                .forEach(f -> {
                    musicFolderDao.reassignChildren(f, musicFolder);
                    musicFolderDao.deleteMusicFolder(f.getId());
                    clearMediaFileCache();
                });
            // other descendants are ignored, they'll stay under descendant hierarchy
        }

        clearMusicFolderCache();
    }

    public void deleteMusicFolder(Integer id) {
        // if empty folder, just delete
        if (mediaFileDao.getMediaFileCount(id) == 0) {
            musicFolderDao.deleteMusicFolder(id);
            clearMusicFolderCache();
            return;
        }

        MusicFolder folder = getMusicFolderById(id, true, true);
        Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps = getMusicFolderPathOverlaps(folder, getAllMusicFolders(true, true, true));

        // if folder has ancestors, reassign hierarchy to immediate ancestor and true delete
        if (!overlaps.getMiddle().isEmpty()) {
            musicFolderDao.reassignChildren(folder, overlaps.getMiddle().get(0));
            musicFolderDao.deleteMusicFolder(id);
        }
        // if folder has descendants, ignore. they'll stay under descendant hierarchy

        musicFolderDao.updateMusicFolderId(id, -id - 1);
        folder.setId(-id - 1);
        folder.setEnabled(false);
        musicFolderDao.updateMusicFolder(folder);
        mediaFileDao.deleteMediaFiles(-id - 1);
        clearMusicFolderCache();
        clearMediaFileCache();
    }

    public boolean enablePodcastFolder(int id) {
        MusicFolder podcastFolder = getMusicFolderById(id, true, true);
        if (podcastFolder != null && podcastFolder.getType() == Type.PODCAST) {
            try {
                getAllMusicFolders(true, true).stream()
                        .filter(f -> f.getType() == Type.PODCAST)
                        .filter(f -> !f.getId().equals(podcastFolder.getId()))
                        .forEach(f -> {
                            f.setEnabled(false);
                            updateMusicFolder(f);
                        });
                podcastFolder.setEnabled(true);
                updateMusicFolder(podcastFolder);
                return true;
            } catch (Exception e) {
                LOG.warn("Could not enable podcast music folder id {} ({})", podcastFolder.getId(), podcastFolder.getName(), e);
                return false;
            }
        }

        return false;
    }

    public void expunge() {
        musicFolderDao.expungeMusicFolders();
    }

    public void updateMusicFolder(MusicFolder musicFolder) {
        Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps = getMusicFolderPathOverlaps(musicFolder, getAllMusicFolders(true, true, true).stream().filter(f -> !f.getId().equals(musicFolder.getId())).collect(toList()));
        MusicFolder existing = getAllMusicFolders(true, true).stream().filter(f -> f.getId().equals(musicFolder.getId())).findAny().orElse(null);
        if (existing != null && !existing.getPath().equals(musicFolder.getPath()) && (!overlaps.getLeft().isEmpty() || !overlaps.getMiddle().isEmpty() || !overlaps.getRight().isEmpty())) {
            throw new IllegalArgumentException("Music folder with path " + musicFolder.getPath() + " overlaps with existing music folder path(s) (" + logMusicFolderOverlap(overlaps) + ") and can therefore not be updated.");
        }
        musicFolderDao.updateMusicFolder(musicFolder);
        clearMusicFolderCache();
    }

    public List<MusicFolder> getDeletedMusicFolders() {
        return musicFolderDao.getDeletedMusicFolders();
    }

    /**
     * @return List of overlaps
     * <ul>
     * <li>List 1: Exact path overlaps (in no order)
     * <li>List 2: Ancestors of the given folder (closest ancestor first: /a/b/c -> [/a/b, /a])
     * <li>List 3: Descendants of the given folder (closest descendant first: /a/b/c -> [/a/b/c/d, /a/b/c/e, /a/b/c/d/f])
     * </ul>
     */
    public static Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> getMusicFolderPathOverlaps(MusicFolder folder, List<MusicFolder> allFolders) {
        Path absoluteFolderPath = folder.getPath().normalize().toAbsolutePath();
        List<MusicFolder> sameFolders = allFolders.parallelStream().filter(f -> {
            // is same but not itself
            Path fAbsolute = f.getPath().normalize().toAbsolutePath();
            return fAbsolute.equals(absoluteFolderPath) && !f.getId().equals(folder.getId());
        }).collect(toList());
        List<MusicFolder> ancestorFolders = allFolders.parallelStream().filter(f -> {
            // is ancestor
            Path fAbsolute = f.getPath().normalize().toAbsolutePath();
            return absoluteFolderPath.getNameCount() > fAbsolute.getNameCount()
                    && absoluteFolderPath.startsWith(fAbsolute);
        }).sorted(Comparator.comparing(f -> f.getPath().getNameCount(), Comparator.reverseOrder())).collect(toList());
        List<MusicFolder> descendantFolders = allFolders.parallelStream().filter(f -> {
            // is descendant
            Path fAbsolute = f.getPath().normalize().toAbsolutePath();
            return fAbsolute.getNameCount() > absoluteFolderPath.getNameCount()
                    && fAbsolute.startsWith(absoluteFolderPath);
        }).sorted(Comparator.comparing(f -> f.getPath().getNameCount())).collect(toList());

        return Triple.of(sameFolders, ancestorFolders, descendantFolders);
    }

    public static String logMusicFolderOverlap(Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps) {
        StringBuilder result = new StringBuilder("SAME: ");
        result.append(overlaps.getLeft().stream().map(f -> f.getName()).collect(joining(",", "[", "]")));
        result.append(", ANCESTOR: ");
        result.append(overlaps.getMiddle().stream().map(f -> f.getName()).collect(joining(",", "[", "]")));
        result.append(", DESCENDANT: ");
        result.append(overlaps.getRight().stream().map(f -> f.getName()).collect(joining(",", "[", "]")));

        return result.toString();
    }

    public void clearMusicFolderCache() {
        cachedMusicFolders = null;
        cachedMusicFoldersPerUser.clear();
    }

    @CacheEvict(cacheNames = { "mediaFilePathCache", "mediaFileIdCache" }, allEntries = true)
    public void clearMediaFileCache() {
        // TODO: optimize cache eviction
    }

}
