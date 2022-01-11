
package org.airsonic.player.service.search;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.service.SearchService;
import org.airsonic.player.util.MusicFolderTestData;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.util.ObjectUtils.isEmpty;

/*
 * Test cases related to #1139.
 * Confirming whether shuffle search can be performed correctly in MusicFolder containing special strings.
 *
 * (Since the query of getRandomAlbums consists of folder paths only,
 * this verification is easy to perform.)
 *
 * This test case is a FalsePattern for search,
 * but there may be problems with the data flow prior to creating the search index.
 */
public class SearchServiceSpecialPathTestCase extends AbstractAirsonicHomeTest {

    private List<MusicFolder> musicFolders;

    @Autowired
    private SearchService searchService;

    @Override
    public List<MusicFolder> getMusicFolders() {
        if (isEmpty(musicFolders)) {
            musicFolders = new ArrayList<>();

            Path musicDir = MusicFolderTestData.resolveBaseMediaPath().resolve("Search").resolve("SpecialPath").resolve("accessible");
            musicFolders.add(new MusicFolder(1, musicDir, "accessible", true, Instant.now()));

            Path music2Dir = MusicFolderTestData.resolveBaseMediaPath().resolve("Search").resolve("SpecialPath").resolve("accessible's");
            musicFolders.add(new MusicFolder(2, music2Dir, "accessible's", true, Instant.now()));

            Path music3Dir = MusicFolderTestData.resolveBaseMediaPath().resolve("Search").resolve("SpecialPath").resolve("accessible+s");
            musicFolders.add(new MusicFolder(3, music3Dir, "accessible+s", true, Instant.now()));
        }
        return musicFolders;
    }

    private static UUID cleanupId = null;

    @Before
    public void setup() {
        UUID id = populateDatabaseOnlyOnce();
        if (id != null) {
            cleanupId = id;
        }
    }

    @AfterClass
    public static void cleanup() {
        AbstractAirsonicHomeTest.cleanup(cleanupId);
        cleanupId = null;
    }

    @Test
    public void testSpecialCharactersInDirName() {

        List<MusicFolder> folders = getMusicFolders();

        // ALL Songs
        List<MediaFile> randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folders);
        Assert.assertEquals("ALL Albums ", 3, randomAlbums.size());

        // dir - accessible
        List<MusicFolder> folder01 = folders.stream()
                .filter(m -> "accessible".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder01);
        Assert.assertEquals("Albums in \"accessible\" ", 1, randomAlbums.size());

        // dir - accessible's
        List<MusicFolder> folder02 = folders.stream()
                .filter(m -> "accessible's".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder02);
        Assert.assertEquals("Albums in \"accessible's\" ", 1, randomAlbums.size());

        // dir - accessible+s
        List<MusicFolder> folder03 = folders.stream()
                .filter(m -> "accessible+s".equals(m.getName()))
                .collect(Collectors.toList());
        randomAlbums = searchService.getRandomAlbums(Integer.MAX_VALUE, folder03);
        Assert.assertEquals("Albums in \"accessible+s\" ", 1, folder03.size());

    }

}
