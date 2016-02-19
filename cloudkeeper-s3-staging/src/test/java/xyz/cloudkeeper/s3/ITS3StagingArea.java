package xyz.cloudkeeper.s3;

import akka.dispatch.ExecutionContexts;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import xyz.cloudkeeper.contracts.RemoteStagingAreaContract;
import xyz.cloudkeeper.contracts.StagingAreaContract;
import xyz.cloudkeeper.contracts.StagingAreaContractProvider;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.s3.io.S3Connection;
import xyz.cloudkeeper.s3.io.S3ConnectionBuilder;

import javax.annotation.Nullable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tests the S3 staging area, by running all staging-area contract tests as well as implementation-specific tests.
 *
 * <p>This class requires the following parameters:
 * <ul><li>
 *     Amazon S3 credentials. This class makes use of {@link DefaultAWSCredentialsProviderChain} in order to retrieve
 *     the S3 credentials. See its documentation for where credentials are physically picked up.
 * </li><li>
 *     Amazon S3 bucket name. If there is a non-empty environment variable {@code CLOUDKEEPER_S3_TEST_BUCKET}, its value
 *     will be used. If not, and there is a non-empty system property {@code xyz.cloudkeeper.s3.testbucket}, its
 *     value will be used.
 * </li></ul>
 * If a parameter cannot be found, a {@link SkipException} exception will be thrown before any tests are run.
 *
 * <p>This test uses {@code (ITS3StagingArea.class.getName() + '/')} as prefix for all objects created in the S3 bucket.
 * Prior and after this test, all keys with this prefix are deleted.
 */
public class ITS3StagingArea {
    private static final String KEY_PREFIX = ITS3StagingArea.class.getName() + '/';

    private static final Duration AWAIT_DURATION = Duration.create(1, TimeUnit.MINUTES);

    private final InstanceProvider instanceProvider = new InstanceProviderImpl();
    @Nullable private AmazonClientException credentialsException = null;
    private boolean skipTest = true;
    @Nullable private S3Connection s3Connection = null;
    @Nullable private String s3Bucket;
    @Nullable private ScheduledExecutorService executorService;
    @Nullable private ExecutionContext executionContext;

    public void setup() {
        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        try {
            awsCredentialsProvider.getCredentials();

            s3Bucket = System.getenv("CLOUDKEEPER_S3_TEST_BUCKET");
            if (s3Bucket == null) {
                s3Bucket = System.getProperty("xyz.cloudkeeper.s3.testbucket");
            }

            if (s3Bucket != null) {
                executorService = Executors.newScheduledThreadPool(4);
                executionContext = ExecutionContexts.fromExecutorService(executorService);

                AmazonS3 s3Client = new AmazonS3Client(awsCredentialsProvider);
                s3Connection = new S3ConnectionBuilder(s3Client, executorService).build();
                skipTest = false;

                cleanS3(s3Connection, s3Bucket);
            }
        } catch (AmazonClientException exception) {
            credentialsException = exception;
        }
    }

    @AfterSuite
    public void tearDown() {
        if (!skipTest) {
            if (s3Connection != null) {
                assert s3Bucket != null && executorService != null : "must have been initialized in setup()";
                cleanS3(s3Connection, s3Bucket);
                executorService.shutdownNow();
            }
        }
    }

    private static void cleanS3(S3Connection s3Connection, String s3Bucket) {
        S3Utilities.deletePrefix(s3Connection.getS3Client(), s3Bucket, KEY_PREFIX);
    }

    @Factory
    public Object[] contractTests() {
        setup();

        ProviderImpl provider = new ProviderImpl();
        return new Object[] {
            new StagingAreaContract(provider),
            new RemoteStagingAreaContract(provider, instanceProvider)
        };
    }

    private class ProviderImpl implements StagingAreaContractProvider {
        @Override
        public void preContract() {
            if (credentialsException != null) {
                throw new SkipException(String.format(
                    "Could not find Amazon S3 credentials that should be used for %s.", ITS3StagingArea.class
                ), credentialsException);
            } else if (s3Bucket == null) {
                throw new SkipException(String.format(
                    "Could not find Amazon S3 bucket name that should be used for %s.", ITS3StagingArea.class
                ));
            }
        }

        @Override
        public StagingArea getStagingArea(String identifier, RuntimeContext runtimeContext,
                RuntimeAnnotatedExecutionTrace executionTrace) {
            assert s3Connection != null && s3Bucket != null && executionContext != null;
            return new S3StagingArea.Builder(executionTrace, s3Connection, s3Bucket, executionContext, runtimeContext)
                .setKeyPrefix(KEY_PREFIX)
                .build();
        }

        @Override
        public <T> T await(Future<T> future) throws Exception {
            return Await.result(future, AWAIT_DURATION);
        }
    }

    /**
     * This instance provider is a mock implementation that satisfies the requirements described in
     * {@link xyz.cloudkeeper.s3}.
     */
    private class InstanceProviderImpl implements InstanceProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getInstance(Class<T> requestedClass) {
            assert executionContext != null && s3Connection != null;
            if (ExecutionContext.class.equals(requestedClass)) {
                return (T) executionContext;
            } else if (S3Connection.class.equals(requestedClass)) {
                return (T) s3Connection;
            } else {
                Assert.fail(String.format("Instance provider ask for unexpected %s.", requestedClass));
                return null;
            }
        }
    }
}
