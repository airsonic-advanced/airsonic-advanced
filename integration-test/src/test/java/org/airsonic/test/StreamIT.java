package org.airsonic.test;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.subsonic.restapi.Child;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class StreamIT {

    @Test
    public void testStreamFlacAsMp3() throws Exception {
        testFileStreaming("dead");
    }

    @Test
    public void testStreamM4aAsMp3() throws Exception {
        testFileStreaming("dance");
    }

    @Test
    public void testStreaMp3() throws Exception {
        testFileStreaming("piano");
    }

    private void testFileStreaming(String file) throws Exception {
        Scanner.uploadToDefaultMusicFolder(
                Paths.get(this.getClass().getResource("/blobs/stream/" + file + "/input").toURI()),
                "");
        Scanner.doScan();
        List<Child> files = Scanner.getMediaFilesInMusicFolder();
        System.out.println("For file " + file + ": " + Scanner.MAPPER.writeValueAsString(files));
        String mediaFileId = files.get(0).getId();
        assertNotNull(mediaFileId);

        byte[] fromServer = Scanner.getMediaFileData(mediaFileId);
        String expectedBodyResource = String.format("/blobs/stream/" + file + "/responses/1.dat");
        byte[] expected = IOUtils.toByteArray(StreamIT.class.getResourceAsStream(expectedBodyResource));
        assertThat(fromServer).containsExactly(expected);
    }
}
