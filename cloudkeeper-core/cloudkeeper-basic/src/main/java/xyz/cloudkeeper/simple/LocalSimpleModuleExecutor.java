package xyz.cloudkeeper.simple;

import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.Executable;
import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.ExtendedModuleConnector;
import xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.InstanceProvisionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.staging.StagingAreaProvider;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class LocalSimpleModuleExecutor implements SimpleModuleExecutor {
    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when {@link Executable#run(xyz.cloudkeeper.model.api.ModuleConnector)}
     * was called.
     *
     * <p>If the {@link xyz.cloudkeeper.model.api.ModuleConnector} used for the execution (or its
     * {@link ModuleConnectorProvider}) prefetch all inputs, the difference to
     * property {@link SimpleModuleExecutor#SUBMISSION_TIME_MILLIS} is an upper
     * bound on the time it took to transmit all inputs.
     */
    public static final SimpleName PROCESSING_START_TIME_MILLIS = SimpleName.identifier("processingStartTimeMillis");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when the call to
     * {@link Executable#run(xyz.cloudkeeper.model.api.ModuleConnector)} returned.
     *
     * <p>If the {@link xyz.cloudkeeper.model.api.ModuleConnector} used for the execution only starts transferring
     * outputs during {@link ExtendedModuleConnector#commit()}, the difference
     * to property {@link SimpleModuleExecutor#COMPLETION_TIME_MILLIS} is a
     * upper bound on the time it took to transmit all outputs.
     */
    public static final SimpleName PROCESSING_FINISH_TIME_MILLIS = SimpleName.identifier("processingFinishTimeMillis");

    private final Executor executor;
    private final InstanceProvider instanceProvider;
    private final ModuleConnectorProvider moduleConnectorProvider;

    private LocalSimpleModuleExecutor(Executor executor, ModuleConnectorProvider moduleConnectorProvider,
            InstanceProvider instanceProvider) {
        this.executor = executor;
        this.instanceProvider = instanceProvider;
        this.moduleConnectorProvider = moduleConnectorProvider;
    }

    /**
     * This class is used to create {@link LocalSimpleModuleExecutor} instances.
     *
     * <p>None of this builder's methods accepts null parameters, and a {@link NullPointerException} is thrown if null
     * is passed.
     */
    public static class Builder {
        private final Executor executor;
        private final ModuleConnectorProvider moduleConnectorProvider;
        @Nullable private InstanceProvider instanceProvider;

        /**
         * Constructs a builder with the specified execution context and module-connector provider.
         *
         * <p>Executing a module may trigger other asynchronous operations as
         * {@link xyz.cloudkeeper.model.api.ModuleConnector} methods are called. It is therefore important that
         * either the given execution context has at least multiple threads available, or the module connector must use
         * a different execution context. Otherwise, a deadlock could occur when one task is waiting for the result of
         * another task, which however cannot be scheduled because the maximum thread-pool size is reached.
         *
         * @param executor the execution context that will be used for executing module (as well as internal
         *     tasks created to complete internal futures)
         * @param moduleConnectorProvider the module-connector provider whose
         *     {@link ModuleConnectorProvider#provideModuleConnector(StagingArea)} method will be called in
         *     order to build a {@link ExtendedModuleConnector} instance around a staging area
         */
        public Builder(Executor executor, ModuleConnectorProvider moduleConnectorProvider) {
            this.executor = Objects.requireNonNull(executor);
            this.moduleConnectorProvider = Objects.requireNonNull(moduleConnectorProvider);
        }

        /**
         * Sets this builder's instance provider.
         *
         * <p>The instance provider will be passed to
         * {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, InstanceProvider)},
         * which will be called from {@link #submit(RuntimeStateProvider)}.
         *
         * <p>By default, if this method is not called, the instance provider will be a new
         * {@link SimpleInstanceProvider} that provides the execution context passed to
         * {@link #Builder(Executor, ModuleConnectorProvider)} and a new
         * {@link DSLRuntimeContextFactory} (which itself also uses the same execution context).
         *
         * @param instanceProvider the instance provider, may be {@code null} to set to the default
         * @return this builder
         */
        public Builder setInstanceProvider(InstanceProvider instanceProvider) {
            this.instanceProvider = instanceProvider;
            return this;
        }

        public LocalSimpleModuleExecutor build() {
            @Nullable InstanceProvider actualInstanceProvider = instanceProvider;
            if (actualInstanceProvider == null) {
                actualInstanceProvider = new SimpleInstanceProvider.Builder(executor).build();
            }
            return new LocalSimpleModuleExecutor(executor, moduleConnectorProvider, actualInstanceProvider);
        }
    }

    /**
     * Intermediate result consisting of optional exception and all time stamps, except for the completion time stamp.
     *
     * <p>The completion time stamp is available only after the future returned by
     * {@link ExtendedModuleConnector#commit()} has been completed.
     */
    private static final class IntermediateResults {
        private final long submissionTimeMillis = System.currentTimeMillis();
        private volatile long processingStartTimeMillis = 0;
        private volatile long processingFinishTimeMillis = 0;
    }

    private static Void runExecutable(IntermediateResults intermediateResults, Executable executable,
            ExtendedModuleConnector moduleConnector) throws ExecutionException {
        try {
            intermediateResults.processingStartTimeMillis = System.currentTimeMillis();
            executable.run(moduleConnector);
        } finally {
            intermediateResults.processingFinishTimeMillis = System.currentTimeMillis();
        }
        return null;
    }

    private CompletionStage<Void> createModuleConnectorAndRun(RuntimeStateProvider runtimeStateProvider,
            RuntimeContext runtimeContext, IntermediateResults intermediateResults)
            throws LinkerException, InstanceProvisionException {
        StagingArea stagingArea;
        stagingArea = runtimeStateProvider.provideStagingArea(runtimeContext, instanceProvider);
        return Futures.thenComposeWithResource(
            moduleConnectorProvider.provideModuleConnector(stagingArea),
            moduleConnector -> {
                RuntimeProxyModule simpleModule = (RuntimeProxyModule) moduleConnector.getExecutionTrace().getModule();
                Executable executable = ((RuntimeSimpleModuleDeclaration) simpleModule.getDeclaration()).toExecutable();
                return Futures
                    .supplyAsync(() -> runExecutable(intermediateResults, executable, moduleConnector), executor)
                    .thenCompose(ignore -> moduleConnector.commit());
            }
        );
    }

    /**
     * @return Future representing pending completion of the task. If the simple module is executed successfully, the
     *     future will be completed with a {@link SimpleModuleExecutorResult}, otherwise with an
     *     {@link ExecutionException} (unless the {@link Throwable} is not an exception).
     */
    @Override
    public CompletableFuture<SimpleModuleExecutorResult> submit(RuntimeStateProvider runtimeStateProvider) {
        IntermediateResults intermediateResults = new IntermediateResults();
        CompletableFuture<Void> runModuleStage = Futures.thenComposeWithResource(
            runtimeStateProvider.provideRuntimeContext(instanceProvider),
            runtimeContext -> createModuleConnectorAndRun(runtimeStateProvider, runtimeContext, intermediateResults)
        );
        CompletableFuture<SimpleModuleExecutorResult> buildResultStage
            = runModuleStage.thenApply(ignored -> resultBuilder(intermediateResults).build());
        return buildResultStage.exceptionally(
            throwable
                -> failedResult(intermediateResults, runtimeStateProvider, Futures.unwrapCompletionException(throwable))
        );
    }

    private static SimpleModuleExecutorResult.Builder resultBuilder(IntermediateResults intermediateResults) {
        SimpleModuleExecutorResult.Builder resultBuilder
            = new SimpleModuleExecutorResult.Builder(Name.qualifiedName(LocalSimpleModuleExecutor.class.getName()))
                .addProperty(SUBMISSION_TIME_MILLIS, intermediateResults.submissionTimeMillis);
        long processingStartTimeMillis = intermediateResults.processingStartTimeMillis;
        if (processingStartTimeMillis != 0) {
            resultBuilder.addProperty(PROCESSING_START_TIME_MILLIS, processingStartTimeMillis);
        }
        long processingFinishTimeMillis = intermediateResults.processingFinishTimeMillis;
        if (processingFinishTimeMillis != 0) {
            resultBuilder.addProperty(PROCESSING_FINISH_TIME_MILLIS, processingFinishTimeMillis);
        }
        resultBuilder.addProperty(COMPLETION_TIME_MILLIS, System.currentTimeMillis());
        return resultBuilder;
    }

    private static SimpleModuleExecutorResult failedResult(IntermediateResults intermediateResults,
            RuntimeStateProvider runtimeStateProvider, Throwable throwable) {
        ExecutionException exception = throwable instanceof ExecutionException
            ? (ExecutionException) throwable
            : new ExecutionException(String.format(
                "Simple-module execution failed for execution trace '%s'.", runtimeStateProvider.getExecutionTrace()
            ), throwable);
        return resultBuilder(intermediateResults)
            .setException(exception)
            .build();
    }
}
