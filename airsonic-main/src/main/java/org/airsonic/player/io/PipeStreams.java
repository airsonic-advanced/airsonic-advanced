package org.airsonic.player.io;

import com.google.common.util.concurrent.RateLimiter;

import org.airsonic.player.domain.TransferStatus;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * The Piped*Stream classes are taken and modified from the proposed alternative
 * implementation in https://bugs.openjdk.java.net/browse/JDK-4404700. The
 * existing JDK implementation is (a) slow (uses polling with a time of 1s), and
 * (b) "buggy" in the sense that it won't always retrieve the full requested
 * byte range block properly (due to writer not having written in).
 *
 */
public class PipeStreams {
    /**
     * This class is equivalent to <code>java.io.PipedInputStream</code>. In the
     * interface it only adds a constructor which allows for specifying the buffer
     * size. Its implementation, however, is much simpler and a lot more efficient
     * than its equivalent. It doesn't rely on polling. Instead it uses proper
     * synchronization with its counterpart
     * <code>org.airsonic.player.util.PipedOutputStream</code>.
     *
     * Multiple readers can read from this stream concurrently. The block asked for
     * by a reader is delivered completely, or until the end of the stream if less
     * is available. Other readers can't come in between.
     *
     * @author WD
     */

    public static class PipedInputStream extends InputStream {

        byte[] buffer;
        boolean closed = false;
        int readLaps = 0;
        int readPosition = 0;
        public PipedOutputStream source;
        int writeLaps = 0;
        int writePosition = 0;

        /**
         * Creates an unconnected PipedInputStream with a default buffer size.
         */

        public PipedInputStream() throws IOException {
            this(null);
        }

        /**
         * Creates a PipedInputStream with a default buffer size and connects it to
         * <code>source</code>.
         *
         * @exception IOException It was already connected.
         */

        public PipedInputStream(PipedOutputStream source) throws IOException {
            this(source, 0x10000);
        }

        /**
         * Creates a PipedInputStream with buffer size <code>bufferSize</code> and
         * connects it to <code>source</code>.
         *
         * @exception IOException It was already connected.
         */

        public PipedInputStream(PipedOutputStream source, int bufferSize) throws IOException {
            if (source != null) {
                connect(source);
            }

            buffer = new byte[bufferSize];
        }

        @Override
        public int available() throws IOException {
            /*
             * The circular buffer is inspected to see where the reader and the writer are
             * located.
             */

            return writePosition > readPosition /* The writer is in the same lap. */ ? writePosition - readPosition
                    : (writePosition < readPosition
                            /* The writer is in the next lap. */ ? buffer.length - readPosition + 1 + writePosition
                            :
                            /* The writer is at the same position or a complete lap ahead. */
                            (writeLaps > readLaps ? buffer.length : 0));
        }

        /**
         * @exception IOException The pipe is not connected.
         */

        @Override
        public void close() throws IOException {
            if (source == null) {
                throw new IOException("Unconnected pipe");
            }

            synchronized (buffer) {
                closed = true;
                // Release any pending writers.
                buffer.notifyAll();
            }
        }

        /**
         * @exception IOException The pipe is already connected.
         */

        public void connect(PipedOutputStream source) throws IOException {
            if (this.source != null) {
                throw new IOException("Pipe already connected");
            }

            this.source = source;
            source.sink = this;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int result = read(b);

            return result == -1 ? -1 : b[0];
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /**
         * @exception IOException The pipe is not connected.
         */

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (source == null) {
                throw new IOException("Unconnected pipe");
            }

            synchronized (buffer) {
                if (writePosition == readPosition && writeLaps == readLaps) {
                    if (closed) {
                        return -1;
                    }

                    // Wait for any writer to put something in the circular buffer.

                    try {
                        buffer.notifyAll();
                        buffer.wait();
                    } catch (InterruptedException e) {
                        throw new IOException(e.getMessage());
                    }

                    // Try again.

                    return read(b, off, len);
                }

                // Don't read more than the capacity indicated by len or what's available
                // in the circular buffer.

                int amount = Math.min(len,
                        (writePosition > readPosition ? writePosition : buffer.length) - readPosition);

                System.arraycopy(buffer, readPosition, b, off, amount);
                readPosition += amount;

                if (readPosition == buffer.length) {
                    // A lap was completed, so go back.
                    readPosition = 0;
                    ++readLaps;
                }

                // The buffer is only released when the complete desired block was
                // obtained.

                if (amount < len) {
                    int second = read(b, off + amount, len - amount);

                    return second == -1 ? amount : amount + second;
                } else {
                    buffer.notifyAll();
                }

                return amount;
            }
        }

    } // PipedInputStream

    /**
     * This class is equivalent to <code>java.io.PipedOutputStream</code>. In the
     * interface it only adds a constructor which allows for specifying the buffer
     * size. Its implementation, however, is much simpler and a lot more efficient
     * than its equivalent. It doesn't rely on polling. Instead it uses proper
     * synchronization with its counterpart
     * <code>org.airsonic.player.util.PipeStreams.PipedInputStream</code>.
     *
     * Multiple writers can write in this stream concurrently. The block written by
     * a writer is put in completely. Other writers can't come in between.
     *
     * @author WD
     */

    public static class PipedOutputStream extends OutputStream {

        PipedInputStream sink;

        /**
         * Creates an unconnected PipedOutputStream.
         */

        public PipedOutputStream() throws IOException {
            this(null);
        }

        /**
         * Creates a PipedOutputStream and connects it to <code>sink</code>.
         *
         * @exception IOException It was already connected.
         */

        public PipedOutputStream(PipedInputStream sink) throws IOException {
            if (sink != null) {
                connect(sink);
            }
        }

        /**
         * @exception IOException The pipe is not connected.
         */

        @Override
        public void close() throws IOException {
            if (sink == null) {
                throw new IOException("Unconnected pipe");
            }

            synchronized (sink.buffer) {
                sink.closed = true;
                flush();
            }
        }

        /**
         * @exception IOException The pipe is already connected.
         */

        public void connect(PipedInputStream sink) throws IOException {
            if (this.sink != null) {
                throw new IOException("Pipe already connected");
            }

            this.sink = sink;
            sink.source = this;
        }

        @Override
        public void flush() throws IOException {
            synchronized (sink.buffer) {
                // Release all readers.
                sink.buffer.notifyAll();
            }
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[] { (byte) b });
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        /**
         * @exception IOException The pipe is not connected or a reader has closed it.
         */

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (sink == null) {
                throw new IOException("Unconnected pipe");
            }

            if (sink.closed) {
                throw new IOException("Broken pipe");
            }

            synchronized (sink.buffer) {
                if (sink.writePosition == sink.readPosition && sink.writeLaps > sink.readLaps) {
                    // The circular buffer is full, so wait for some reader to consume
                    // something.

                    try {
                        sink.buffer.notifyAll();
                        sink.buffer.wait();
                    } catch (InterruptedException e) {
                        throw new IOException(e.getMessage());
                    }

                    // Try again.

                    write(b, off, len);

                    return;
                }

                // Don't write more than the capacity indicated by len or the space
                // available in the circular buffer.

                int amount = Math.min(len,
                        (sink.writePosition < sink.readPosition ? sink.readPosition : sink.buffer.length)
                                - sink.writePosition);

                System.arraycopy(b, off, sink.buffer, sink.writePosition, amount);
                sink.writePosition += amount;

                if (sink.writePosition == sink.buffer.length) {
                    sink.writePosition = 0;
                    ++sink.writeLaps;
                }

                // The buffer is only released when the complete desired block was
                // written.

                if (amount < len) {
                    write(b, off + amount, len - amount);
                } else {
                    sink.buffer.notifyAll();
                }
            }
        }

    } // PipedOutputStream

    public static class MonitoredInputStream extends FilterInputStream {
        private final RateLimiter rateLimiter;
        private final TransferStatus status;
        private final Consumer<TransferStatus> statusCloser;

        public MonitoredInputStream(InputStream delegate, RateLimiter rateLimiter,
                Supplier<TransferStatus> statusSupplier, Consumer<TransferStatus> statusCloser,
                BiConsumer<InputStream, TransferStatus> initAction) {
            super(delegate);
            this.rateLimiter = rateLimiter;
            this.status = statusSupplier.get();
            this.statusCloser = statusCloser;
            initAction.accept(delegate, status);
        }

        private void acquire(int len) {
            if (rateLimiter != null) {
                rateLimiter.acquire(len);
            }
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read >= 0) {
                acquire(1);
                status.addBytesTransferred(1);
            }

            return read;
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) {
                acquire(read);
                status.addBytesTransferred(read);
            }

            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipped = super.skip(n);
            status.addBytesSkipped(skipped);
            return skipped;
        }

        @Override
        public void close() throws IOException {
            super.close();
            statusCloser.accept(status);
        }
    }

    public static class MonitoredResource implements Resource {
        private final Resource delegate;
        private final RateLimiter rateLimiter;
        private final Supplier<TransferStatus> statusSupplier;
        private final Consumer<TransferStatus> statusCloser;
        private final BiConsumer<InputStream, TransferStatus> inputStreamInit;

        public MonitoredResource(Resource delegate, RateLimiter rateLimiter, Supplier<TransferStatus> statusSupplier,
                Consumer<TransferStatus> statusCloser, BiConsumer<InputStream, TransferStatus> inputStreamInit) {
            this.delegate = delegate;
            this.rateLimiter = rateLimiter;
            this.statusSupplier = statusSupplier;
            this.statusCloser = statusCloser;
            this.inputStreamInit = inputStreamInit;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new MonitoredInputStream(delegate.getInputStream(), rateLimiter, statusSupplier, statusCloser, inputStreamInit);
        }

        @Override
        public boolean exists() {
            return delegate.exists();
        }

        @Override
        public URL getURL() throws IOException {
            return delegate.getURL();
        }

        @Override
        public URI getURI() throws IOException {
            return delegate.getURI();
        }

        @Override
        public File getFile() throws IOException {
            return delegate.getFile();
        }

        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }

        @Override
        public long lastModified() throws IOException {
            return delegate.lastModified();
        }

        @Override
        public Resource createRelative(String relativePath) throws IOException {
            return delegate.createRelative(relativePath);
        }

        @Override
        public String getFilename() {
            return delegate.getFilename();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

    }
}
