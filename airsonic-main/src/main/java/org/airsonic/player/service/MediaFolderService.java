package org.airsonic.player.service;

import org.airsonic.player.dao.MusicFolderDao;
import org.airsonic.player.domain.MusicFolder;
import org.apache.commons.lang3.tuple.Triple;
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Service
public class MediaFolderService {
    @Autowired
    private MusicFolderDao musicFolderDao;

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
        Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps = getMusicFolderPathOverlaps(musicFolder, getAllMusicFolders(true, true));

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
        // if new folder has descendants, ignore. they'll stay under descendant hierarchy

        clearMusicFolderCache();
    }

    public void deleteMusicFolder(Integer id) {
        MusicFolder folder = getMusicFolderById(id);
        Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps = getMusicFolderPathOverlaps(folder, getAllMusicFolders(true, true));

        // if folder has ancestors, reassign hierarchy to immediate ancestor
        if (!overlaps.getMiddle().isEmpty()) {
            musicFolderDao.reassignChildren(folder, overlaps.getMiddle().get(0));
            clearMediaFileCache();
        }
        // if folder has descendants, ignore. they'll stay under descendant hierarchy

        musicFolderDao.deleteMusicFolder(id);
        clearMusicFolderCache();
    }

    public void updateMusicFolder(MusicFolder musicFolder) {
        Triple<List<MusicFolder>, List<MusicFolder>, List<MusicFolder>> overlaps = getMusicFolderPathOverlaps(musicFolder, getAllMusicFolders(true, true));
        if (!overlaps.getLeft().isEmpty() || !overlaps.getMiddle().isEmpty() || !overlaps.getRight().isEmpty()) {
            throw new IllegalArgumentException("Music folder with path " + musicFolder.getPath() + " overlaps with existing music folder path(s) (" + logMusicFolderOverlap(overlaps) + ") and can therefore not be updated.");
        }
        musicFolderDao.updateMusicFolder(musicFolder);
        clearMusicFolderCache();
    }

    /**
     * @return List of overlaps
     * <ul>
     * <li>List 1: Exact path overlaps (in no order)
     * <li>List 2: Ancestors of the given folder (closest ancestor first: /a/b/c -> [/a/b, /a])
     * <li>List 3: Descendants of the given folder (closest descendant first: /a/b/c -> [/a/b/c/d, /a/b/c/d/e])
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
