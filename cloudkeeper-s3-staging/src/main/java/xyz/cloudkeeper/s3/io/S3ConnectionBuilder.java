package xyz.cloudkeeper.s3.io;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Builder for an {@link S3Connection} instance.
 */
public final class S3ConnectionBuilder {
    private final AmazonS3 s3Client;
    private final ScheduledExecutorService executorService;
    @Nullable private ClientConfiguration s3ClientConfiguration;
    private int parallelConnectionsPerRequest = 2;
    private int bufferSize = S3Connection.MINIMUM_PART_SIZE;
    private boolean serverSideEncryption = true;

    /**
     * Constructor.
     *
     * <p>Note that the Amazon S3 client is thread-safe and should be shared among threads.
     *
     * @param s3Client Amazon S3 client
     * @param executorService the executor service used for executing parallel tasks (and, ultimately, asynchronous
     *     Amazon S3 requests)
     *
     * @see S3Connection#getS3Client()
     * @see S3Connection#getExecutorService()
     */
    public S3ConnectionBuilder(AmazonS3 s3Client, ScheduledExecutorService executorService) {
        this.s3Client = Objects.requireNonNull(s3Client);
        this.executorService = Objects.requireNonNull(executorService);
    }

    /**
     * Sets the S3 client configuration.
     *
     * <p>The configuration will be used when transferring data from S3 via
     * {@link S3Connection#readBytesWithinRange(String, String, long, byte[])}. Specifically, if an I/O exception occurs
     * while reading from the stream returned by {@link com.amazonaws.services.s3.model.S3Object#getObjectContent()},
     * the configured retry policy will be used for re-attempting the download.
     *
     * <p>It is recommended that this is the same client configuration that was used to construct the {@link AmazonS3}
     * instance. That way, the retry policy has to be configured only once.
     *
     * @param s3ClientConfiguration the Amazon S3 client configuration
     *
     * @see S3Connection#getS3ClientConfiguration()
     */
    public S3ConnectionBuilder setS3ClientConfiguration(ClientConfiguration s3ClientConfiguration) {
        this.s3ClientConfiguration = s3ClientConfiguration;
        return this;
    }

    /**
     * Sets the maximum number of parallel requests to S3 per stream.
     *
     * The default value is 2.
     *
     * @see S3Connection#getParallelConnectionsPerRequest()
     */
    public S3ConnectionBuilder setParallelConnectionsPerRequest(int parallelConnectionsPerRequest) {
        this.parallelConnectionsPerRequest = parallelConnectionsPerRequest;
        return this;
    }

    /**
     * Sets the size of a single buffer used for streaming.
     *
     * The default value is {@link S3Connection#MINIMUM_PART_SIZE}.
     *
     * @see S3Connection#getBufferSize()
     */
    public S3ConnectionBuilder setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Sets whether data transferred to S3 will be encrypted on the server side.
     *
     * The default value is {@code true}.
     *
     * @see S3Connection#isServerSideEncrypted()
     */
    public S3ConnectionBuilder setServerSideEncryption(boolean serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
        return this;
    }

    /**
     * Returns a new {@link S3Connection} instance using the current properties of this builder.
     *
     * @throws NullPointerException if any required properties are {@code null}
     */
    public S3Connection build() {
        ClientConfiguration actualClientConfiguration = s3ClientConfiguration != null
            ? s3ClientConfiguration
            : new ClientConfiguration();

        return new S3ConnectionImpl(actualClientConfiguration, s3Client, executorService, parallelConnectionsPerRequest,
            bufferSize, serverSideEncryption);
    }
}
