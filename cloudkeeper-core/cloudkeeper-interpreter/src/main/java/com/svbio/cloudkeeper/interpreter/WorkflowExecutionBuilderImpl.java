package com.svbio.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.dispatch.OnFailure;
import akka.japi.Option;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.CancellationException;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.RuntimeContextFactory;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.api.WorkflowExecution;
import com.svbio.cloudkeeper.model.api.WorkflowExecutionBuilder;
import com.svbio.cloudkeeper.model.api.WorkflowExecutionException;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvisionException;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.api.util.ScalaFutures;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.execution.MutableOverride;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimePort;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.util.Failure;
import scala.util.Success;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        private final ExecutionContext executionContext;
        private final BareModule module;
        private final List<URI> bundleIdentifiers;
        private final List<BareOverride> overrides;
        private final Map<SimpleName, Object> inputValues;

        private final Promise<StagingArea> stagingAreaPromise = Futures.promise();
        private final Promise<ImmutableList<Future<Object>>> outPortFuturesPromise = Futures.promise();
        private final Promise<Long> executionIdPromise = Futures.promise();
        private final Promise<Long> finishTimeMillisPromise = Futures.promise();
        private final Promise<Option<WorkflowExecutionException>> executionExceptionPromise = Futures.promise();
        private final Promise<CancellationException> cancellationPromise = Futures.promise();

        private Context(CloudKeeperEnvironmentImpl environment, BareModule module,
            List<URI> bundleIdentifiers, List<BareOverride> overrides,
            Map<SimpleName, Object> inputValues) {

            assert environment != null && module != null && bundleIdentifiers != null && overrides != null
                && inputValues != null;

            cloudKeeperEnvironment = environment;
            executionContext = environment.getExecutionContext();
            this.module = module;
            this.bundleIdentifiers = bundleIdentifiers;
            this.overrides = overrides;
            this.inputValues = inputValues;
        }

        /**
         * Writes the inputs to the staging area and send a create-execution message to the master interpreter. This
         * message will only return a new execution ID, but the execution will not yet be started.
         */
        private Future<Long> writeInputsAndCreateExecution(RuntimeContext runtimeContext, StagingArea stagingArea)
                throws WorkflowExecutionException {
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
            List<Future<RuntimeExecutionTrace>> inputFutures = new ArrayList<>(inputValues.size());
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
            return ScalaFutures.createListFuture(inputFutures, executionContext)
                .flatMap(new Mapper<ImmutableList<RuntimeExecutionTrace>, Future<Long>>() {
                    @Override
                    public Future<Long> apply(ImmutableList<RuntimeExecutionTrace> ignored) {
                        ActorRef masterInterpreter = cloudKeeperEnvironment.getMasterInterpreter();
                        return Patterns
                            .ask(masterInterpreter, message, cloudKeeperEnvironment.getRemoteAskTimeout())
                            .map(TO_LONG_MAPPER, executionContext);
                    }
                }, executionContext);
        }

        /**
         *
         */
        private Future<Long> createOutputFuturesAndStartExecution(RuntimeContext runtimeContext,
                StagingArea stagingArea) throws WorkflowExecutionException {
            final RuntimeModule runtimeModule = stagingArea.getAnnotatedExecutionTrace().getModule();
            int numOutPorts = runtimeModule.getOutPorts().size();
            final List<Promise<Object>> outPortPromises = new ArrayList<>(numOutPorts);
            final List<Future<Object>> outPortFutures = new ArrayList<>(numOutPorts);
            for (int i = 0; i < numOutPorts; ++i) {
                Promise<Object> promise = Futures.promise();
                outPortPromises.add(promise);
                outPortFutures.add(promise.future());
            }

            return writeInputsAndCreateExecution(runtimeContext, stagingArea)
                .flatMap(new Mapper<Long, Future<Object>>() {
                    @Override
                    public Future<Object> apply(Long executionId) {
                        ActorRef administrator = cloudKeeperEnvironment.getAdministrator();
                        boolean retrieveResults = cloudKeeperEnvironment.isRetrieveResults();
                        Object message = new AdministratorActorInterface.ManageExecution(executionId, runtimeContext,
                            stagingArea, retrieveResults, outPortPromises, finishTimeMillisPromise,
                            executionExceptionPromise);
                        Timeout localAskTimeout = cloudKeeperEnvironment.getLocalAskTimeout();
                        return Patterns.ask(administrator, message, localAskTimeout);
                    }
                }, executionContext)
                .map(TO_LONG_MAPPER, executionContext)
                .map(new Mapper<Long, Long>() {
                    @Override
                    public Long apply(final Long executionId) {
                        final ActorRef masterInterpreter = cloudKeeperEnvironment.getMasterInterpreter();
                        // Now that the administrator is aware of the execution, can we route the cancellation future to
                        // the master interpreter (and be sure that the cancellation event will transpire to the
                        // administrator).
                        cancellationPromise.future()
                            .onFailure(new OnFailure() {
                                @Override
                                public void onFailure(Throwable throwable) {
                                    masterInterpreter.tell(
                                        new MasterInterpreterActorInterface.CancelWorkflow(executionId, throwable),
                                        ActorRef.noSender()
                                    );
                                }
                            }, executionContext);

                        masterInterpreter.tell(
                            new MasterInterpreterActorInterface.StartExecution(executionId), ActorRef.noSender());

                        outPortFuturesPromise.complete(new Success<>(ImmutableList.copyOf(outPortFutures)));
                        return executionId;
                    }
                }, executionContext);
        }

        private static final class RuntimeState {
            private final RuntimeContext runtimeContext;
            private final StagingArea stagingArea;

            private RuntimeState(RuntimeContext runtimeContext, StagingArea stagingArea) {
                this.runtimeContext = runtimeContext;
                this.stagingArea = stagingArea;
            }
        }

        private WorkflowExecution createWorkflowExecution() {
            Future<RuntimeContextFactory> runtimeContextFactoryFuture = ScalaFutures.supplySync(
                () -> cloudKeeperEnvironment.getInstanceProvider().getInstance(RuntimeContextFactory.class)
            );

            Future<RuntimeState> runtimeStateFuture = runtimeContextFactoryFuture
                .flatMap(new Mapper<RuntimeContextFactory, Future<RuntimeContext>>() {
                    @Override
                    public Future<RuntimeContext> apply(RuntimeContextFactory runtimeContextFactory) {
                        return runtimeContextFactory.newRuntimeContext(bundleIdentifiers);
                    }
                }, executionContext)
                .map(new Mapper<RuntimeContext, RuntimeState>() {
                    @Override
                    public RuntimeState checkedApply(RuntimeContext runtimeContext)
                        throws LinkerException, InstanceProvisionException {
                        RuntimeAnnotatedExecutionTrace executionTrace
                            = runtimeContext.newAnnotatedExecutionTrace(ExecutionTrace.empty(), module, overrides);
                        StagingArea stagingArea = cloudKeeperEnvironment
                            .getStagingAreaProvider()
                            .provideStaging(
                                runtimeContext, executionTrace, cloudKeeperEnvironment.getInstanceProvider()
                            );
                        return new RuntimeState(runtimeContext, stagingArea);
                    }
                }, executionContext);

            Future<StagingArea> stagingAreaFuture = runtimeStateFuture.map(new Mapper<RuntimeState, StagingArea>() {
                @Override
                public StagingArea apply(RuntimeState parameter) {
                    return parameter.stagingArea;
                }
            }, executionContext);
            stagingAreaPromise.completeWith(stagingAreaFuture);

            Future<Long> executionIdFuture = runtimeStateFuture
                .flatMap(new Mapper<RuntimeState, Future<Long>>() {
                    @Override
                    public Future<Long> checkedApply(RuntimeState runtimeState) throws WorkflowExecutionException {
                        return createOutputFuturesAndStartExecution(
                            runtimeState.runtimeContext, runtimeState.stagingArea);
                    }
                }, executionContext);
            executionIdPromise.completeWith(executionIdFuture);

            // If executionIdPromise is completed with a failure, it is possible that those actions that would normally
            // complete the other promises are never run. Hence, we try to complete the promises exceptionally (to avoid
            // potential deadlocks by dangling promises).
            executionIdFuture.onFailure(new OnFailure() {
                @Override
                public void onFailure(Throwable throwable) {
                    outPortFuturesPromise.tryFailure(throwable);
                    finishTimeMillisPromise.tryFailure(throwable);
                    executionExceptionPromise.trySuccess(Option.some(mapThrowable(throwable)));
                }
            }, executionContext);

            return new WorkflowExecutionImpl(executionContext, startTimeMillis,
                stagingAreaPromise.future(), outPortFuturesPromise.future(), executionIdPromise.future(),
                finishTimeMillisPromise.future(), executionExceptionPromise.future(), cancellationPromise);
        }
    }

    @Override
    public WorkflowExecution start() {
        Context context = new Context(cloudKeeperEnvironment, module, bundleIdentifiers, overrides, inputValues);
        return context.createWorkflowExecution();
    }

    static WorkflowExecutionException mapThrowable(Throwable throwable) {
        if (throwable == null) {
            return null;
        } else if (throwable instanceof WorkflowExecutionException) {
            return (WorkflowExecutionException) throwable;
        } else {
            return new WorkflowExecutionException(throwable);
        }
    }

    private static final class WorkflowExecutionImpl implements WorkflowExecution {
        private final ExecutionContext executionContext;
        private final long startTimeMillis;
        private final Future<StagingArea> stagingAreaFuture;
        private final Future<ImmutableList<Future<Object>>> outPortFuturesFuture;
        private final Future<Long> executionIdFuture;
        private final Future<Long> finishTimeFuture;
        private final Future<Option<WorkflowExecutionException>> executionExceptionFuture;
        private final Promise<CancellationException> cancellationPromise;

        private WorkflowExecutionImpl(ExecutionContext executionContext,
                long startTimeMillis,
                Future<StagingArea> stagingAreaFuture,
                Future<ImmutableList<Future<Object>>> outPortFuturesFuture,
                Future<Long> executionIdFuture,
                Future<Long> finishTimeFuture,
                Future<Option<WorkflowExecutionException>> executionExceptionFuture,
                Promise<CancellationException> cancellationPromise) {
            this.executionContext = executionContext;
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
            return isRunning()
                && cancellationPromise.tryComplete(new Failure<>(new CancellationException()));
        }

        @Override
        public void whenHasExecutionId(final OnActionComplete<Long> onHasExecutionId) {
            executionIdFuture.onComplete(new OnComplete<Long>() {
                @Override
                public void onComplete(Throwable throwable, Long executionId) {
                    onHasExecutionId.complete(throwable, executionId);
                }
            }, executionContext);
        }

        @Override
        public void whenHasRootExecutionTrace(
                final OnActionComplete<RuntimeAnnotatedExecutionTrace> onHasRootExecutionTrace) {
            stagingAreaFuture.onComplete(new OnComplete<StagingArea>() {
                @Override
                public void onComplete(Throwable throwable, StagingArea stagingArea) {
                    onHasRootExecutionTrace.complete(throwable, stagingArea.getAnnotatedExecutionTrace());
                }
            }, executionContext);
        }

        @Override
        public boolean isRunning() {
            return !finishTimeFuture.isCompleted();
        }

        private Future<Object> getOutputValueFuture(String outPortString, StagingArea stagingArea) {
            final SimpleName outPortName = SimpleName.identifier(outPortString);
            RuntimeModule runtimeModule = stagingArea.getAnnotatedExecutionTrace().getModule();

            @Nullable RuntimePort port = runtimeModule.getEnclosedElement(RuntimePort.class, outPortName);
            if (!(port instanceof RuntimeOutPort)) {
                return Futures.failed(new WorkflowExecutionException(String.format(
                    "Out-port with name '%s' does not exist in %s.",
                    outPortName, runtimeModule
                )));
            }
            final int index = ((RuntimeOutPort) port).getOutIndex();

            return outPortFuturesFuture
                .flatMap(new Mapper<ImmutableList<Future<Object>>, Future<Object>>() {
                    @Override
                    public Future<Object> apply(ImmutableList<Future<Object>> outPortFutures) {
                        return outPortFutures.get(index);
                    }
                }, executionContext);
        }

        @Override
        public void whenHasOutput(final String outPortName, final OnActionComplete<Object> onHasOutput) {
            Future<Object> outputValueFuture = stagingAreaFuture
                .flatMap(new Mapper<StagingArea, Future<Object>>() {
                    @Override
                    public Future<Object> apply(StagingArea stagingArea) {
                        return getOutputValueFuture(outPortName, stagingArea);
                    }
                }, executionContext);

            outputValueFuture.onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable throwable, Object object) {
                    onHasOutput.complete(mapThrowable(throwable), object);
                }
            }, executionContext);
        }

        @Override
        public void whenHasFinishTimeMillis(final OnActionComplete<Long> onHasFinishTimeMillis) {
            finishTimeFuture.onComplete(new OnComplete<Long>() {
                @Override
                public void onComplete(Throwable throwable, Long finishTimeMillis) {
                    onHasFinishTimeMillis.complete(throwable, finishTimeMillis);
                }
            }, executionContext);
        }

        @Override
        public void whenExecutionFinished(final OnActionComplete<Void> onExecutionFinished) {
            executionExceptionFuture.onComplete(new OnComplete<Option<WorkflowExecutionException>>() {
                @Override
                public void onComplete(Throwable throwable, Option<WorkflowExecutionException> optionalException) {
                    WorkflowExecutionException exception = null;
                    if (throwable != null) {
                        exception = new WorkflowExecutionException("Unexpected exception.", throwable);
                    } else if (optionalException.isDefined()) {
                        exception = optionalException.get();
                    }
                    onExecutionFinished.complete(exception, null);
                }
            }, executionContext);
        }
    }
}
