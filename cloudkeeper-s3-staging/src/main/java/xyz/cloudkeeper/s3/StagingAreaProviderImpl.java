package xyz.cloudkeeper.s3;

import scala.concurrent.ExecutionContext;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.InstanceProvisionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.staging.StagingAreaProvider;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.s3.io.S3Connection;

import java.util.Objects;

final class StagingAreaProviderImpl implements StagingAreaProvider {
    private static final long serialVersionUID = -4770208081578912789L;

    private final String s3Bucket;
    private final String s3KeyPrefix;
    private final int maxStagingAreaPrefixLength;

    StagingAreaProviderImpl(String s3Bucket, String s3KeyPrefix, int maxStagingAreaPrefixLength) {
        this.s3Bucket = Objects.requireNonNull(s3Bucket);
        this.s3KeyPrefix = Objects.requireNonNull(s3KeyPrefix);
        this.maxStagingAreaPrefixLength = maxStagingAreaPrefixLength;
    }

    @Override
    public StagingArea provideStaging(RuntimeContext runtimeContext, RuntimeAnnotatedExecutionTrace executionTrace,
            InstanceProvider instanceProvider) throws InstanceProvisionException {
        ExecutionContext executionContext = instanceProvider.getInstance(ExecutionContext.class);
        S3Connection s3Connection = instanceProvider.getInstance(S3Connection.class);
        return new S3StagingArea.Builder(executionTrace, s3Connection, s3Bucket, executionContext, runtimeContext)
            .setKeyPrefix(s3KeyPrefix)
            .setMaxStagingAreaPrefixLength(maxStagingAreaPrefixLength)
            .build();
    }
}
