package com.svbio.cloudkeeper.samples;

import akka.dispatch.ExecutionContexts;
import com.svbio.cloudkeeper.filesystem.FileStagingArea;
import com.svbio.cloudkeeper.model.api.CloudKeeperEnvironment;
import com.svbio.cloudkeeper.model.api.WorkflowExecutionBuilder;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvisionException;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.simple.SingleVMCloudKeeper;
import scala.concurrent.ExecutionContext;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CloudKeeper environment uses {@link SingleVMCloudKeeper} together with a file-based staging area (see
 * {@link com.svbio.cloudkeeper.filesystem}).
 *
 * <p>This class implements {@link Closeable}, so that this CloudKeeper environment can be used within in a
 * try-with-resources statement. This is not necessarily realistic in real-world projects, where a CloudKeeper
 * environment is typically longer-lived than just a single block.
 */
public final class FileBasedCloudKeeperEnvironment implements CloudKeeperEnvironment, Closeable {
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final SingleVMCloudKeeper cloudKeeper;
    private final Path temporaryDirectory;
    private final CloudKeeperEnvironment delegate;

    public FileBasedCloudKeeperEnvironment() throws IOException {
        final ExecutionContext executionContext = ExecutionContexts.fromExecutorService(executorService);

        temporaryDirectory = Files.createTempDirectory(getClass().getSimpleName());
        cloudKeeper = new SingleVMCloudKeeper.Builder()
            .setWorkspaceBasePath(temporaryDirectory)
            .build();
        delegate = cloudKeeper.newCloudKeeperEnvironmentBuilder()
            .setCleaningRequested(false)
            .setStagingAreaProvider((runtimeContext, executionTrace, instanceProvider) -> {
                Path basePath;
                try {
                    basePath = Files.createTempDirectory(temporaryDirectory, "StagingArea");
                    return new FileStagingArea.Builder(runtimeContext, executionTrace, basePath, executionContext)
                            .build();
                } catch (IOException exception) {
                    throw new InstanceProvisionException(
                        "Failed to provide file-based staging area because temporary directory could not be created.",
                        exception
                    );
                }
            })
            .build();
    }

    @Override
    public void close() throws IOException {
        cloudKeeper.shutdown();
        executorService.shutdown();
        Files.walkFileTree(temporaryDirectory, RecursiveDeleteVisitor.getInstance());
    }

    public Path getTemporaryDirectory() {
        return temporaryDirectory;
    }

    @Override
    public WorkflowExecutionBuilder newWorkflowExecutionBuilder(BareModule module) {
        return delegate.newWorkflowExecutionBuilder(module);
    }
}
