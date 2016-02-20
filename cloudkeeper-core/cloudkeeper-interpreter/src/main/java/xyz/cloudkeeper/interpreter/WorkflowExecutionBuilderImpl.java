package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.dispatch.ExecutionContexts;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.ExecutionContext;
import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeContextFactory;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.WorkflowExecution;
import xyz.cloudkeeper.model.api.WorkflowExecutionBuilder;
import xyz.cloudkeeper.model.api.WorkflowExecutionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.beans.element.module.MutableModule;
import xyz.cloudkeeper.model.beans.execution.MutableOverride;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimePort;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Implementation of workflow-execution builder.
 */
final class WorkflowExecutionBuilderImpl implements WorkflowExecutionBuilder {
    private final CloudKeeperEnvironmentImpl cloudKeeperEnvironment;
    private final BareModule module;
    private ImmutableList<URI> bundleIdentifiers = ImmutableList.of();
    private List<BareOverride> overrides = Collections.emptyList();
    private Map<SimpleName, Object> inputValues = Collections.emptyMap();

    WorkflowExecutionBuilderImpl(CloudKeeperEnvironmentImpl cloudKeeperEnvironment, BareModule module) {
        Objects.requireNonNull(cloudKeeperEnvironment);
        Objects.requireNonNull(module);

        this.cloudKeeperEnvironment = cloudKeeperEnvironment;
        this.module = module instanceof Immutable
            ? module
            : MutableModule.copyOfModule(module);
    }

    @Override
    public WorkflowExecutionBuilderImpl setBundleIdentifiers(List<URI> bundleIdentifiers) {
        Objects.requireNonNull(bundleIdentifiers);

        this.bundleIdentifiers = ImmutableList.copyOf(bundleIdentifiers);
        return this;
    }

    @Override
    public WorkflowExecutionBuilderImpl setOverrides(List<? extends BareOverride> overrides) {
        Objects.requireNonNull(overrides);

        this.overrides = new ArrayList<>(overrides.size());
        for (BareOverride override: overrides) {
            Objects.requireNonNull(override, "null element not permitted.");
            this.overrides.add(override instanceof Immutable
                ? override
                : MutableOverride.copyOf(override)
            );
        }
        return this;
    }

    @Override
    public WorkflowExecutionBuilderImpl setInputs(Map<SimpleName, Object> inputValues) {
        Objects.requireNonNull(inputValues);
        for (Map.Entry<SimpleName, Object> entry: inputValues.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "null key not permitted.");
            Objects.requireNonNull(entry.getValue(), "null value not permitted.");
        }

        this.inputValues = new LinkedHashMap<>(inputValues.size());
        this.inputValues.putAll(inputValues);
        return this;
    }


    private static final Mapper<Object, Long> TO_LONG_MAPPER = new Mapper<Object, Long>() {
        @Override
        public Long apply(Object parameter) {
            return (Long) parameter;
        }
    };

    private static final class Context {
        private final long startTimeMillis = System.currentTimeMillis();
        private final CloudKeeperEnvironmentImpl cloudKeeperEnvironment;
        private final Executor executor;
        private final ExecutionContext executionContext;
        private final BareModule module;
        private final List<URI> bundleIdentifiers;
        private final List<BareOverride> overrides;
        private final Map<SimpleName, Object> inputValues;

        private final CompletableFuture<StagingArea> stagingAreaPromise = new CompletableFuture<>();
        private final CompletableFuture<ImmutableList<CompletableFuture<Object>>> outPortFuturesPromise
            = new CompletableFuture<>();
        private final CompletableFuture<Long> executionIdPromise = new CompletableFuture<>();
        private final CompletableFuture<Long> finishTimeMillisPromise = new CompletableFuture<>();
        private final CompletableFuture<Optional<WorkflowExecutionException>> executionExceptionPromise
            = new CompletableFuture<>();
        private final CompletableFuture<Void> cancellationPromise = new CompletableFuture<>();

        private Context(CloudKeeperEnvironmentImpl environment, BareModule module,
                List<URI> bundleIdentifiers, List<BareOverride> overrides,
                Map<SimpleName, Object> inputValues) {
            assert environment != null && module != null && bundleIdentifiers != null && overrides != null
                && inputValues != null;

            cloudKeeperEnvironment = environment;
            executor = environment.getRunnableExecutor();
            executionContext = ExecutionContexts.fromExecutor(executor);
            this.module = module;
            this.bundleIdentifiers = bundleIdentifiers;
            this.overrides = overrides;
            this.inputValues = inputValues;
        }

        /**
         * Writes the inputs to the staging area and send a create-execution message to the master interpreter. This
         * message will only return a new execution ID, but the execution will not yet be started.
         */
        private CompletableFuture<Long> writeInputsAndCreateExecution(RuntimeContext runtimeContext,
                StagingArea stagingArea) throws WorkflowExecutionException {
            RuntimeModule runtimeModule = stagingArea.getAnnotatedExecutionTrace().getModule();

            // All ports for which inputs were explicitly set are implicitly marked as updated!
            BitSet updatedInPorts = new BitSet();

            // First verify that all input identifiers are indeed in-ports of the module
            for (SimpleName simpleName : inputValues.keySet()) {
                @Nullable RuntimePort port = runtimeModule.getEnclosedElement(RuntimePort.class, simpleName);
                if (!(port instanceof RuntimeInPort)) {
                    throw new WorkflowExecutionException(String.format(
                        "In-port with name '%s' does not exist in %s.",
                        simpleName, runtimeModule
                    ));
                }
                updatedInPorts.set(((RuntimeInPort) port).getInIndex());
            }

            // Now go ahead and write the inputs
            List<CompletableFuture<Void>> inputFutures = new ArrayList<>(inputValues.size());
            for (Map.Entry<SimpleName, Object> entry: inputValues.entrySet()) {
                @Nullable Object value = entry.getValue();
                if (value != null) {
                    // If value is null, the staging area must already contain a value. Otherwise, an exception will be
                    // raised by the executor.
                    inputFutures.add(
                        stagingArea.putObject(ExecutionTrace.empty().resolveInPort(entry.getKey()), value)
                    );
                }
            }

            InterpreterProperties executionProperties = new InterpreterProperties(
                cloudKeeperEnvironment.isCleaningRequested(), cloudKeeperEnvironment.getAdministrator(),
                cloudKeeperEnvironment.getExecutor(), cloudKeeperEnvironment.getEventSubscriptions());
            final MasterInterpreterActorInterface.CreateExecution message
                = new MasterInterpreterActorInterface.CreateExecution(
                    cloudKeeperEnvironment.getInstanceProviderActorPath(),
                    RuntimeStateProvider.of(runtimeContext, stagingArea),
                    cloudKeeperEnvironment.getInterpreterPropsProvider(), executionProperties, updatedInPorts
                );

            // Once all inputs have been written to the staging area, we will send the create-execution message to the
            // master interpreter
            return CompletableFuture.allOf(inputFutures.toArray(new CompletableFuture<?>[inputFutures.size()]))
                .thenCompose(ignored -> {
                    ActorRef masterInterpreter = cloudKeeperEnvironment.getMasterInterpreter();
                    Timeout remoteAskTimeout = cloudKeeperEnvironment.getRemoteAskTimeout();
                    return ScalaFutures.completableFutureOf(
                        Patterns
                            .ask(masterInterpreter, message, remoteAskTimeout)
                            .map(TO_LONG_MAPPER, executionContext),
                        executionContext
                    );
                });
        }

        /**
         *
         */
        private CompletableFuture<Long> createOutputFuturesAndStartExecution(RuntimeContext runtimeContext,
                StagingArea stagingArea) throws WorkflowExecutionException {
            final RuntimeModule runtimeModule = stagingArea.getAnnotatedExecutionTrace().getModule();
            int numOutPorts = runtimeModule.getOutPorts().size();
            List<CompletableFuture<Object>> outPortPromises = new ArrayList<>(numOutPorts);
            for (int i = 0; i < numOutPorts; ++i) {
                outPortPromises.add(new CompletableFuture<>());
            }

            return writeInputsAndCreateExecution(runtimeContext, stagingArea)
                .thenCompose(executionId -> {
                    ActorRef administrator = cloudKeeperEnvironment.getAdministrator();
                    boolean retrieveResults = cloudKeeperEnvironment.isRetrieveResults();
                    Object message = new AdministratorActorInterface.ManageExecution(executionId, runtimeContext,
                        stagingArea, retrieveResults, outPortPromises, finishTimeMillisPromise,
                        executionExceptionPromise);
                    Timeout localAskTimeout = cloudKeeperEnvironment.getLocalAskTimeout();
                    return ScalaFutures.completableFutureOf(
                        Patterns.ask(administrator, message, localAskTimeout)
                            .map(TO_LONG_MAPPER, executionContext),
                        executionContext
                    );
                })
                .thenApply(executionId -> {
                    ActorRef masterInterpreter = cloudKeeperEnvironment.getMasterInterpreter();
                    // Now that the administrator is aware of the execution, can we route the cancellation future to
                    // the master interpreter (and be sure that the cancellation event will transpire to the
                    // administrator).
                    cancellationPromise.exceptionally(throwable -> {
                        masterInterpreter.tell(
                            new MasterInterpreterActorInterface.CancelWorkflow(executionId, throwable),
                            ActorRef.noSender()
                        );
                        return null;
                    });

                    masterInterpreter.tell(
                        new MasterInterpreterActorInterface.StartExecution(executionId), ActorRef.noSender());

                    outPortFuturesPromise.complete(ImmutableList.copyOf(outPortPromises));
                    return executionId;
                });
        }

        private static final class RuntimeState {
            private final RuntimeContext runtimeContext;
            private final StagingArea stagingArea;

            private RuntimeState(RuntimeContext runtimeContext, StagingArea stagingArea) {
                this.runtimeContext = runtimeContext;
                this.stagingArea = stagingArea;
            }

            private StagingArea getStagingArea() {
                return stagingArea;
            }
        }

        private static WorkflowExecutionException mapThrowable(Throwable throwable) {
            if (throwable instanceof WorkflowExecutionException) {
                return (WorkflowExecutionException) throwable;
            } else {
                return new WorkflowExecutionException(throwable);
            }
        }

        private WorkflowExecution createWorkflowExecution() {
            CompletionStage<RuntimeContext> runtimeContextStage = Futures
                .supply(() -> cloudKeeperEnvironment.getInstanceProvider().getInstance(RuntimeContextFactory.class))
                .thenCompose(runtimeContextFactory -> runtimeContextFactory.newRuntimeContext(bundleIdentifiers));
            CompletionStage<RuntimeState> runtimeStateStage = Futures.thenApplyAsync(
                runtimeContextStage,
                runtimeContext -> {
                    RuntimeAnnotatedExecutionTrace executionTrace
                        = runtimeContext.newAnnotatedExecutionTrace(ExecutionTrace.empty(), module, overrides);
                    StagingArea stagingArea = cloudKeeperEnvironment
                        .getStagingAreaProvider()
                        .provideStaging(
                            runtimeContext, executionTrace, cloudKeeperEnvironment.getInstanceProvider()
                        );
                    return new RuntimeState(runtimeContext, stagingArea);
                },
                executor
            );
            Futures.completeWith(stagingAreaPromise, runtimeStateStage.thenApply(RuntimeState::getStagingArea));
            CompletionStage<Long> executionIdStage = Futures.thenCompose(
                runtimeStateStage,
                runtimeState
                    -> createOutputFuturesAndStartExecution(runtimeState.runtimeContext, runtimeState.stagingArea)
            );
            Futures.completeWith(executionIdPromise, executionIdStage);

            // If executionIdPromise is completed with a failure, it is possible that those actions that would normally
            // complete the other promises are never run. Hence, we try to complete the promises exceptionally (to avoid
            // potential deadlocks by dangling promises).
            executionIdStage.exceptionally(throwable -> {
                Throwable unwrapped = Futures.unwrapCompletionException(throwable);
                outPortFuturesPromise.completeExceptionally(unwrapped);
                finishTimeMillisPromise.completeExceptionally(unwrapped);
                executionExceptionPromise.complete(Optional.of(mapThrowable(unwrapped)));
                return null;
            });

            return new WorkflowExecutionImpl(startTimeMillis,
                stagingAreaPromise, outPortFuturesPromise, executionIdPromise,
                finishTimeMillisPromise, executionExceptionPromise, cancellationPromise);
        }
    }

    @Override
    public WorkflowExecution start() {
        Context context = new Context(cloudKeeperEnvironment, module, bundleIdentifiers, overrides, inputValues);
        return context.createWorkflowExecution();
    }

    private static final class WorkflowExecutionImpl implements WorkflowExecution {
        private final long startTimeMillis;
        private final CompletableFuture<StagingArea> stagingAreaFuture;
        private final CompletableFuture<ImmutableList<CompletableFuture<Object>>> outPortFuturesFuture;
        private final CompletableFuture<Long> executionIdFuture;
        private final CompletableFuture<Long> finishTimeFuture;
        private final CompletableFuture<Optional<WorkflowExecutionException>> executionExceptionFuture;
        private final CompletableFuture<Void> cancellationPromise;

        private WorkflowExecutionImpl(
                long startTimeMillis,
                CompletableFuture<StagingArea> stagingAreaFuture,
                CompletableFuture<ImmutableList<CompletableFuture<Object>>> outPortFuturesFuture,
                CompletableFuture<Long> executionIdFuture,
                CompletableFuture<Long> finishTimeFuture,
                CompletableFuture<Optional<WorkflowExecutionException>> executionExceptionFuture,
                CompletableFuture<Void> cancellationPromise) {
            this.startTimeMillis = startTimeMillis;
            this.stagingAreaFuture = stagingAreaFuture;
            this.outPortFuturesFuture = outPortFuturesFuture;
            this.executionIdFuture = executionIdFuture;
            this.finishTimeFuture = finishTimeFuture;
            this.executionExceptionFuture = executionExceptionFuture;
            this.cancellationPromise = cancellationPromise;
        }

        @Override
        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        @Override
        public boolean cancel() {
            // From CompletableFuture#cancel(boolean) API doc: "no effect in this implementation because interrupts are
            // not used to control processing"
            return isRunning() && cancellationPromise.cancel(true);
        }

        @Override
        public CompletableFuture<Long> getExecutionId() {
            return Futures.unwrapCompletionException(executionIdFuture);
        }

        @Override
        public CompletableFuture<RuntimeAnnotatedExecutionTrace> getTrace() {
            return Futures.unwrapCompletionException(
                stagingAreaFuture.thenApply(StagingArea::getAnnotatedExecutionTrace)
            );
        }

        @Override
        public boolean isRunning() {
            return !finishTimeFuture.isDone();
        }

        private CompletableFuture<Object> getOutputValueFuture(String outPortString, StagingArea stagingArea) {
            final SimpleName outPortName = SimpleName.identifier(outPortString);
            RuntimeModule runtimeModule = stagingArea.getAnnotatedExecutionTrace().getModule();

            @Nullable RuntimePort port = runtimeModule.getEnclosedElement(RuntimePort.class, outPortName);
            if (!(port instanceof RuntimeOutPort)) {
                return Futures.completedExceptionally(new WorkflowExecutionException(String.format(
                    "Out-port with name '%s' does not exist in %s.",
                    outPortName, runtimeModule
                )));
            }
            final int index = ((RuntimeOutPort) port).getOutIndex();

            return outPortFuturesFuture
                .thenCompose(outPortFutures -> outPortFutures.get(index));
        }

        @Override
        public CompletableFuture<Object> getOutput(String outPortName) {
            return Futures.unwrapCompletionException(
                stagingAreaFuture.thenCompose(stagingArea -> getOutputValueFuture(outPortName, stagingArea))
            );
        }

        @Override
        public CompletableFuture<Long> getFinishTimeMillis() {
            return Futures.unwrapCompletionException(finishTimeFuture);
        }

        @Override
        public CompletableFuture<Void> toCompletableFuture() {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            executionExceptionFuture.whenComplete((optionalException, failure) -> {
                assert (failure != null || optionalException != null)
                    && !(failure instanceof CompletionException);
                if (failure != null) {
                    completableFuture.completeExceptionally(failure);
                } else if (optionalException.isPresent()) {
                    completableFuture.completeExceptionally(optionalException.get());
                } else {
                    completableFuture.complete(null);
                }
            });
            return completableFuture;
        }
    }
}
