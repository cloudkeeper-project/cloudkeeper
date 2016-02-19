package xyz.cloudkeeper.s3.io;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Buffered input stream for reading from S3.
 *
 * <p>This class implements an input stream that satisfies the requirements of
 * {@link S3Connection#newBufferedInputStream(String, String, long)}. Data transfer from S3 occurs in
 * parts of size {@link S3Connection#getBufferSize()} (using Amazon S3 get-object requests that include
 * a range), and the transfer of each part is wrapped in a lightweight task that will be executed by
 * {@link S3Connection#getExecutorService()}. Up to
 * {@link S3Connection#getParallelConnectionsPerRequest()} part downloads may be active at the same time
 * (per stream). Accordingly, the total size of all buffers used for the returned stream may be up to the product of
 * {@link S3Connection#getParallelConnectionsPerRequest()} and
 * {@link S3Connection#getBufferSize()}. However, the total size off all buffers will never exceed the
 * S3 object size by more {@link #INITIAL_BUFFER_SIZE}. Hence, this input stream is also memory-efficient for many small
 * S3 objects.
 *
 * <p>An input stream of this class will read from an S3 object. It is lenient with not being closed in time. In
 * particular, instances of this class do not keep system resources for longer than absolutely necessary. In contrast,
 * an {@link com.amazonaws.services.s3.model.S3ObjectInputStream} instance returned by
 * {@link com.amazonaws.services.s3.AmazonS3#getObject(com.amazonaws.services.s3.model.GetObjectRequest)} contains "a
 * direct stream of data from the HTTP connection", and "the underlying HTTP connection cannot be closed until the user
 * finishes reading the data and closes the stream".
 *
 * @author Florian Schoppmann
 */
final class S3BufferedInputStream extends InputStream {
    private static final int INITIAL_BUFFER_SIZE = 1024;

    private final S3Connection s3Connection;
    private final String bucketName;
    private final String key;

    private final int numBuffers;
    private final int bufferSize;

    private long size = Long.MAX_VALUE;
    private long absolutePos;
    private boolean closed;

    private final Deque<CompletableFuture<Long>> futures;
    private final Deque<byte[]> buffers;

    @Nullable private byte[] currentBuffer;
    private int posInBuffer = 0;
    private long currentReadAheadPos;

    /**
     * Constructor.
     *
     * @param s3Connection high-level connection to Amazon S3
     * @param bucketName name of the bucket that contains the object
     * @param key key of the object to create and write to
     * @param offset offset (in bytes) where to start reading from
     */
    S3BufferedInputStream(S3Connection s3Connection, String bucketName, String key, long offset) {
        Objects.requireNonNull(s3Connection);
        Objects.requireNonNull(bucketName);
        Objects.requireNonNull(key);

        this.s3Connection = s3Connection;
        this.bucketName = bucketName;
        this.key = key;
        numBuffers = this.s3Connection.getParallelConnectionsPerRequest();
        bufferSize = s3Connection.getBufferSize();

        absolutePos = offset;
        currentReadAheadPos = offset;
        futures = new ArrayDeque<>(numBuffers);
        buffers = new ArrayDeque<>(numBuffers);
    }

    private void startAsynchronousDownload(byte[] buffer) {
        futures.add(s3Connection.readBytesWithinRange(bucketName, key, currentReadAheadPos, buffer));
        buffers.add(buffer);
        currentReadAheadPos += buffer.length;
    }

    private IOException mapException(Exception exception) {
        Throwable cause = exception instanceof ExecutionException
            ? exception.getCause()
            : exception;

        if (cause instanceof IOException) {
            return (IOException) cause;
        } else {
            return new IOException(String.format("Reading s3://%s/%s failed.", bucketName, key), cause);
        }
    }

    /**
     * Waits for a buffer to become available for reading.
     *
     * <p>This method must only be called from a read method, that is, from the thread reading from this stream. Fields
     * that are only accessed from this thread do not need synchronization.
     */
    private void waitForBuffer() throws IOException {
        if (closed) {
            throw new IOException("Read attempt after stream was closed.");
        }

        // Note that this method is a no-op if absolutePos == size.
        try {
            if (size == Long.MAX_VALUE) {
                startAsynchronousDownload(new byte[INITIAL_BUFFER_SIZE]);
                size = futures.poll().get();
                currentBuffer = buffers.poll();
                posInBuffer = 0;
                while (currentReadAheadPos < size && buffers.size() < numBuffers) {
                    int newBufferSize = (int) Math.min(bufferSize, size - currentReadAheadPos);
                    startAsynchronousDownload(new byte[newBufferSize]);
                }
            } else {
                if (posInBuffer == currentBuffer.length && absolutePos < size) {
                    if (currentReadAheadPos < size) {
                        startAsynchronousDownload(currentBuffer);
                    }
                    long newSize = futures.poll().get();
                    if (size != newSize) {
                        throw new IOException(String.format(
                            "Length of %s/%s changed from %d to %d during transmission.", bucketName, key, size, newSize
                        ));
                    }
                    currentBuffer = buffers.poll();
                    posInBuffer = 0;
                }
            }
        } catch (ExecutionException | InterruptedException exception) {
            throw mapException(exception);
        }

        assert currentBuffer != null;
    }

    @Override
    public int read() throws IOException {
        waitForBuffer();
        assert currentBuffer != null && size < Long.MAX_VALUE;

        int returnValue;
        if (absolutePos < size) {
            returnValue = currentBuffer[posInBuffer];
            ++absolutePos;
            ++posInBuffer;
        } else {
            assert absolutePos <= size;
            returnValue = -1;
        }
        return returnValue;
    }

    @Override
    public int read(byte[] target, int offset, int length) throws IOException {
        Objects.requireNonNull(target);
        if (offset < 0 || length < 0 || length > target.length - offset) {
            throw new IndexOutOfBoundsException(String.format(
                "length of array = %d, offset = %d, length = %d", target.length, offset, length
            ));
        } else if (length == 0) {
            return 0;
        }

        waitForBuffer();
        assert currentBuffer != null && size < Long.MAX_VALUE;

        // Cannot read beyond the end. Also note that the (int) cast is safe because we are taking the minimum of
        // non-negative numbers.
        int remaining = (int) Math.min(size - absolutePos, length);
        int bytesRead = 0;
        while (true) {
            int blockSize = Math.min(remaining, currentBuffer.length - posInBuffer);
            System.arraycopy(currentBuffer, posInBuffer, target, offset + bytesRead, blockSize);
            remaining -= blockSize;
            bytesRead += blockSize;
            posInBuffer += blockSize;
            absolutePos += blockSize;

            if (remaining == 0) {
                break;
            }
            waitForBuffer();
            assert currentBuffer != null;
        }
        return bytesRead > 0
            ? bytesRead
            : -1;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }
}
