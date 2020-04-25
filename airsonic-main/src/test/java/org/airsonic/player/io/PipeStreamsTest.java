package org.airsonic.player.io;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.RateLimiter;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.io.PipeStreams.MonitoredInputStream;
import org.airsonic.player.io.PipeStreams.MonitoredResource;
import org.airsonic.player.io.PipeStreams.PipedInputStream;
import org.airsonic.player.io.PipeStreams.PipedOutputStream;
import org.airsonic.player.util.FileUtil;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PipeStreamsTest {

    // this test will sporadically fail with the built-in JDK Piped*Stream classes,
    // thus we use our own
    @Test
    public void testPipeStreams() throws IOException {
        byte[] b = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
        TransferStatus status = new TransferStatus(null);

        try (PipedInputStream pin = new PipedInputStream(null, 5);
                MonitoredInputStream min = new MonitoredInputStream(pin, null, () -> status, s -> {}, (i, s) -> {});
                PipedOutputStream pout = new PipedOutputStream(pin);) {

            // can initialize it in the MonitoredInputStream constructor too, but chose not
            // to for simplicity
            new Thread(() -> {
                try (PipedOutputStream po = pout) {
                    po.write(b, 4, 12);
                } catch (IOException e) {
                    fail("Should not throw IOException");
                }
            }).start();

            byte[] b2 = new byte[2];

            assertThat(min.read(b2)).isEqualTo(2);
            assertThat(b2).containsExactly(4, 5);
            assertThat(status.getBytesTransferred()).isEqualTo(2);
            assertThat(status.getBytesSkipped()).isEqualTo(0);

            assertThat(min.skip(4)).isEqualTo(4);
            assertThat(b2).containsExactly(4, 5);
            assertThat(status.getBytesTransferred()).isEqualTo(2);
            assertThat(status.getBytesSkipped()).isEqualTo(4);

            assertThat(min.read(b2)).isEqualTo(2);
            assertThat(b2).containsExactly(10, 11);
            assertThat(status.getBytesTransferred()).isEqualTo(4);
            assertThat(status.getBytesSkipped()).isEqualTo(4);

            assertThat(min.read()).isEqualTo(12);
            assertThat(b2).containsExactly(10, 11);
            assertThat(status.getBytesTransferred()).isEqualTo(5);
            assertThat(status.getBytesSkipped()).isEqualTo(4);

            byte[] b5 = new byte[5];

            assertThat(min.read(b5)).isEqualTo(3);
            assertThat(b5).containsExactly(13, 14, 15, 0, 0);
            assertThat(status.getBytesTransferred()).isEqualTo(8);
            assertThat(status.getBytesSkipped()).isEqualTo(4);

            assertThat(min.read(b5)).isEqualTo(-1);
            assertThat(b5).containsExactly(13, 14, 15, 0, 0);
            assertThat(status.getBytesTransferred()).isEqualTo(8);
            assertThat(status.getBytesSkipped()).isEqualTo(4);

            assertThat(min.skip(4)).isEqualTo(0);
            assertThat(status.getBytesTransferred()).isEqualTo(8);
            assertThat(status.getBytesSkipped()).isEqualTo(4);

            assertThat(min.read()).isEqualTo(-1);
            assertThat(status.getBytesTransferred()).isEqualTo(8);
            assertThat(status.getBytesSkipped()).isEqualTo(4);
        }
    }

    @Test
    public void testMonitoredResource() throws Exception {
        TransferStatus status = new TransferStatus(null);
        RateLimiter limit = RateLimiter.create(4.0);
        Path file = Paths.get(Resources.getResource("MEDIAS/piano.mp3").toURI());
        Set<String> eventSet = new HashSet<>();
        Resource r = new MonitoredResource(new FileSystemResource(file), limit, () -> status, s -> {
            if (!eventSet.add("statusClosed")) {
                fail("statusClosed multiple times");
            }
        }, (i, s) -> {
            if (!eventSet.add("inputStreamOpened")) {
                fail("inputStream opened multiple times");
            }
        });

        assertThat(r.contentLength()).isEqualTo(FileUtil.size(file));
        assertThat(eventSet).isEmpty();

        // these are timed tests and a degree of variability is to be expected
        try (InputStream is = r.getInputStream()) {
            assertThat(eventSet).containsOnly("inputStreamOpened");

            long start = System.currentTimeMillis();
            is.skip(2);
            is.skip(2);
            is.skip(2);
            is.skip(2);
            is.skip(2);
            is.skip(2);
            // skips shouldn't be rate-limited
            assertThat(System.currentTimeMillis() - start).isLessThan(1000);
            assertThat(status.getBytesTransferred()).isEqualTo(0);
            assertThat(status.getBytesSkipped()).isEqualTo(12);
            assertThat(eventSet).containsOnly("inputStreamOpened");

            byte[] b = new byte[2];
            start = System.currentTimeMillis();
            is.read(b);
            is.read(b);
            is.read(b);
            is.read(b);
            is.read(b);
            is.read(b);
            is.read();
            is.read();
            // reads should be rate-limited
            assertThat(System.currentTimeMillis() - start).isGreaterThan(2000);
            assertThat(status.getBytesTransferred()).isEqualTo(14);
            assertThat(status.getBytesSkipped()).isEqualTo(12);
            assertThat(eventSet).containsOnly("inputStreamOpened");
        }

        assertThat(eventSet).containsOnly("inputStreamOpened", "statusClosed");
    }
}
