package org.airsonic.player.api;

import org.airsonic.player.TestCaseUtils;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.MediaFolderService;
import org.airsonic.player.service.MediaScannerService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScanningTestUtils {
    private static Map<UUID, List<MusicFolder>> map = new ConcurrentHashMap<>();

    public static UUID before(List<MusicFolder> musicFolders, MediaFolderService mediaFolderService, MediaScannerService mediaScannerService) {
        UUID id = UUID.randomUUID();
        map.put(id, musicFolders);
        musicFolders.forEach(mediaFolderService::createMusicFolder);

        TestCaseUtils.execScan(mediaScannerService);

        return id;
    }

    public static void after(UUID id, MediaFolderService mediaFolderService) {
        Optional.ofNullable(map.remove(id)).orElse(Collections.emptyList()).forEach(f -> {
            // maybe consider deleting media_files too
            mediaFolderService.deleteMusicFolder(f.getId());
        });
    }
}
