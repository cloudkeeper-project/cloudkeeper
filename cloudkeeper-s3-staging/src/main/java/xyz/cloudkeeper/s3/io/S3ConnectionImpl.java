package xyz.cloudkeeper.s3.io;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a high-level representation of a connection (and its parameters) to Amazon S3.
 */
final class S3ConnectionImpl implements S3Connection {
    /**
     * Logarithm base 2 of the binary prefix "mebi", as in one mebibyte = {@code 2^20} bytes.
     */
    private static final int LOG_2_MEBI = 20;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ClientConfiguration s3ClientConfiguration;
    private final AmazonS3 s3Client;
    private final ScheduledExecutorService executorService;
    private final int parallelConnectionsPerRequest;
    private final int bufferSize;
    private final boolean serverSideEncrypted;

    /**
     * Constructor.
     *
     * @param s3ClientConfiguration Amazon S3 client configuration; the object will be copied, and any subsequent
     *     modifications to the object passed as argument will not have any effect.
     * @param s3Client Amazon S3 client; note that the S3 client is thread-safe and reusing it is encouraged
     * @param executorService executor service that supports scheduling tasks in advance and that S3 requests
     *     will be submitted to
     * @param parallelConnectionsPerRequest maximum number of parallel requests to S3 per SFTP request
     * @param bufferSize Size of in-memory buffers (in bytes) for routing between SFTP and S3 input/output streams. The
     *     buffer size must be at least the minimum size of a part in a multi-part S3 upload
     *     ({@link #MINIMUM_PART_SIZE}).
     * @param serverSideEncrypted Whether any data uploaded to S3 will be encrypted on the server side.
     *
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if an argument does not satisfy the constraints given above
     */
    S3ConnectionImpl(ClientConfiguration s3ClientConfiguration, AmazonS3 s3Client,
            ScheduledExecutorService executorService, int parallelConnectionsPerRequest,
            int bufferSize, boolean serverSideEncrypted) {
        this.s3ClientConfiguration = new ClientConfiguration(Objects.requireNonNull(s3ClientConfiguration));
        this.s3Client = Objects.requireNonNull(s3Client);
        this.executorService = Objects.requireNonNull(executorService);

        if (bufferSize < MINIMUM_PART_SIZE) {
            throw new IllegalArgumentException(String.format(
                "Expected bucket size >= %d MiB, but got %d B.", MINIMUM_PART_SIZE / (1 << LOG_2_MEBI), bufferSize
            ));
        }

        this.parallelConnectionsPerRequest = parallelConnectionsPerRequest;
        this.bufferSize = bufferSize;
        this.serverSideEncrypted = serverSideEncrypted;
    }

    @Override
    public ClientConfiguration getS3ClientConfiguration() {
        return new ClientConfiguration(s3ClientConfiguration);
    }

    @Override
    public AmazonS3 getS3Client() {
        return s3Client;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public int getParallelConnectionsPerRequest() {
        return parallelConnectionsPerRequest;
    }

    @Override
    public boolean isServerSideEncrypted() {
        return serverSideEncrypted;
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public CompletableFuture<ObjectListing> listObjects(String bucketName, String prefix, String separator,
        Integer maxKeys) {

        final ListObjectsRequest listObjectsRequest
            = new ListObjectsRequest(bucketName, prefix, null, separator, maxKeys);

        return CompletableFuture.supplyAsync(() -> s3Client.listObjects(listObjectsRequest), executorService);
    }

    @Override
    public CompletableFuture<ObjectListing> listNextBatchOfObjects(final ObjectListing previousObjectListing) {
        return CompletableFuture.supplyAsync(
            () -> s3Client.listNextBatchOfObjects(previousObjectListing),
            executorService
        );
    }

    /**
     * Task that retrieves a chunk of an S3 object.
     */
    private final class S3ReadTask implements Runnable {
        private final String bucketName;
        private final String key;
        private final long offsetInS3Object;
        private final byte[] target;
        private final CompletableFuture<Long> promise;
        private int posInTarget = 0;
        private int retriesAttempted = 0;

        private S3ReadTask(String bucketName, String key, long offsetInS3Object, byte[] target,
                CompletableFuture<Long> promise) {
            this.bucketName = bucketName;
            this.key = key;
            this.offsetInS3Object = offsetInS3Object;
            this.target = target;
            this.promise = promise;
        }

        /**
         * Reads bytes into {@link #target}, until either the end of {@link #target} or the end of the S3 object is
         * reached.
         *
         * @param s3Object the S3 object
         * @param getObjectRequest the S3 get-object request used for retrieving {@code s3Object}
         * @return the total size of the S3 object
         * @throws AmazonClientException if a call to {@link S3ObjectInputStream#read(byte[], int, int)} does not read
         *     any bytes even though it should have
         * @throws IOException if a call to {@link S3ObjectInputStream#read(byte[], int, int)} throws an I/O exception
         */
        private long readS3Object(@Nullable S3Object s3Object, GetObjectRequest getObjectRequest) throws IOException {
            long totalSize;
            if (s3Object == null) {
                totalSize = s3Client.getObjectMetadata(bucketName, key).getInstanceLength();
                if (offsetInS3Object < totalSize) {
                    throw new AmazonClientException(String.format(
                        "Could not read %s (range: %s), because AmazonS3#getClient() returned null.",
                        key, Arrays.toString(getObjectRequest.getRange())
                    ));
                }
            } else {
                totalSize = s3Object.getObjectMetadata().getInstanceLength();
                // Note that the (int) cast is safe because target.length is of type int.
                int remainingBytesToRead
                    = (int) Math.max(0, Math.min(target.length - posInTarget, totalSize - offsetInS3Object));

                S3ObjectInputStream inputStream = s3Object.getObjectContent();
                int bytesRead;
                while (remainingBytesToRead > 0) {
                    // read() promises to read "up to" remainingBytesToRead bytes. There is no guarantee that
                    // this many bytes are read, even if enough bytes are available. In fact, experiments showed
                    // that read() sometimes only returns 2^15 bytes.
                    bytesRead = inputStream.read(target, posInTarget, remainingBytesToRead);
                    posInTarget += bytesRead;
                    remainingBytesToRead -= bytesRead;
                    if (bytesRead <= 0) {
                        // This should not happen and indicates a logical bug. We therefore fail here.
                        throw new AmazonClientException(String.format(
                            "Could not read %s (range: %s). Requested %d bytes from input stream, but "
                                + "S3ObjectInputStream#read() returned %d.",
                            key, Arrays.toString(getObjectRequest.getRange()),
                            remainingBytesToRead, bytesRead
                        ));
                    }
                }
            }
            return totalSize;
        }

        /**
         * Returns whether this method was able to initiate recovery from the given exception.
         *
         * <p>If recovery is possible and if the retry policy returned by {@link ClientConfiguration#getRetryPolicy()}
         * suggests to perform another transmission attempt, this methods schedules this task to be run again, with a
         * delay as returned by the back-off strategy of the retry policy.
         *
         * @param amazonClientException the exception that will be passed to the retry policy and the back-off strategy
         * @param getObjectRequest the S3 get-object request
         * @return whether this method was able to initiate recovery
         *
         * @see RetryPolicy.BackoffStrategy#delayBeforeNextRetry(com.amazonaws.AmazonWebServiceRequest, AmazonClientException, int)
         */
        private boolean couldRecoverFromException(AmazonClientException amazonClientException,
            GetObjectRequest getObjectRequest) {

            RetryPolicy retryPolicy = s3ClientConfiguration.getRetryPolicy();
            int configuredMaxErrorRetry = s3ClientConfiguration.getMaxErrorRetry();
            // The following is copied from AmazonHttpClient#shouldRetry(): "We should use the maxErrorRetry in the
            // RetryPolicy if either the user has not explicitly set it in ClientConfiguration, or the RetryPolicy
            // is configured to take higher precedence."
            int maxErrorRetry = (configuredMaxErrorRetry < 0 || !retryPolicy.isMaxErrorRetryInClientConfigHonored())
                ? retryPolicy.getMaxErrorRetry()
                : configuredMaxErrorRetry;

            RetryPolicy.RetryCondition retryCondition = retryPolicy.getRetryCondition();
            boolean returnValue = false;
            if (retriesAttempted <= maxErrorRetry
                && retryCondition.shouldRetry(getObjectRequest, amazonClientException, retriesAttempted)) {

                long delay = retryPolicy.getBackoffStrategy()
                    .delayBeforeNextRetry(getObjectRequest, amazonClientException, retriesAttempted);
                ++retriesAttempted;

                // java.util.concurrent states: "Actions in a thread prior to the submission of a Runnable to an
                // Executor happen-before its execution begins." Consequently, memory consistency with the thread that
                // will execute this task is guaranteed.
                // Note that the state of this object will not be touched after this line (-> tail recursion).
                executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
                returnValue = true;
            }
            return returnValue;
        }

        /**
         * Performs the actual data transfer.
         */
        private void transfer() {
            // Strangely, withRange() expects an inclusive end parameter
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key)
                .withRange(offsetInS3Object + posInTarget, offsetInS3Object + target.length - 1);

            // getObject() may return null if some constraints of the request cannot be met. Thanks to JDK-7020047,
            // the try-with-resources statement protects the automatic call to close() with a non-null check.
            try (@Nullable S3Object s3Object = s3Client.getObject(getObjectRequest)) {
                long totalSize = readS3Object(s3Object, getObjectRequest);
                promise.complete(totalSize);
            } catch (IOException exception) {
                AmazonClientException amazonClientException = new AmazonClientException(String.format(
                    "Could not read %s (range: %s).", key, Arrays.toString(getObjectRequest.getRange())
                ), exception);
                boolean couldRecover = couldRecoverFromException(amazonClientException, getObjectRequest);
                if (!couldRecover) {
                    promise.completeExceptionally(amazonClientException);
                }
            }
        }

        /**
         * Wrapper around the actual data transfer.
         *
         * <p>See {@link java.util.concurrent} for an explanation why memory consistency (in the sense of §17.4 JLS) is
         * guaranteed.
         */
        @Override
        public void run() {
            try {
                transfer();
            } catch (RuntimeException exception) {
                // This should not happen. But to be on the safe side in case of programming errors, we catch exceptions
                // here and complete the promise exceptionally.
                promise.completeExceptionally(exception);
                log.warn("Unexpected runtime exception caught.", exception);
            }
        }
    }

    @Override
    public CompletableFuture<Long> readBytesWithinRange(final String bucketName, final String key, final long offset,
            final byte[] target) {
        CompletableFuture<Long> promise = new CompletableFuture<>();
        S3ReadTask s3ReadTask = new S3ReadTask(bucketName, key, offset, target, promise);
        executorService.execute(s3ReadTask);
        return promise;
    }

    @Override
    public CompletableFuture<PutObjectResult> putObject(String bucketName, String key, InputStream inputStream,
            int length) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(length);
        if (serverSideEncrypted) {
            objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        }
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, objectMetadata);
        return CompletableFuture.supplyAsync(() -> s3Client.putObject(putObjectRequest), executorService);
    }

    @Override
    public CompletableFuture<Void> deleteObject(String bucketName, String key) {
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, key);
        return CompletableFuture.runAsync(() -> s3Client.deleteObject(deleteObjectRequest), executorService);
    }

    @Override
    public CompletableFuture<Void> moveObject(String fromBucketName, String fromKey, String toBucketName,
            String toKey) {
        CopyObjectRequest copyObjectResult
            = new CopyObjectRequest(fromBucketName, fromKey, toBucketName, toKey);
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(fromBucketName, fromKey);
        return CompletableFuture.runAsync(
            () -> {
                s3Client.copyObject(copyObjectResult);
                s3Client.deleteObject(deleteObjectRequest);
            },
            executorService
        );
    }

    @Override
    public CompletableFuture<InitiateMultipartUploadResult> initiateMultipartUpload(String bucketName, String key) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        if (serverSideEncrypted) {
            objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            request.setObjectMetadata(objectMetadata);
        }
        return CompletableFuture.supplyAsync(() -> s3Client.initiateMultipartUpload(request), executorService);
    }

    @Override
    public CompletableFuture<UploadPartResult> uploadPart(String bucketName, String key, String uploadId,
            int partNumber, InputStream inputStream, int length) {
        UploadPartRequest request = new UploadPartRequest()
            .withBucketName(bucketName)
            .withKey(key)
            .withUploadId(uploadId)
            .withPartNumber(partNumber)
            .withInputStream(inputStream)
            .withPartSize(length);
        return CompletableFuture.supplyAsync(() -> s3Client.uploadPart(request), executorService);
    }

    @Override
    public CompletableFuture<CompleteMultipartUploadResult> completeMultipartUpload(String bucketName, String key,
            String uploadId, List<PartETag> partETags) {
        CompleteMultipartUploadRequest request
            = new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
        return CompletableFuture.supplyAsync(() -> s3Client.completeMultipartUpload(request), executorService);
    }

    @Override
    public CompletableFuture<Void> abortMultipartUpload(String bucketName, String key, String uploadId) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(bucketName, key, uploadId);
        return CompletableFuture.runAsync(() -> s3Client.abortMultipartUpload(request), executorService);
    }

    @Override
    public InputStream newBufferedInputStream(String bucketName, String key, long offset) {
        return new S3BufferedInputStream(this, bucketName, key, offset);
    }

    @Override
    public OutputStream newBufferedOutputStream(String bucketName, String key) {
        return new S3BufferedOutputStream(this, bucketName, key);
    }
}
