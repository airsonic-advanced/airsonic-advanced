package org.airsonic.player.util;

import org.airsonic.player.domain.MusicFolder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MusicFolderTestData {

    private static String baseResources = "/MEDIAS/";

    public static String resolveBaseMediaPath() {
        return MusicFolderTestData.class.getResource(baseResources).toString().replace("file:","");
    }

    public static String resolveMusicFolderPath() {
        return (MusicFolderTestData.resolveBaseMediaPath() + "Music");
    }

    public static String resolveMusic2FolderPath() {
        return (MusicFolderTestData.resolveBaseMediaPath() + "Music2");
    }

    public static String resolveMusic3FolderPath() {
        return (MusicFolderTestData.resolveBaseMediaPath() + "Music3");
    }

    public static List<MusicFolder> getTestMusicFolders() {
        List<MusicFolder> liste = new ArrayList<>();
        Path musicDir = Paths.get(MusicFolderTestData.resolveMusicFolderPath());
        MusicFolder musicFolder = new MusicFolder(1,musicDir,"Music",true,Instant.now());
        liste.add(musicFolder);

        Path music2Dir = Paths.get(MusicFolderTestData.resolveMusic2FolderPath());
        MusicFolder musicFolder2 = new MusicFolder(2,music2Dir,"Music2",true,Instant.now());
        liste.add(musicFolder2);
        return liste;
    }
}
