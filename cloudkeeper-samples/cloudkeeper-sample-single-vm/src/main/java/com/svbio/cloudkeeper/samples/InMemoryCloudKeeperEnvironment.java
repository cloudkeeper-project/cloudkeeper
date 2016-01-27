package com.svbio.cloudkeeper.samples;

import com.svbio.cloudkeeper.model.api.CloudKeeperEnvironment;
import com.svbio.cloudkeeper.model.api.WorkflowExecutionBuilder;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.simple.SingleVMCloudKeeper;
import com.svbio.cloudkeeper.staging.MapStagingArea;

/**
 * CloudKeeper environment uses {@link com.svbio.cloudkeeper.simple.SingleVMCloudKeeper} with the default in-memory
 * staging area (see {@link MapStagingArea}).
 *
 * <p>This class implements {@link java.io.Closeable}, so that this CloudKeeper environment can be used within in a
 * try-with-resources statement. This is not necessarily realistic in real-world projects, where a CloudKeeper
 * environment is typically longer-lived than just a single block.
 */
public final class InMemoryCloudKeeperEnvironment implements CloudKeeperEnvironment, AutoCloseable {
    private final SingleVMCloudKeeper cloudKeeper;
    private final CloudKeeperEnvironment delegate;

    public InMemoryCloudKeeperEnvironment() {
        cloudKeeper = new SingleVMCloudKeeper.Builder().build();
        delegate = cloudKeeper.newCloudKeeperEnvironmentBuilder().build();
    }

    @Override
    public WorkflowExecutionBuilder newWorkflowExecutionBuilder(BareModule module) {
        return delegate.newWorkflowExecutionBuilder(module);
    }

    @Override
    public void close() {
        cloudKeeper.shutdown();
    }
}
