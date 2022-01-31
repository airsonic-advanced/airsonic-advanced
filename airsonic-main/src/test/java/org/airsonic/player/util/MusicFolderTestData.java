package org.airsonic.player.util;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.MusicFolder.Type;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MusicFolderTestData {

    private static String baseResources = "/MEDIAS/";

    public static Path resolveBaseMediaPath() {
        try {
            return Paths.get(MusicFolderTestData.class.getResource(baseResources).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not find base resources for tests", e);
        }
    }

    public static Path resolveMusicFolderPath() {
        return resolveBaseMediaPath().resolve("Music");
    }

    public static Path resolveMusic2FolderPath() {
        return resolveBaseMediaPath().resolve("Music2");
    }

    public static Path resolveMusic3FolderPath() {
        return resolveBaseMediaPath().resolve("Music3");
    }

    public static List<MusicFolder> getTestMusicFolders() {
        List<MusicFolder> liste = new ArrayList<>();
        Path musicDir = resolveMusicFolderPath();
        MusicFolder musicFolder = new MusicFolder(1, musicDir, "Music", Type.MEDIA, true, Instant.now());
        liste.add(musicFolder);

        Path music2Dir = resolveMusic2FolderPath();
        MusicFolder musicFolder2 = new MusicFolder(2, music2Dir, "Music2", Type.MEDIA, true, Instant.now());
        liste.add(musicFolder2);
        return liste;
    }
}
