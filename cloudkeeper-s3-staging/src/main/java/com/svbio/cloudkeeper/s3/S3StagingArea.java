package com.svbio.cloudkeeper.s3;

import akka.japi.Option;
import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.types.ByteSequence;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider;
import com.svbio.cloudkeeper.model.api.staging.StagingException;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTraceVisitor;
import com.svbio.cloudkeeper.s3.io.S3Connection;
import com.svbio.cloudkeeper.staging.ExternalStagingArea;
import com.svbio.cloudkeeper.staging.MutableObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * S3-based staging area.
 */
public final class S3StagingArea extends ExternalStagingArea {
    /**
     * Buffer size used for redirecting an input stream to an output stream.
     *
     * <p>This is the same buffer size Open JDK uses within the {@code Files#copy()} methods.
     */
    private static final int BUFFER_SIZE = 8192;
    private static final String CONTENT_DIRECTORY = "content";
    private static final String INPUT_DIRECTORY = "input";
    private static final String OUTPUT_DIRECTORY = "output";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JAXBContext jaxbContext;
    private final S3Connection s3Connection;
    private final AmazonS3 s3Client;
    private final String bucketName;
    private final S3Path s3BaseKey;
    private final int maxStagingAreaPrefixLength;

    private S3StagingArea(RuntimeAnnotatedExecutionTrace executionTrace, RuntimeContext runtimeContext,
            ExecutionContext executionContext, JAXBContext jaxbContext, S3Connection s3Connection, AmazonS3 s3Client,
            String bucketName, S3Path s3BaseKey, int maxStagingAreaPrefixLength) {
        super(executionTrace, runtimeContext, executionContext);
        this.jaxbContext = jaxbContext;
        this.s3Connection = s3Connection;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.s3BaseKey = s3BaseKey;
        this.maxStagingAreaPrefixLength = maxStagingAreaPrefixLength;
    }

    private enum TraceElementVisitor implements RuntimeExecutionTraceVisitor<S3Path, S3Path> {
        INSTANCE;

        @Override
        public S3Path visitModule(RuntimeExecutionTrace module, @Nullable S3Path basePath) {
            assert basePath != null;
            return basePath.resolve(module.getSimpleName().toString());
        }

        @Override
        public S3Path visitContent(RuntimeExecutionTrace content, @Nullable S3Path basePath) {
            assert basePath != null;
            return basePath.resolve(CONTENT_DIRECTORY);
        }

        @Override
        public S3Path visitIteration(RuntimeExecutionTrace iteration, @Nullable S3Path basePath) {
            assert basePath != null;
            return basePath.resolve(iteration.getIndex().toString());
        }

        @Override
        public S3Path visitInPort(RuntimeExecutionTrace inPort, @Nullable S3Path basePath) {
            assert basePath != null;
            return basePath.resolve(INPUT_DIRECTORY).resolve(inPort.getSimpleName().toString());
        }

        @Override
        public S3Path visitOutPort(RuntimeExecutionTrace outPort, @Nullable S3Path basePath) {
            assert basePath != null;
            return basePath.resolve(OUTPUT_DIRECTORY).resolve(outPort.getSimpleName().toString());
        }

        @Override
        public S3Path visitArrayIndex(RuntimeExecutionTrace index, @Nullable S3Path basePath) {
            assert basePath != null;
            return basePath.resolve(index.getIndex().toString());
        }
    }

    /**
     * Returns the S3 key that corresponds to the given execution trace.
     *
     * @param s3BaseKey the base S3 key that the trace will be resolved against
     * @param trace execution trace, must be of type {@link RuntimeExecutionTrace.Type#IN_PORT},
     *     {@link RuntimeExecutionTrace.Type#OUT_PORT}, or {@link RuntimeExecutionTrace.Type#ARRAY_INDEX}
     * @return the path
     */
    private static S3Path toS3Path(S3Path s3BaseKey, RuntimeExecutionTrace trace) {
        @Nullable S3Path currentPath = s3BaseKey;
        for (RuntimeExecutionTrace element: trace.asElementList()) {
            currentPath = element.accept(TraceElementVisitor.INSTANCE, currentPath);
            assert currentPath != null;
        }
        return currentPath;
    }

    private S3Path toS3Path(RuntimeExecutionTrace trace) {
        return toS3Path(s3BaseKey, trace);
    }

    @Override
    public WriteContext newWriteContext(RuntimeExecutionTrace target) {
        return new WriteContextImpl(toS3Path(target));
    }

    @Override
    public ReadContext newReadContext(RuntimeExecutionTrace source) {
        return new ReadContextImpl(toS3Path(source));
    }

    @Override
    protected void delete(RuntimeExecutionTrace prefix, RuntimeAnnotatedExecutionTrace absolutePrefix)
            throws IOException {
        String keyPrefix = toS3Path(prefix).getPrefixForChildren();
        S3Utilities.deletePrefix(s3Connection.getS3Client(), bucketName, keyPrefix);
    }

    @Override
    protected void copy(RuntimeExecutionTrace source, RuntimeExecutionTrace target,
            RuntimeAnnotatedExecutionTrace absoluteSource, RuntimeAnnotatedExecutionTrace absoluteTarget)
            throws IOException {
        String sourcePrefix = toS3Path(source).getPrefixForChildren();
        String targetPrefix = toS3Path(target).getPrefixForChildren();
        for (S3ObjectSummary summary: S3Objects.withPrefix(s3Client, bucketName, sourcePrefix)) {
            if (summary.getKey().startsWith(sourcePrefix)) {
                String relativeKey = summary.getKey().substring(sourcePrefix.length());
                s3Client.copyObject(bucketName, summary.getKey(), bucketName, targetPrefix + relativeKey);
            } else {
                log.error(String.format(
                    "S3Objects.withPrefix() returned unexpected key '%s' when asked for prefix '%s'.",
                    summary.getKey(), sourcePrefix
                ));
            }
        }
    }

    @Override
    protected boolean exists(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource)
            throws IOException {
        ObjectListing listing = s3Client.listObjects(
            new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(toS3Path(source).getMetadataKey())
                .withMaxKeys(1)
        );
        return !listing.getCommonPrefixes().isEmpty() || !listing.getObjectSummaries().isEmpty();
    }

    @Override
    protected Option<Index> getMaximumIndex(RuntimeExecutionTrace trace, RuntimeAnnotatedExecutionTrace absoluteTrace,
            @Nullable Index upperBound) throws IOException {
        int upperBoundInt = upperBound == null
            ? Integer.MAX_VALUE
            : upperBound.intValue();
        int maximumIndex = -1;
        ObjectListing listing = s3Client.listObjects(
            new ListObjectsRequest()
                .withBucketName(bucketName)
                .withPrefix(toS3Path(trace).getPrefixForChildren())
                .withDelimiter(String.valueOf(S3Path.SEPARATOR))
        );
        do {
            for (String commonPrefix: listing.getCommonPrefixes()) {
                int index = Index.parseIndex(commonPrefix.substring(
                    commonPrefix.lastIndexOf(S3Path.SEPARATOR) + 1, commonPrefix.length() - 1
                ));
                if (index > maximumIndex && index <= upperBoundInt) {
                    maximumIndex = index;
                }
            }
            for (S3ObjectSummary summary: listing.getObjectSummaries()) {
                String key = summary.getKey();
                int index = Index.parseIndex(
                    key.substring(key.lastIndexOf(S3Path.SEPARATOR) + 1, key.length() - 1)
                );
                if (index > maximumIndex && index <= upperBoundInt) {
                    maximumIndex = index;
                }
            }
            if (maximumIndex == upperBoundInt) {
                // Optimization: If we found the upper bound, there will not be a greater index,
                // so we can stop here.
                break;
            }
            listing = s3Client.listNextBatchOfObjects(listing);
        } while (!listing.getCommonPrefixes().isEmpty() || !listing.getObjectSummaries().isEmpty());
        return maximumIndex >= 0
            ? Option.some(Index.index(maximumIndex))
            : Option.<Index>none();
    }

    @Override
    protected S3StagingArea resolveDescendant(RuntimeExecutionTrace trace,
            RuntimeAnnotatedExecutionTrace absoluteTrace) {
        return new S3StagingArea(absoluteTrace, getRuntimeContext(), getExecutionContext(),
            jaxbContext, s3Connection, s3Client, bucketName, toS3Path(trace), maxStagingAreaPrefixLength);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In order for the provider returned by {@link ExternalStagingArea#getStagingAreaProvider()} to be capable of
     * reconstructing an S3-based staging area in a separate JVM, the instance provider passed to
     * {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, com.svbio.cloudkeeper.model.api.staging.InstanceProvider)}
     * needs to be able to provide instances of the following classes:
     * <ul><li>
     *     {@link ExecutionContext}: The execution context will be used to execute the futures created by the staging
     *     area.
     * </li><li>
     *     {@link S3Connection}: An established connection to S3.
     * </li></ul>
     */
    @Override
    public StagingAreaProvider getStagingAreaProvider() {
        return new StagingAreaProviderImpl(bucketName, s3BaseKey.getPrefix(), maxStagingAreaPrefixLength);
    }

    private static final class S3ByteSequence implements ByteSequence {
        private final S3Connection s3Connection;
        private final URI uri;
        private final Object mutex = new Object();
        private volatile long contentLength = -1;

        private S3ByteSequence(S3Connection s3Connection, URI uri) {
            this.s3Connection = s3Connection;
            this.uri = uri;
        }

        @Override
        public ByteSequenceMarshaler.Decorator getDecorator() {
            return ByteSequenceMarshaler.noDecorator();
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public boolean isSelfContained() {
            return false;
        }

        /**
         * Returns the key corresponding to the given URI.
         *
         * <p>This method removes the slash that {@link URI#getPath()} starts with. This slash was previously added in
         * {@link #createS3Uri(S3Path)}.
         */
        private static String keyFromUri(URI uri) {
            assert uri.getPath().charAt(0) == S3Path.SEPARATOR;
            return uri.getPath().substring(1);
        }

        @Override
        public long getContentLength() throws IOException {
            long length = contentLength;
            if (length == -1) {
                synchronized (mutex) {
                    length = contentLength;
                    if (length == -1) {
                        try {
                            length = s3Connection.getS3Client()
                                .getObjectMetadata(uri.getAuthority(), keyFromUri(uri))
                                .getInstanceLength();
                        } catch (AmazonClientException exception) {
                            throw new S3StagingException(String.format(
                                "Failed to determine content length for '%s'.", uri
                            ), exception);
                        }
                        if (length < 0) {
                            throw new S3StagingException(String.format(
                                "Failed to determine content length for '%s', because Amazon S3 returned length %d.",
                                uri, length
                            ));
                        }
                        contentLength = length;
                    }
                }
            }
            return length;
        }

        @Override
        public String getContentType() throws IOException {
            return DEFAULT_CONTENT_TYPE;
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return s3Connection.newBufferedInputStream(uri.getAuthority(), keyFromUri(uri), 0);
        }
    }

    private URI createS3Uri(S3Path s3Path) {
        try {
            return new URI("s3", bucketName, S3Path.SEPARATOR + s3Path.toString(), null);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(String.format(
                "Failed to create URI 's3://%s/%s'. This should not happen.", bucketName, s3Path
            ), exception);
        }
    }

    private final class ReadContextImpl implements ReadContext {
        private final S3Path s3Path;

        private ReadContextImpl(S3Path s3Path) {
            this.s3Path = s3Path;
        }

        @Override
        public MutableObjectMetadata getMetadata() throws IOException {
            try (InputStream inputStream
                    = s3Connection.newBufferedInputStream(bucketName, s3Path.getMetadataKey(), 0)) {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                return (MutableObjectMetadata) unmarshaller.unmarshal(inputStream);
            } catch (JAXBException exception) {
                throw new StagingException(String.format(
                    "Failed to unmarshal object metadata from '%s'.", createS3Uri(s3Path)
                ), exception);
            }
        }

        @Override
        public ByteSequence getByteSequence(Key key) throws IOException {
            S3Path byteSequencePath = key instanceof NoKey
                ? s3Path.resolveDefaultKey()
                : s3Path.resolve(key.toString());
            return new S3ByteSequence(s3Connection, createS3Uri(byteSequencePath));
        }

        @Override
        public ReadContext resolve(Key key) throws IOException {
            return key instanceof NoKey
                ? this
                : new ReadContextImpl(s3Path.resolve(key.toString()));
        }
    }

    private final class WriteContextImpl implements WriteContext {
        private final S3Path s3Path;

        private WriteContextImpl(S3Path s3Path) {
            this.s3Path = s3Path;
        }

        private S3Path targetPath(Key key) {
            return key instanceof NoKey
                ? s3Path.resolveDefaultKey()
                : s3Path.resolve(key.toString());
        }

        @Override
        public OutputStream newOutputStream(Key key, @Nullable MutableObjectMetadata metadata) throws IOException {
            return s3Connection.newBufferedOutputStream(bucketName, targetPath(key).toString());
        }

        @Override
        public void putMetadata(MutableObjectMetadata metadata) throws IOException {
            try (
                OutputStream outputStream
                    = s3Connection.newBufferedOutputStream(bucketName, s3Path.getMetadataKey())
            ) {
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(metadata, outputStream);
            } catch (JAXBException exception) {
                throw new StagingException(String.format(
                    "Failed to marshal object metadata to '%s'.", createS3Uri(s3Path)
                ), exception);
            }
        }

        private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int bytesInBuffer = inputStream.read(buffer);
                if (bytesInBuffer <= 0) {
                    break;
                }
                outputStream.write(buffer, 0, bytesInBuffer);
            }
        }

        /**
         * Converts a URI to an {@link AmazonS3URI} instance, or returns {@code null} if parsing fails.
         *
         * <p>Unfortunately, the Amazon S3 API does not follow good engineering practices. The {@link AmazonS3URI}
         * constructor throws generic runtime exceptions, which makes it impossible to distinguish logical errors from
         * faulty user input (when passing user input to the constructor). Unfortunately, passing user input to the
         * constructor is without alternative, because the constructor performs multiple non-obvious operations (and
         * copying those operations into our own code is not a viable option).
         *
         * <p>We therefore catch all runtime exceptions when calling the {@link AmazonS3URI#AmazonS3URI(URI)}
         * constructor. However, we make sure the try-block does <em>not</em> contain any other statements, because
         * catching all runtime exceptions masks logical errors and is therefore bad practice.
         */
        private AmazonS3URI uriToAmazonS3Uri(@Nullable URI genericUri) {
            try {
                return genericUri == null
                    ? null
                    : new AmazonS3URI(genericUri);
            } catch (RuntimeException exception) {
                log.debug(String.format("Failed to parse %s as Amazon S3 URI.", genericUri), exception);
                return null;
            }
        }

        @Override
        public void putByteSequence(ByteSequence byteSequence, Key key, @Nullable MutableObjectMetadata metadata)
                throws IOException {
            @Nullable String byteSequenceBucket = null;
            @Nullable String byteSequenceKey = null;
            @Nullable URI byteSequenceURI = byteSequence.getURI();
            if (byteSequenceURI != null && "s3".equals(byteSequenceURI.getScheme())) {
                byteSequenceBucket = byteSequenceURI.getAuthority();
                byteSequenceKey = byteSequenceURI.getPath();
            }

            if (byteSequenceBucket == null || byteSequenceBucket.isEmpty() || byteSequenceKey == null
                    || byteSequenceKey.isEmpty()) {
                @Nullable AmazonS3URI amazonS3URI = uriToAmazonS3Uri(byteSequenceURI);
                if (amazonS3URI != null) {
                    byteSequenceBucket = amazonS3URI.getBucket();
                    byteSequenceKey = amazonS3URI.getKey();
                }
            }

            boolean wroteByteSequence = false;
            if (byteSequenceBucket != null && !byteSequenceBucket.isEmpty() && byteSequenceKey != null
                    && !byteSequenceKey.isEmpty()) {
                // Best case: If the byte sequence originates from S3, simply perform a S3 copy operation
                try {
                    s3Connection.getS3Client().copyObject(
                        byteSequenceBucket, byteSequenceKey, bucketName, targetPath(key).toString()
                    );
                    wroteByteSequence = true;
                } catch (AmazonClientException exception) {
                    log.info(String.format(
                        "Request for S3 server-side copy from 's3://%s/%s' to '%s' failed. This is not yet a "
                            + "problem, will attempt client-side copy instead.",
                        byteSequenceBucket, byteSequenceKey, createS3Uri(targetPath(key))
                    ), exception);
                }
            }

            if (!wroteByteSequence) {
                try (
                    InputStream inputStream = byteSequence.newInputStream();
                    OutputStream outputStream
                        = s3Connection.newBufferedOutputStream(bucketName, targetPath(key).toString())
                ) {
                    copy(inputStream, outputStream);
                }
            }
        }

        @Override
        public WriteContext resolve(Key key) throws IOException {
            return key instanceof NoKey
                ? this
                : new WriteContextImpl(s3Path.resolve(key.toString()));
        }
    }

    /**
     * This class is used to create S3-based staging areas.
     */
    public static class Builder {
        private static final int DEFAULT_MAX_STAGING_AREA_PREFIX_LENGTH = 256;

        private final RuntimeAnnotatedExecutionTrace absoluteTrace;
        private final S3Connection s3Connection;
        private final String s3Bucket;
        private final RuntimeContext runtimeContext;
        private final ExecutionContext executionContext;
        private String keyPrefix = "";
        private int maxStagingAreaPrefixLength = DEFAULT_MAX_STAGING_AREA_PREFIX_LENGTH;

        /**
         * Constructs a builder with the specified arguments.
         *
         * @param absoluteTrace absolute execution trace that will correspond to the base path of this staging area
         * @param s3Connection connection to Amazon S3
         * @param s3Bucket S3 bucket for the new staging area
         * @param executionContext execution context that file-system tasks will be submitted to
         * @param runtimeContext runtime context consisting of CloudKeeper plug-in declarations and Java class loader,
         *     both needed during deserialization
         */
        public Builder(RuntimeAnnotatedExecutionTrace absoluteTrace, S3Connection s3Connection,
                String s3Bucket, ExecutionContext executionContext, RuntimeContext runtimeContext) {
            this.absoluteTrace = Objects.requireNonNull(absoluteTrace);
            this.s3Connection = Objects.requireNonNull(s3Connection);
            this.s3Bucket = Objects.requireNonNull(s3Bucket);
            this.executionContext = Objects.requireNonNull(executionContext);
            this.runtimeContext = Objects.requireNonNull(runtimeContext);
        }

        /**
         * Sets this builder's key prefix.
         *
         * <p>By default, the key prefix is the empty string. If the given string is non-empty, it is advisable (but not
         * enforced) that the given prefix ends with a slash (/).
         *
         * @param keyPrefix prefix for all S3 key created by the staging area, must not be null
         * @return this builder
         * @throws NullPointerException if the argument is null
         */
        public Builder setKeyPrefix(String keyPrefix) {
            this.keyPrefix = Objects.requireNonNull(keyPrefix);
            return this;
        }

        /**
         * Sets this builder's property that controls the maximum staging-area prefix length.
         *
         * <p>By default, this value is 256.
         *
         * @param maxStagingAreaPrefixLength the maximum staging-area prefix length
         * @return this builder
         * @see com.svbio.cloudkeeper.s3
         */
        public Builder setMaxStagingAreaPrefixLength(int maxStagingAreaPrefixLength) {
            this.maxStagingAreaPrefixLength = maxStagingAreaPrefixLength;
            return this;
        }

        private static JAXBContext jaxbContext() {
            try {
                return JAXBContext.newInstance(MutableObjectMetadata.class);
            } catch (JAXBException exception) {
                throw new IllegalStateException(
                    "Exception while constructing JAXB context. This should not happen.", exception);
            }
        }

        /**
         * Creates and returns a new file-based staging area using the attributes of this builder.
         *
         * @return the new staging area
         */
        public ExternalStagingArea build() {
            return new S3StagingArea(absoluteTrace, runtimeContext, executionContext, jaxbContext(), s3Connection,
                s3Connection.getS3Client(), s3Bucket, toS3Path(S3Path.empty(keyPrefix), absoluteTrace),
                maxStagingAreaPrefixLength);
        }
    }
}
