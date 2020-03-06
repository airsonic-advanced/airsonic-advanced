package org.airsonic.test;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class StreamIT {

    @Test
    public void testStreamFlacAsMp3() throws Exception {
        Scanner.uploadToDefaultMusicFolder(
                Paths.get(this.getClass().getResource("/blobs/stream/dead/input").toURI()),
                "");
        System.out.println("Scan starting");
        Scanner.doScan();
        System.out.println("Scan complete");
        String mediaFileId = Scanner.getMediaFilesInMusicFolder().get(0).getId();
        assertNotNull(mediaFileId);

        byte[] fromServer = Scanner.getMediaFileData(mediaFileId);
        String expectedBodyResource = String.format("/blobs/stream/dead/responses/1.dat");
        byte[] expected = IOUtils.toByteArray(StreamIT.class.getResourceAsStream(expectedBodyResource));
        assertThat(fromServer).containsExactly(expected);
    }

    @Test
    public void testStreamM4aAsMp3() throws Exception {
        Scanner.uploadToDefaultMusicFolder(
                Paths.get(this.getClass().getResource("/blobs/stream/dance/input").toURI()),
                "");
        Scanner.doScan();
        String mediaFileId = Scanner.getMediaFilesInMusicFolder().get(0).getId();
        assertNotNull(mediaFileId);

        byte[] fromServer = Scanner.getMediaFileData(mediaFileId);
        String expectedBodyResource = String.format("/blobs/stream/dance/responses/1.dat");
        byte[] expected = IOUtils.toByteArray(StreamIT.class.getResourceAsStream(expectedBodyResource));
        assertThat(fromServer).containsExactly(expected);
    }
}
