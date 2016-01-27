package com.svbio.cloudkeeper.simple;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.svbio.cloudkeeper.interpreter.AdministratorActorCreator;
import com.svbio.cloudkeeper.interpreter.CloudKeeperEnvironmentBuilder;
import com.svbio.cloudkeeper.interpreter.ExecutorActorCreator;
import com.svbio.cloudkeeper.interpreter.InstanceProviderActorCreator;
import com.svbio.cloudkeeper.interpreter.MasterInterpreterActorCreator;
import com.svbio.cloudkeeper.model.api.CloudKeeperEnvironment;
import com.svbio.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.staging.MapStagingArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SingleVMCloudKeeper {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ADMINISTRATOR_NAME = "administrator";
    private static final String MASTER_INTERPRETER_NAME = "master-interpreter";
    private static final String EXECUTOR_NAME = "executor";
    private static final String INSTANCE_PROVIDER_NAME = "instance-provider";
    private static final String INSTANCE_PROVIDER_PATH = "/user/" + INSTANCE_PROVIDER_NAME;

    private final boolean ownsActorSystem;
    private final ActorSystem actorSystem;
    private final AtomicBoolean isShutDown = new AtomicBoolean(false);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private final ActorRef administrator;
    private final ActorRef masterInterpreter;
    private final ActorRef executor;

    private final InstanceProvider instanceProvider;
    private final boolean ownsWorkspaceBasePath;
    private final Path workspaceBasePath;

    private SingleVMCloudKeeper(
        boolean ownsActorSystem,
        ActorSystem actorSystem,
        ActorRef administrator,
        ActorRef masterInterpreter,
        ActorRef executor,
        InstanceProvider instanceProvider,
        boolean ownsWorkspaceBasePath,
        Path workspaceBasePath
    ) {
        this.ownsActorSystem = ownsActorSystem;
        this.actorSystem = actorSystem;
        this.administrator = administrator;
        this.masterInterpreter = masterInterpreter;
        this.executor = executor;
        this.instanceProvider = instanceProvider;
        this.ownsWorkspaceBasePath = ownsWorkspaceBasePath;
        this.workspaceBasePath = workspaceBasePath;
    }

    public static final class Builder {
        private ActorSystem actorSystem;
        private Path workspaceBasePath = null;
        private InstanceProvider instanceProvider = null;
        private long firstExecutionId = 0;

        public Builder setWorkspaceBasePath(Path workspaceBasePath) {
            this.workspaceBasePath = Objects.requireNonNull(workspaceBasePath);
            return this;
        }

        public Builder setActorSystem(ActorSystem actorSystem) {
            this.actorSystem = Objects.requireNonNull(actorSystem);
            return this;
        }

        public Builder setInstanceProvider(InstanceProvider instanceProvider) {
            this.instanceProvider = Objects.requireNonNull(instanceProvider);
            return this;
        }

        public Builder setFirstExecutionId(long firstExecutionId) {
            this.firstExecutionId = firstExecutionId;
            return this;
        }

        /**
         * Returns a new {@link SingleVMCloudKeeper} instance using the attributes of this builder.
         *
         * @return the new {@link SingleVMCloudKeeper} instance
         * @throws BuilderException if construction fails
         */
        public SingleVMCloudKeeper build() {
            boolean success = false;
            ActorSystem actualActorSystem = actorSystem;
            try {
                boolean ownsWorkspaceBasePath = workspaceBasePath == null;
                Path actualWorkspaceBasePath = ownsWorkspaceBasePath
                    ? Files.createTempDirectory(SingleVMCloudKeeper.class.getSimpleName())
                    : workspaceBasePath;

                boolean ownsActorSystem = actorSystem == null;
                actualActorSystem = ownsActorSystem
                    ? ActorSystem.create()
                    : actorSystem;
                ExecutionContext executionContext = actualActorSystem.dispatcher();

                ActorRef administrator = actualActorSystem.actorOf(
                    Props.create(AdministratorActorCreator.getInstance()), ADMINISTRATOR_NAME);
                ActorRef masterInterpreter = actualActorSystem.actorOf(
                    Props.create(new MasterInterpreterActorCreator(firstExecutionId)), MASTER_INTERPRETER_NAME);

                ModuleConnectorProvider moduleConnectorProvider
                    = new PrefetchingModuleConnectorProvider(actualWorkspaceBasePath, executionContext);
                SimpleModuleExecutor simpleModuleExecutor
                    = new LocalSimpleModuleExecutor.Builder(executionContext, moduleConnectorProvider).build();
                ActorRef executor = actualActorSystem.actorOf(
                    Props.create(new ExecutorActorCreator(simpleModuleExecutor)), EXECUTOR_NAME);

                InstanceProvider actualInstanceProvider = instanceProvider;
                if (actualInstanceProvider == null) {
                    actualInstanceProvider = new SimpleInstanceProvider.Builder(executionContext).build();
                }

                actualActorSystem.actorOf(
                    Props.create(new InstanceProviderActorCreator(actualInstanceProvider)),
                    INSTANCE_PROVIDER_NAME
                );

                SingleVMCloudKeeper cloudKeeper = new SingleVMCloudKeeper(ownsActorSystem, actualActorSystem,
                    administrator, masterInterpreter, executor, actualInstanceProvider, ownsWorkspaceBasePath,
                    actualWorkspaceBasePath);
                success = true;
                return cloudKeeper;
            } catch (IOException|BuilderException exception) {
                throw new BuilderException(SingleVMCloudKeeper.class.toString(), exception);
            } finally {
                if (!success) {
                    if (actualActorSystem != null) {
                        actualActorSystem.terminate();
                    }
                }
            }
        }
    }

    @Override
    protected void finalize() {
        shutdown();
    }

    private final class CleanUpThread implements Runnable {
        @Override
        public void run() {
            if (ownsActorSystem) {
                actorSystem.shutdown();
                actorSystem.awaitTermination();
            }
            if (ownsWorkspaceBasePath) {
                try {
                    Files.walkFileTree(workspaceBasePath, RecursiveDeleteVisitor.getInstance());
                } catch (IOException exception) {
                    log.warn(String.format(
                        "Failed to delete workspace base path at %s.", workspaceBasePath
                    ), exception);
                }
            }
            shutdownLatch.countDown();
        }
    }

    /**
     * Stops this CloudKeeper system asynchronously.
     *
     * <p>If a synchronous shutdown is needed, {@link #awaitTermination()} should be called subsequent to this method.
     */
    public SingleVMCloudKeeper shutdown() {
        if (isShutDown.compareAndSet(false, true)) {
            new Thread(new CleanUpThread()).start();
        }
        return this;
    }

    /**
     * Blocks the current thread until the CloudKeeper system has been shutdown.
     *
     * <p>This method also blocks if {@link #shutdown()} has <em>not</em> been called, yet.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        shutdownLatch.await();
    }

    public final class EnvironmentBuilder {
        private StagingAreaProvider stagingAreaProvider
            = (runtimeContext, executionTrace, ignored) -> new MapStagingArea(runtimeContext, executionTrace);
        private boolean cleaningRequested = true;

        private EnvironmentBuilder() { }

        public EnvironmentBuilder setCleaningRequested(boolean cleaningRequested) {
            this.cleaningRequested = cleaningRequested;
            return this;
        }

        public EnvironmentBuilder setStagingAreaProvider(StagingAreaProvider stagingAreaProvider) {
            this.stagingAreaProvider = stagingAreaProvider;
            return this;
        }

        /**
         * Returns a new {@link CloudKeeperEnvironment} instance using the attributes of this builder.
         *
         * @return the new {@link SingleVMCloudKeeper} instance
         * @throws BuilderException if construction fails
         */
        public CloudKeeperEnvironment build() {
            CloudKeeperEnvironmentBuilder builder = new CloudKeeperEnvironmentBuilder(
                actorSystem.dispatcher(), administrator, masterInterpreter, executor, instanceProvider,
                stagingAreaProvider
            );
            builder
                .setCleaningRequested(cleaningRequested)
                .setInstanceProviderActorPath(INSTANCE_PROVIDER_PATH);
            return builder.build();
        }
    }

    public EnvironmentBuilder newCloudKeeperEnvironmentBuilder() {
        return new EnvironmentBuilder();
    }
}
