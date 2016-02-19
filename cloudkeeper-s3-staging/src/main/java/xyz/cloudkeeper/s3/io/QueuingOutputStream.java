package xyz.cloudkeeper.s3.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Output stream for queuing bytes and later retrieving them through an input stream.
 *
 * <p>This class provides an efficient alternative to naive buffering: writing raw data to a {@code byte[]} buffer only
 * to later read from this byte array through an {@link InputStream}. The problem with this naive approach is that the
 * amount of data that needs to be buffered may vary in size. Hence, choosing a correct buffer size is problematic:
 * Too small may hurt down-stream efficiency (for instance, if flushing the buffer triggers network activity), too large
 * a buffer may waste lots of memory and therefore limit the number of streams that may be active at a single time.
 *
 * <p>This class implements an output stream that allocates buffers of incremental size. The size of the first buffer is
 * the smallest power of 2 larger than or equal to the minimum size {@code minSize} passed to the constructor
 * {@link #QueuingOutputStream(int, int)}. A new buffer is created whenever the previous buffer is full and more data is
 * written. The size of a new buffer is twice the size of the previous buffer. Hence, assuming that the total number of
 * bytes written to an input stream is at least the size of the first buffer, then at most 3 times as much memory is
 * allocated than absolutely necessary (that is, there is a 3-approximation guarantee). At the same time, there will be
 * no more than about log(<em>n</em>) memory allocation requests if <em>n</em> is the total number of bytes written.
 * Thus, this class provides a reasonable trade-off between raw speed and memory efficiency.
 *
 * <p>The bytes written to an instance of this output-stream class are available by calling {@link #toInputStream()}.
 * This method returns a new input stream for reading all bytes previously written to the output stream. The new input
 * stream does <em>not</em>, however, provide any bytes written to the output stream after creation of the input stream.
 * Calling {@link #toInputStream()} multiple times is allowed, and each call will create a new input stream. Reading
 * from any input stream will not have any side effects outside of that input stream; in particular, no bytes written to
 * the output stream will ever be removed from the buffer. As a result, there is a maximum number of bytes that can be
 * written to an output stream, and this size is specified at construction time. Writing more than the initially
 * specified size will raise an exception.
 *
 * <p>As an edge case, this class also replicates the functionality of {@link java.io.ByteArrayOutputStream}. The
 * single-argument constructor {@link #QueuingOutputStream(byte[])} may be used in this case.
 *
 * @author Florian Schoppmann
 */
public final class QueuingOutputStream extends OutputStream {
    private final byte[][] byteBuffers;
    private final int maxNumberOfBytes;
    private final int sizeOfFirstBuffer;
    private final int sizeOfLastBuffer;
    private int currentWriteBuffer = 0;
    private int posInCurrentWriteBuffer = 0;

    /**
     * Constructor for instance the supplied array as single buffer.
     *
     * @param buffer Single buffer to use for this output stream.
     */
    public QueuingOutputStream(byte[] buffer) {
        byteBuffers = new byte[][] {buffer};
        maxNumberOfBytes = buffer.length;
        sizeOfFirstBuffer = buffer.length;
        sizeOfLastBuffer = buffer.length;
    }

    /**
     * Constructor for instance with incremental buffer sizes.
     *
     * @param minSize Minimum size (in bytes) that should be allocated for the first buffer. Must be > 0. It is
     *     recommended but not necessary that the size is a power of 2.
     * @param maxNumberOfBytes Maximum number of bytes that will be written to this input stream. Must be >= 0. Writing
     *     more than this many bytes will cause an exception.
     */
    public QueuingOutputStream(int minSize, int maxNumberOfBytes) {
        if (minSize <= 0) {
            throw new IllegalArgumentException(String.format("Expected minSize > 0, but got %d.", minSize));
        } else if (maxNumberOfBytes < 0) {
            throw new IllegalArgumentException(String.format(
                "Expected maxNumberOfBytes >= 0, but got %d", maxNumberOfBytes
            ));
        }

        this.maxNumberOfBytes = maxNumberOfBytes;

        // sizeOfFirstBuffer will contain the size of the first buffer. Each subsequent buffer will be twice the size of
        // its preceding buffer (except for the last buffer, which will simply consume the difference between
        // maxNumberOfBytes and the size of all previous buffers).

        // Set initialSizeLog2 to the smallest non-negative integer j so that 2^j >= minSize
        int initialSizeLog2 = Integer.SIZE - Integer.numberOfLeadingZeros(minSize - 1);

        // Set sizeOfFirstBuffer to 2^j
        sizeOfFirstBuffer = 1 << initialSizeLog2;

        // We want to compute the number of buffers n needed to store maxNumberOfBytes, when the buffer sizes are
        // 2^j, 2^{j+1}, ... 2^{j + n - 1}. The total size of all buffers is
        // sum(2^i, i = j..j + n - 1) = 2^{j + n} - 1 - (2^j - 1) = 2^{j + n} - 2^j.
        // This implies that if 2^{j + n} >= maxNumberOfBytes + 2^j, then n buffers are enough.
        // Hence, set p to the smallest non-negative integer p so that 2^p >= maxNumberOfBytes + 2^j.
        int p = Integer.SIZE - Integer.numberOfLeadingZeros(maxNumberOfBytes + sizeOfFirstBuffer - 1);

        // And then set numberOfBuffers to p - j.
        int numberOfBuffers = p - initialSizeLog2;
        byteBuffers = new byte[numberOfBuffers][];

        // The size of the last buffer may be less than 2^{j + n - 1} if 2^{j + n} >= maxNumberOfBytes + 2^j did not
        // hold with equality. In particular, it only needs to be of size
        // 2^{j + n - 1} - (2^{j + n} - 2^j - maxNumberOfBytes) = maxNumberOfBytes + sizeOfFirstBuffer - 2^{j + n - 1}
        sizeOfLastBuffer = maxNumberOfBytes + sizeOfFirstBuffer - (1 << (initialSizeLog2 + numberOfBuffers - 1));

        // Note that if maxNumberOfBytes == 0, sizeOfLastBuffer is not well-defined. However, no special care is needed
        // because the value is not used in this case anyway.
    }

    private void ensureRemainingCapacityInCurrentBuffer() throws IOException {
        byte[] currentBuffer = byteBuffers.length > currentWriteBuffer
            ? byteBuffers[currentWriteBuffer]
            : null;
        if (currentBuffer != null && posInCurrentWriteBuffer == currentBuffer.length) {
            ++currentWriteBuffer;
            currentBuffer = null;
        }

        // This stream does not support writing more bytes than the maximum size initially passed to the
        // constructor.
        if (currentWriteBuffer == byteBuffers.length) {
            throw new IOException(String.format(
                "Tried to write beyond capacity (%d) of instance of %s.", maxNumberOfBytes, getClass()
            ));
        }

        if (currentBuffer == null) {
            int newSize;
            if (currentWriteBuffer == 0) {
                newSize = sizeOfFirstBuffer;
            } else if (currentWriteBuffer == byteBuffers.length - 1) {
                newSize = sizeOfLastBuffer;
            } else {
                newSize = 2 * byteBuffers[currentWriteBuffer - 1].length;
            }
            byteBuffers[currentWriteBuffer] = new byte[newSize];
            posInCurrentWriteBuffer = 0;
        }
    }

    @Override
    public void write(int singleByte) throws IOException {
        ensureRemainingCapacityInCurrentBuffer();
        byteBuffers[currentWriteBuffer][posInCurrentWriteBuffer] = (byte) singleByte;
        ++posInCurrentWriteBuffer;
    }

    @Override
    public void write(byte[] array, int offset, int length) throws IOException {
        Objects.requireNonNull(array);
        if (offset < 0 || offset > array.length || length < 0 || offset + length > array.length
            || offset + length < 0) {

            throw new IndexOutOfBoundsException(String.format(
                "array length = %d, offset = %d, length = %d", array.length, offset, length
            ));
        } else if (length == 0) {
            return;
        }

        int remaining = length;
        int posInArray = offset;
        while (remaining > 0) {
            ensureRemainingCapacityInCurrentBuffer();
            byte[] currentBuffer = byteBuffers[currentWriteBuffer];
            int blockSize = Math.min(remaining, currentBuffer.length - posInCurrentWriteBuffer);
            System.arraycopy(array, posInArray, currentBuffer, posInCurrentWriteBuffer, blockSize);
            remaining -= blockSize;
            posInArray += blockSize;
            posInCurrentWriteBuffer += blockSize;
        }
    }

    /**
     * Returns the total number of bytes that have been written to this output stream.
     */
    public int getTotalBytesWritten() {
        // Size of all buffers before previous buffer is:
        // sum(2^i, i = j..j + currentWriteBuffer - 1)
        // = 2^{j + currentWriteBuffer} - 1 - (2^j - 1) = 2^j * (2^currentWriteBuffer - 1)
        return sizeOfFirstBuffer * ((1 << currentWriteBuffer) - 1) + posInCurrentWriteBuffer;
    }

    /**
     * Returns a new input stream for reading the bytes previously written to this input stream up to when this method
     * was called.
     *
     * <p>Any bytes written to the input stream after calling this method will not have any effect on the returned input
     * stream. The returned input stream supports {@link InputStream#mark(int)} and {@link InputStream#reset()}, that
     * is, {@link InputStream#markSupported()} returns {@code true}.
     *
     * <p>Without external synchronization, this method should only be called from the thread that also writes to this
     * stream. Likewise, while the returned input stream may be passed on to another thread, without external
     * synchronization only a single through may read from the returned input stream.
     *
     * @return the new input stream
     */
    public InputStream toInputStream() {
        return new ByteArraysInputStream();
    }

    /**
     * Returns the underlying arrays of this stream.
     *
     * <p>Note that this method does not create defensive copies. Hence, this method should be used with care. Also note
     * that the returned array may contain null elements (in which case all subsequent element are also null).
     */
    public byte[][] getArrays() {
        return byteBuffers;
    }

    private final class ByteArraysInputStream extends InputStream {
        private final int size = getTotalBytesWritten();
        private int markedReadBuffer = 0;
        private int posInMarkedReadBuffer = 0;
        private int currentReadBuffer = 0;
        private int posInCurrentReadBuffer = 0;

        private int getTotalBytesRead() {
            // see getTotalBytesWritten() for explanation
            return sizeOfFirstBuffer * ((1 << currentReadBuffer) - 1) + posInCurrentReadBuffer;
        }

        private void normalizePosition() {
            assert getTotalBytesRead() < size;

            if (posInCurrentReadBuffer == byteBuffers[currentReadBuffer].length) {
                ++currentReadBuffer;
                posInCurrentReadBuffer = 0;
            }
        }

        @Override
        public int read() {
            if (getTotalBytesRead() < size) {
                normalizePosition();

                int character = byteBuffers[currentReadBuffer][posInCurrentReadBuffer];
                ++posInCurrentReadBuffer;
                return character;
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] array, int offset, int length) {
            Objects.requireNonNull(array);
            if (offset < 0 || length < 0 || length > array.length - offset) {
                throw new IndexOutOfBoundsException(String.format(
                    "array length = %d, offset = %d, length = %d", array.length, offset, length
                ));
            } else if (length == 0) {
                return 0;
            }

            int remaining = Math.min(size - getTotalBytesRead(), length);
            int bytesRead = 0;
            while (remaining > 0) {
                normalizePosition();

                byte[] currentBuffer = byteBuffers[currentReadBuffer];
                int blockSize = Math.min(remaining, currentBuffer.length - posInCurrentReadBuffer);
                System.arraycopy(currentBuffer, posInCurrentReadBuffer, array, offset + bytesRead, blockSize);
                remaining -= blockSize;
                bytesRead += blockSize;
                posInCurrentReadBuffer += blockSize;
            }
            return bytesRead > 0
                ? bytesRead
                : -1;
        }

        @Override
        public synchronized void mark(int readlimit) {
            markedReadBuffer = currentReadBuffer;
            posInMarkedReadBuffer = posInCurrentReadBuffer;
        }

        @Override
        public synchronized void reset() throws IOException {
            currentReadBuffer = markedReadBuffer;
            posInCurrentReadBuffer = posInMarkedReadBuffer;
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public int available() {
            return size - getTotalBytesRead();
        }
    }
}
