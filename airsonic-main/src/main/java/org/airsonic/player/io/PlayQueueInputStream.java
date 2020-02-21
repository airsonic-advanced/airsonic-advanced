package org.airsonic.player.io;

import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.PlayQueue;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

public class PlayQueueInputStream extends InputStream {
    private final PlayQueue queue;
    private final Consumer<MediaFile> fileStartListener;
    private final Consumer<MediaFile> fileEndListener;
    private final Function<MediaFile, InputStream> streamGenerator;
    private InputStream currentStream;
    private MediaFile currentFile;

    public PlayQueueInputStream(PlayQueue queue, Consumer<MediaFile> fileStartListener,
            Consumer<MediaFile> fileEndListener, Function<MediaFile, InputStream> streamGenerator) {
        this.queue = queue;
        this.fileStartListener = fileStartListener;
        this.fileEndListener = fileEndListener;
        this.streamGenerator = streamGenerator;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int n = read(b);
        return n == -1 ? -1 : b[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        prepare();
        if (currentStream == null || queue.getStatus() == PlayQueue.Status.STOPPED) {
            return -1;
        }

        int n = currentStream.read(b, off, len);

        // If end of song reached, skip to next song and call read() again.
        if (n == -1) {
            queue.next();
            closeStream();
            return read(b, off, len);
        }

        return n;
    }

    private void prepare() throws IOException {
//        PlayQueue playQueue = player.getPlayQueue();
//
//        // If playlist is in auto-random mode, populate it with new random songs.
//        if (playQueue.getIndex() == -1 && playQueue.getRandomSearchCriteria() != null) {
//            populateRandomPlaylist(playQueue);
//        }

        MediaFile file = queue.getCurrentFile();
        if (file == null) {
            closeStream();
        } else if (!file.equals(currentFile)) {
            closeStream();
            currentFile = file;
            fileStartListener.accept(currentFile);
            currentStream = streamGenerator.apply(currentFile);
        }
    }

    public void closeStream() throws IOException {
        if (currentStream != null) {
            currentStream.close();
            currentStream = null;
        }
        if (currentFile != null) {
            fileEndListener.accept(currentFile);
            currentFile = null;
        }
    }

    @Override
    public void close() throws IOException {
        closeStream();
        super.close();
    }
}
