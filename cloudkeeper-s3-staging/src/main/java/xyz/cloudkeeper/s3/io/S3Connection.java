package xyz.cloudkeeper.s3.io;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.UploadPartResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This interface provides a high-level representation of a connection (and its parameters) to Amazon S3.
 *
 * <p>As a high-level API, methods for interacting with S3 may provide built-in error recovery, such as transparently
 * retrying communication if an intermittent exception occurs.
 *
 * <p>As explained in package {@link java.util.concurrent}, memory consistency is guaranteed without explicit
 * synchronization if results are consumed in the usual ways (using {@link CompletableFuture#get()} or by calling any of
 * the computation-chaining methods in {@link CompletableFuture}).
 */
public interface S3Connection {
    /**
     * Minimum size of an upload part.
     *
     * <p>The buffer size must be at least 5 MB, which is the minimum part size in a multi-part upload (except for the
     * last part). This stream will fill the buffer before transmitting any data to S3.
     *
     * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/mpUploadUploadPart.html">Amazon Simple Storage
     * Service: Upload Part</a>
     */
    int MINIMUM_PART_SIZE = 5 * 1024 * 1024;

    /**
     * Returns the Amazon S3 client configuration.
     *
     * <p>The returned object is a defensive copy, and therefore any modifications will not have any effect on the S3
     * connection.
     */
    ClientConfiguration getS3ClientConfiguration();

    /**
     * Returns the Amazon S3 client.
     *
     * <p>The S3 client is thread-safe and should be shared among threads.
     */
    AmazonS3 getS3Client();

    /**
     * Returns the executor used for executing parallel tasks (and, ultimately, asynchronous Amazon S3 requests).
     */
    ScheduledExecutorService getExecutorService();

    /**
     * Returns whether data transferred to S3 will be encrypted on the server side.
     *
     * @see com.amazonaws.services.s3.model.ObjectMetadata#setServerSideEncryption(String)
     */
    boolean isServerSideEncrypted();

    /**
     * Returns the maximum number of parallel requests to S3 per stream. This is equivalent the maximum number of
     * buffers per stream.
     *
     * For instance, this number is the maximum number of part-upload requests to Amazon S3 that can be active in
     * parallel for a single {@link S3BufferedOutputStream} instance.
     *
     * @see #getBufferSize()
     */
    int getParallelConnectionsPerRequest();

    /**
     * Returns the size of a single buffer (in bytes) used for streaming.
     *
     * <p>For instance, this is the size of each part-upload request to Amazon S3 that instances of
     * {@link S3BufferedOutputStream} submit.
     *
     * <p>Note that each stream may use up to {@link #getParallelConnectionsPerRequest()} buffers, so the total memory
     * size reserved by a single stream is at least the product of {@link #getParallelConnectionsPerRequest()} and the
     * buffer size.
     *
     * @see #getParallelConnectionsPerRequest()
     */
    int getBufferSize();

    /**
     * Returns a listing of objects with the given prefix.
     *
     * @param maxKeys optional parameter indicating the maximum number of keys to include in the response
     *
     * @return Future that will be completed with an object listing on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#listObjects(com.amazonaws.services.s3.model.ListObjectsRequest)
     */
    CompletableFuture<ObjectListing> listObjects(String bucketName, String prefix, String separator, Integer maxKeys);

    /**
     * Returns the next batch an object listing that was previously truncated.
     *
     * @return Future that will be completed with an object listing on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#listNextBatchOfObjects(com.amazonaws.services.s3.model.ObjectListing)
     */
    CompletableFuture<ObjectListing> listNextBatchOfObjects(ObjectListing previousObjectListing);

    /**
     * Reads a range from an Amazon S3 object.
     *
     * <p>In case of success, the returned future will be completed with <em>the total size</em> of the object
     * identified by {@code key}. That is, if {@code size} is the value that the future is completed with, then exactly
     * {@code Math.min(size - offset, target.length)} (or zero if that is negative) bytes will have been read.
     * Otherwise, the returned future will be completed exceptionally.
     *
     * <p>Memory consistency effects: Writing the received bytes to {@code target} <i>happens-before</i> the result is
     * retrieved via {@link CompletableFuture#get()}. It also <i>happens-before</i> the computation for a chained future
     * is started.
     *
     * @return Future that will be completed with the total size of the object on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#getObject(com.amazonaws.services.s3.model.GetObjectRequest)
     * @see com.amazonaws.services.s3.model.S3Object#getObjectContent()
     */
    CompletableFuture<Long> readBytesWithinRange(String bucketName, String key, long offset, byte[] target);

    /**
     * Uploads the entire content of the given byte buffer with a single S3 put-object request.
     *
     * @return Future that will be completed with an put-object result on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#putObject(com.amazonaws.services.s3.model.PutObjectRequest)
     */
    CompletableFuture<PutObjectResult> putObject(String bucketName, String key, InputStream inputStream, int length);

    /**
     * Initiates a multi-part upload.
     *
     * @return Future that will be completed with an instantiate-multipart-upload result on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#initiateMultipartUpload(com.amazonaws.services.s3.model.InitiateMultipartUploadRequest)
     */
    CompletableFuture<InitiateMultipartUploadResult> initiateMultipartUpload(String bucketName, String key);

    /**
     * Uploads a new part of a multi-part upload.
     *
     * @return Future that will be completed with an upload-part result on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#uploadPart(com.amazonaws.services.s3.model.UploadPartRequest)
     */
    CompletableFuture<UploadPartResult> uploadPart(String bucketName, String key, String uploadId, int partNumber,
        InputStream inputStream, int length);

    /**
     * Completes a multi-part upload.
     *
     * @return Future that will be completed with an complete-multipart-upload result on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#completeMultipartUpload(com.amazonaws.services.s3.model.CompleteMultipartUploadRequest)
     */
    CompletableFuture<CompleteMultipartUploadResult> completeMultipartUpload(String bucketName, String key,
        String uploadId, List<PartETag> partETags);

    /**
     * Aborts a multi-part upload.
     *
     * @return Future that will be completed with an abort-multipart-upload result on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#abortMultipartUpload(com.amazonaws.services.s3.model.AbortMultipartUploadRequest)
     */
    CompletableFuture<Void> abortMultipartUpload(String bucketName, String key, String uploadId);

    /**
     * Moves the given key to another key. This involves copying the object and then deleting the original.
     *
     * @return Future that will be completed with {@code null} on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#copyObject(com.amazonaws.services.s3.model.CopyObjectRequest)
     * @see AmazonS3#deleteObject(com.amazonaws.services.s3.model.DeleteObjectRequest)
     */
    CompletableFuture<Void> moveObject(String fromBucketName, String fromKey, String toBucketName, String toKey);

    /**
     * Deletes the given key.
     *
     * @return Future that will be completed with {@code null} on success, and an
     *     {@link com.amazonaws.AmazonClientException} in case of transmission failure. The future may also be completed
     *     with another runtime time exception; however, this indicates a logical bug (programming error).
     * @see AmazonS3#deleteObject(com.amazonaws.services.s3.model.DeleteObjectRequest)
     */
    CompletableFuture<Void> deleteObject(String bucketName, String key);

    /**
     * Creates a new buffered input stream for reading from S3.
     *
     * Data transfer from S3 occurs in parts of size {@link #getBufferSize()} (using Amazon S3 get-object requests that
     * include a range), and the transfer of each part is wrapped in a lightweight task that will be executed by
     * {@link #getExecutorService()}. Up to {@link #getParallelConnectionsPerRequest()} part downloads may be active at
     * the same time (per stream). Accordingly, the total size of all buffers used for the returned stream may be up to
     * the product of {@link #getParallelConnectionsPerRequest()} and {@link #getBufferSize()}. The total buffer size is
     * also upper-bounded by the size of the S3 object; hence, downloading many small files from S3 is guaranteed not to
     * consume an unduly amount of memory.
     *
     * @param bucketName name of the bucket that contains the object
     * @param key key of the object to read from
     * @param offset offset (in bytes) where to start reading from
     * @return the new buffered input stream for reading from S3
     */
    InputStream newBufferedInputStream(String bucketName, String key, long offset);

    /**
     * Creates a new buffered output stream for writing to S3.
     *
     * Data transfer to S3 occurs in parts of size {@link #getBufferSize()} (using Amazon S3 multi-part uploads),
     * and the transfer of each part is wrapped in a lightweight task that will be executed
     * by {@link #getExecutorService()}. Up to {@link #getParallelConnectionsPerRequest()} part uploads may be active at
     * the same time (per stream). Accordingly, the total size of all buffers used for the returned stream may be up to
     * the product of {@link #getParallelConnectionsPerRequest()} and {@link #getBufferSize()}. In case the S3
     * connection is faster than the speed at which bytes are written into this stream, less than the maximum buffer
     * size may be used.
     *
     * It is crucial that this stream will be closed, that is, the {@link OutputStream#close()} method will be
     * called eventually. Otherwise, Amazon may keep an incomplete S3 multi-part upload, which will incur cost. Whenever
     * possible, this method should therefore only be used in try-with-resources statements. Moreover, as a precaution,
     * it is recommended that users of this method periodically check for incomplete multi-part uploads and abort stale
     * uploads.
     *
     * Note that the number of bytes that can be written to the returned stream is limited by the maximum number of
     * parts in a multi-part upload and the buffer/part size. Currently, Amazon S3 supports 10,000 parts, and the
     * minimum part size is 5 MB (which is the default buffer size). So with the default settings, no more than 50 GB
     * may be written.
     *
     * @param bucketName name of the bucket that will contain the new object
     * @param key key of the object to create and write to
     * @return the new buffered output stream for writing to S3
     *
     * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/dev/qfacts.html">AWS Documentation: Multipart Upload
     *     Overview: Quick Facts</a>
     */
    OutputStream newBufferedOutputStream(String bucketName, String key);
}
