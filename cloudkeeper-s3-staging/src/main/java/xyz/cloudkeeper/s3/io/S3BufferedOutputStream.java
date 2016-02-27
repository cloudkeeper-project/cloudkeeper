package xyz.cloudkeeper.s3.io;

import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartResult;
import net.florianschoppmann.java.futures.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Buffered output stream for writing to S3.
 *
 * <p>This class implements an output stream that satisfies the requirements of
 * {@link S3Connection#newBufferedOutputStream(String, String)}. Data transfer to S3 occurs in parts of
 * size {@link S3Connection#getBufferSize()} (using Amazon S3 multi-part uploads), and the transfer of
 * each part is wrapped in a lightweight task that will be executed by
 * {@link S3Connection#getExecutorService()}. Up to
 * {@link S3Connection#getParallelConnectionsPerRequest()} part uploads may be active at the same time
 * (per stream). Accordingly, the maximum size of all buffers used for the returned stream may be up to the product of
 * {@link S3Connection#getParallelConnectionsPerRequest()} and
 * {@link S3Connection#getBufferSize()}. However, this class will use a
 * {@link QueuingOutputStream} for buffering the first {@link S3Connection#getBufferSize()}
 * bytes. Hence, this class is memory-efficient also for writing many small files to S3.
 *
 * <p>An output stream of this class will split the upload into multiple parts (using Amazon S3 multi-part uploads). It
 * is therefore crucial that this stream will be closed, that is, the {@link #close()} will be called eventually.
 * Otherwise, Amazon may keep an incomplete S3 multi-part upload, which will incur cost. Whenever possible, instances of
 * this class should therefore only be created in try-with-resources statements. Moreover, as a precaution, it is
 * recommended that users of this class periodically check for incomplete multi-part uploads and abort stale uploads.
 *
 * <p>Instances of this class maintain {@link S3Connection#getParallelConnectionsPerRequest()} buffers of size
 * {@link S3Connection#getBufferSize()} each. There is a (blocking) queue of available buffers, the first of which is
 * the <em>current</em> buffer. Any write operation is directed to the current buffer if there is one. Otherwise, the
 * write will block until a buffer becomes available again. When the current buffer is full, a new S3 part upload is
 * started as a parallel task, and the current buffer will become unavailable; that is, it is removed from the queue,
 * and it is no longer the current buffer. Once an upload completes, the associated buffer is returned to the queue.
 *
 * <p>Streams of this class are not intended to be written to from different threads (which holds for any
 * {@link OutputStream}, unless otherwise noted). However, instances of this class will submit futures to an
 * executor that is expected to complete these futures in different threads. This is safe because all futures submitted
 * from this class will only access volatile, final, or static fields.
 *
 * @see com.amazonaws.services.s3.AmazonS3#initiateMultipartUpload(com.amazonaws.services.s3.model.InitiateMultipartUploadRequest)
 *
 * @author Florian Schoppmann
 */
final class S3BufferedOutputStream extends OutputStream {
    /**
     * Initial buffer size. This is the minimum size that will be allocated when the first byte is written. This
     * constant will be passed to {@link QueuingOutputStream#QueuingOutputStream(int, int)} as first
     * argument.
     */
    private static final int INITIAL_BUFFER_SIZE = 256;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final S3Connection s3Connection;
    private final Executor executorService;
    private final String bucketName;
    private final String key;
    private final int numBuffers;
    private final int bufferSize;

    private volatile boolean failure;
    @Nullable private QueuingOutputStream currentBuffer;
    private int posInBuffer = 0;
    private boolean closed = false;

    private int nextPartNumber = 1;

    /**
     * The number of byte arrays that have been explicitly created. This only happens in {@link #waitForBuffer()}.
     */
    private int numByteArraysCreated = 0;

    /**
     * Blocking queue containing buffer to write to. If no buffer is currently available and a write method is called,
     * the operation will block until a buffer becomes available again. Note that {@link BlockingQueue} implementations
     * are always thread-safe.
     */
    private final BlockingQueue<QueuingOutputStream> availableBuffers;

    /**
     * Set of part-upload futures. Note that this field is initially null, and will be initialized together with
     * {@link #uploadIdFuture} in {@link #getUploadIdFuture()};
     */
    @Nullable private List<CompletableFuture<PartETag>> partETagFutures;

    /**
     * This field is not written to from callback methods (but instead only when {@link OutputStream} methods
     * are on the call stack), hence we do not have to worry about the memory model. No need to make this field
     * volatile.
     */
    @Nullable private CompletableFuture<String> uploadIdFuture;

    /**
     * This field exists for convenience. It is volatile because it is written to from a callback registered with
     * {@link #uploadIdFuture}.
     */
    @Nullable private volatile String uploadId;

    /**
     * Constructor.
     *
     * @param s3Connection high-level connection to Amazon S3
     * @param bucketName name of the bucket that will contain the new object
     * @param key key of the object to create and write to
     */
    S3BufferedOutputStream(S3Connection s3Connection, String bucketName, String key) {
        Objects.requireNonNull(s3Connection);
        Objects.requireNonNull(bucketName);
        Objects.requireNonNull(key);

        this.s3Connection = s3Connection;
        executorService = s3Connection.getExecutorService();
        this.bucketName = bucketName;
        this.key = key;
        bufferSize = s3Connection.getBufferSize();

        numBuffers = s3Connection.getParallelConnectionsPerRequest();
        availableBuffers = new ArrayBlockingQueue<>(numBuffers);
        currentBuffer = new QueuingOutputStream(INITIAL_BUFFER_SIZE, bufferSize);
    }

    /**
     * Enumeration of the conditions that will trigger uploading a new part, when
     * {@link #uploadPartToS3(WhenToUpload)} is called.
     */
    enum WhenToUpload {
        /**
         * Indicates that a new part should only be uploaded if the buffer is full.
         */
        WHEN_BUFFER_FULL,

        /**
         * Indicates that a new part should be uploaded if the buffer contains enough data to start a new part (that is,
         * if there are more than {@link S3Connection#MINIMUM_PART_SIZE} bytes in the buffer).
         */
        WHEN_ENOUGH_FOR_NEW_PART,

        /**
         * Indicates that a new part should always be uploaded, because this part will be the last, and the stream will
         * be closed afterward.
         */
        ALWAYS
    }

    private CompletableFuture<String> getUploadIdFuture() {
        if (uploadIdFuture == null) {
            uploadIdFuture = s3Connection.initiateMultipartUpload(bucketName, key)
                .thenApplyAsync(InitiateMultipartUploadResult::getUploadId, executorService);
            partETagFutures = new ArrayList<>();
        }
        return uploadIdFuture;
    }

    /**
     * Uploads the current buffer to Amazon S3.
     *
     * <p>Depending on {@code whenToUpload}, this method will do nothing if the buffer contains only little data. In
     * particular,since S3 has minimum part sizes, this method will usually not do anything if less than
     * {@link S3Connection#MINIMUM_PART_SIZE} bytes are available. Only if the current content of the
     * buffer is guaranteed to be the last part, data would be uploaded.
     *
     * @param whenToUpload indicates under what conditions a new part should be uploaded
     */
    private void uploadPartToS3(WhenToUpload whenToUpload) {
        // Necessary precondition
        assert currentBuffer != null;

        boolean uploadNow = whenToUpload == WhenToUpload.ALWAYS
            || (whenToUpload == WhenToUpload.WHEN_ENOUGH_FOR_NEW_PART && posInBuffer >= S3Connection.MINIMUM_PART_SIZE)
            || posInBuffer == bufferSize;
        if (!uploadNow) {
            return;
        }

        // We must not close over mutable instance fields, so we need to create local copies. In all callbacks below,
        // we only access final or volatile fields.
        final QueuingOutputStream buffer = currentBuffer;
        final int length = posInBuffer;
        final int partNumber = nextPartNumber;

        currentBuffer = null;
        posInBuffer = 0;
        ++nextPartNumber;

        // Sequence of steps: Get upload ID, upload new part, extract identifier from S3 upload result, register
        // callback for handling failures and returning the buffer.
        getUploadIdFuture();
        assert uploadIdFuture != null && partETagFutures != null;
        CompletableFuture<PartETag> partETagFuture = uploadIdFuture
            .thenComposeAsync(
                newUploadId -> {
                    // UploadId is volatile, so fine to write to.
                    uploadId = newUploadId;
                    return s3Connection.uploadPart(bucketName, key, newUploadId, partNumber,
                        buffer.toInputStream(), length);
                },
                executorService
            )
            .thenApplyAsync(UploadPartResult::getPartETag, executorService)
            .whenCompleteAsync(
                (partETag, throwable) -> {
                    if (throwable != null) {
                        // failure is volatile, so fine to write to.
                        failure = true;
                    }

                    // Return the buffer to the pool. Fine to do here, as BlockingQueue implementations are thread-safe.
                    byte[][] bufferArrays = buffer.getArrays();
                    if (bufferArrays.length == 1 && bufferArrays[0].length == bufferSize) {
                        // Reuse the buffer if it was backed by a single array of the correct size.
                        availableBuffers.add(new QueuingOutputStream(bufferArrays[0]));
                    }
                },
                executorService
            );

        partETagFutures.add(partETagFuture);
    }

    private IOException mapException(Exception exception) {
        Throwable cause = exception instanceof ExecutionException
            ? exception.getCause()
            : exception;

        if (cause instanceof IOException) {
            return (IOException) cause;
        } else {
            return new IOException(String.format("Writing s3://%s/%s failed.", bucketName, key), cause);
        }
    }

    private void waitForBuffer() throws IOException {
        if (closed) {
            throw new IOException("Write attempt after stream was closed.");
        } else if (failure) {
            // Note that exception != null implies that a future was completed with an exception, which means that
            // close() picks up the exception and throws it.
            close();
            assert false : "Unreachable code";
        } else if (currentBuffer == null) {
            if (availableBuffers.isEmpty() && numByteArraysCreated < numBuffers) {
                // In theory, a QueuingOutputStream could become available again right here. But it clearly does not
                // hurt to create a new buffer here.
                availableBuffers.add(new QueuingOutputStream(new byte[bufferSize]));
                ++numByteArraysCreated;
            }

            boolean interrupted = false;
            while (true) {
                try {
                    // This cannot block indefinitely because eventually all buffers will be returned to
                    // availableBuffers (either when a part upload completes or fails). See uploadPartToS3().
                    // Initially, availableBuffers is not empty.
                    currentBuffer = availableBuffers.take();
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
        }

        // Post-condition of this method upon successful completion.
        assert currentBuffer != null && posInBuffer < bufferSize;
    }

    @Override
    public void write(int singleByte) throws IOException {
        // See method for post-condition.
        waitForBuffer();
        assert currentBuffer != null;

        posInBuffer++;
        currentBuffer.write(singleByte);
        uploadPartToS3(WhenToUpload.WHEN_BUFFER_FULL);
    }

    @Override
    public void write(byte[] newBytes, int offset, int length) throws IOException {
        Objects.requireNonNull(newBytes);
        if (offset < 0 || offset > newBytes.length || length < 0 || offset + length > newBytes.length
                || offset + length < 0) {
            throw new IndexOutOfBoundsException(String.format(
                "array length = %d, offset = %d, length = %d", newBytes.length, offset, length
            ));
        } else if (length == 0) {
            return;
        }

        int posInNewBytes = offset;
        int remaining = length;
        while (remaining > 0) {
            // See method for post-condition.
            waitForBuffer();
            assert currentBuffer != null;

            int blockSize = Math.min(remaining, bufferSize - posInBuffer);
            currentBuffer.write(newBytes, posInNewBytes, blockSize);
            remaining -= blockSize;
            posInNewBytes += blockSize;
            posInBuffer += blockSize;
            uploadPartToS3(WhenToUpload.WHEN_BUFFER_FULL);
        }
    }

    @Override
    public void flush() throws IOException {
        uploadPartToS3(WhenToUpload.WHEN_ENOUGH_FOR_NEW_PART);
    }

    /**
     * Uploads the last part of the multi-part upload and then completes the request.
     *
     * <p>This method must only be called from {@link #close()} if a multi-part upload has been initiated previously
     * because a single buffer could not hold all the bytes written to this stream.
     */
    private CompletableFuture<?> finishMultiPartUploadFuture() {
        assert uploadIdFuture != null;

        // There may be bytes in the buffer that have not been submitted to S3 yet. This may update partETagFutures.
        uploadPartToS3(WhenToUpload.ALWAYS);

        // Now partETagFutures contains all partETagFutures
        CompletableFuture<CompleteMultipartUploadResult> completionFuture = Futures
            .shortCircuitCollect(partETagFutures)
            .thenComposeAsync(
                partETags -> s3Connection.completeMultipartUpload(bucketName, key, uploadId, partETags),
                executorService
            );

        // If anything goes wrong, it is crucial that we abort the multi-part upload. If the abort request also fails,
        // all we can do is write a log message. Exceptions occurring during the abort request will not otherwise be
        // propagated.
        completionFuture.whenCompleteAsync(
            (ignoredUploadResult, uploadThrowable) -> {
                if (uploadThrowable != null) {
                    CompletableFuture<Void> abortFuture = s3Connection.abortMultipartUpload(bucketName, key, uploadId);
                    abortFuture.whenCompleteAsync(
                        (ignoredVoid, abortThrowable) -> {
                            if (abortThrowable != null) {
                                log.error(String.format(
                                    "Failed to abort multi-part upload for key '%s'. MANUAL CLEAN-UP REQUIRED!", key
                                ), abortThrowable);
                            }
                        },
                        executorService
                    );
                }
            },
            executorService
        );

        return completionFuture;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            // If uploadIfFuture == null, then the number of written bytes is less than the buffer size. In this case,
            // no multi-part upload has been started.
            CompletableFuture<?> completionFuture = uploadIdFuture == null
                ? s3Connection.putObject(bucketName, key, currentBuffer.toInputStream(), posInBuffer)
                : finishMultiPartUploadFuture();

            try {
                completionFuture.get();
            } catch (ExecutionException | InterruptedException exception) {
                throw mapException(exception);
            } finally {
                // Release references and let garbage collector do its work
                partETagFutures = null;
                availableBuffers.clear();
            }
        }
    }
}
