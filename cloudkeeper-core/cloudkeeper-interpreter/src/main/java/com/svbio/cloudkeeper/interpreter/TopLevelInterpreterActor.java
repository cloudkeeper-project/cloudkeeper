package com.svbio.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.svbio.cloudkeeper.interpreter.MasterInterpreterActorInterface.CreateExecution;
import com.svbio.cloudkeeper.interpreter.event.FailedExecutionTraceEvent;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvisionException;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Top-level interpreter.
 *
 * <p>Top-level actors are supervised by the {@link MasterInterpreterActor}, and there is exactly one top-level
 * interpreter for each workflow execution. All (transitively) supervised actors run within the same JVM. This means
 * that reliable messaging may be assumed, and messages do not have to be serializable.
 */
final class TopLevelInterpreterActor extends AbstractActor {
    private static final String ROOT_MODULE_INTERPRETER_NAME = "root";
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);

    private final long executionId;
    @Nullable private final ExecutionContext asyncTaskContext;
    private final CreateExecution parameters;
    private final ActorRef administrator;
    private final InterpreterEventBus eventBus;

    private InstanceProvider instanceProvider;
    private LocalInterpreterProperties interpreterProperties;
    private StagingArea stagingArea;
    private RuntimeModule module;
    private ActorRef rootActor;
    private BitSet outPortsRequiringValue;

    private State state = State.WAITING_FOR_START_MESSAGE;

    private enum State {
        /**
         * Currently waiting for the the {@link TopLevelInterpreterActorInterface.Start} message (sent by the
         * {@link MasterInterpreterActor}).
         */
        WAITING_FOR_START_MESSAGE,

        /**
         * Received start message, but has not started the interpreter for the root module yet. The actor can stay in
         * this state for a while if the staging area takes a while to be constructed.
         */
        READY,

        /**
         * Started the interpreter for the root module and waiting for result.
         */
        RUNNING,

        /**
         * The values for all out-ports have been received.
         */
        RECEIVED_OUT_PORT_VALUES,

        /**
         * The module interpreter actor has terminated.
         */
        DONE
    }

    /**
     * {@link Creator} for {@link TopLevelInterpreterActor}.
     */
    static final class Factory implements Creator<UntypedActor> {
        private final long executionId;
        @Nullable private final ExecutionContext asyncTaskContext;
        private final CreateExecution parameters;

        /**
         * Constructor.
         *
         * @param executionId id of this workflow execution
         * @param asyncTaskContext The {@link ExecutionContext} that is to be used for scheduling asynchronous tasks
         *     (such as futures), or {@code null} to indicate that {@code getContext().dispatcher()} should be used.
         *     This should be non-null only if the actor is created within the current JVM. This parameter is
         *     <em>not</em> preserved during serialization.
         * @param parameters execution parameters stemming from
         *     {@link com.svbio.cloudkeeper.model.api.WorkflowExecutionBuilder}
         */
        Factory(long executionId, @Nullable ExecutionContext asyncTaskContext, CreateExecution parameters) {
            this.executionId = executionId;
            this.asyncTaskContext = asyncTaskContext;
            this.parameters = Objects.requireNonNull(parameters);
        }

        private Object writeReplace() throws ObjectStreamException {
            return new SerializableFactory(executionId, parameters);
        }

        @Override
        public UntypedActor create() {
            return new TopLevelInterpreterActor(this);
        }
    }

    /**
     * Serializable version of {@link Factory}.
     */
    private static final class SerializableFactory implements Serializable {
        private static final long serialVersionUID = 2056119771158997124L;

        private final long executionId;
        private final CreateExecution parameters;

        private SerializableFactory(long executionId, CreateExecution parameters) {
            this.executionId = executionId;
            this.parameters = parameters;
        }

        private Object readResolve() {
            return new Factory(executionId, null, parameters);
        }
    }

    private TopLevelInterpreterActor(Factory factory) {
        super(factory.asyncTaskContext);
        executionId = factory.executionId;
        asyncTaskContext = factory.asyncTaskContext;
        parameters = factory.parameters;
        InterpreterProperties properties = factory.parameters.getExecutionProperties();
        administrator = properties.getAdministrator();

        eventBus = new InterpreterEventBus();
        for (EventSubscription subscription: properties.getEventSubscriptions()) {
            eventBus.subscribe(subscription.getActorRef(), subscription.getClassifier());
        }
    }

    @Override
    void onEmptySetOfAsynchronousActions() {
        if (state == State.DONE) {
            administrator.tell(new AdministratorActorInterface.ExecutionFinished(executionId, null), getSelf());

            // Terminate ourselves
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    /**
     * Handles event that an out-port received a value.
     *
     * @param outPortId index of the out-port in {@link RuntimeModule#getOutPorts()}
     */
    private void moduleOutPortHasSignal(int outPortId) {
        assert state == State.RUNNING;

        if (outPortsRequiringValue.get(outPortId)) {
            outPortsRequiringValue.set(outPortId, false);
            administrator.tell(
                new AdministratorActorInterface.OutPortAvailable(executionId, outPortId), getSelf()
            );

            if (outPortsRequiringValue.isEmpty()) {
                state = State.RECEIVED_OUT_PORT_VALUES;
            }

            endAsynchronousAction(module.getOutPorts().get(outPortId));
        } else {
            RuntimeOutPort outPort = module.getOutPorts().get(outPortId);
            log.warning(String.format(
                "Ignoring redundant value for %s during state %s.",
                outPort, state
            ));
        }
    }

    private void start(Props props) {
        assert state == State.RUNNING;

        rootActor = getContext().actorOf(props, ROOT_MODULE_INTERPRETER_NAME);
        getContext().watch(rootActor);
        startAsynchronousAction(rootActor, "supervising interpreter for root module");

        // We need to explicitly tell the child actor about the updated inputs!
        BitSet updatedInPorts = parameters.getUpdatedInPorts();
        for (
            int inPortId = updatedInPorts.nextSetBit(0);
            inPortId >= 0;
            inPortId = updatedInPorts.nextSetBit(inPortId + 1)
        ) {
            rootActor.tell(new InterpreterInterface.InPortHasSignal(inPortId), getSelf());
        }
    }

    /**
     * Returns a future that will be completed with a {@link BitSet} that contains the indices of all in-ports that have
     * a value.
     */
    private Future<BitSet> getInPortsWithValues() {
        final RuntimeModule child = stagingArea.getAnnotatedExecutionTrace().getModule();
        final List<? extends RuntimeInPort> inPorts = child.getInPorts();
        final List<ExecutionTrace> portValueTraces = inPorts.stream()
            .map(inPort -> ExecutionTrace.empty().resolveInPort(inPort.getSimpleName()))
            .collect(Collectors.toList());

        return Futures
            .traverse(portValueTraces, stagingArea::exists, getContext().dispatcher())
            .map(
                new Mapper<Iterable<Boolean>, BitSet>() {
                    @Override
                    public BitSet apply(Iterable<Boolean> hasValueIterable) {
                        BitSet inPortsWithValue = new BitSet(inPorts.size());

                        int inPortId = 0;
                        for (boolean hasValue: hasValueIterable) {
                            if (hasValue) {
                                inPortsWithValue.set(inPortId);
                            }
                            ++inPortId;
                        }
                        return inPortsWithValue;
                    }
                },
                getContext().dispatcher()
            );
    }

    private void run() {
        assert state.compareTo(State.READY) <= 0;

        if (stagingArea == null || state != State.READY) {
            // Staging area is missing, or we have not yet received the start message.
            return;
        }

        state = State.RUNNING;
        module = stagingArea.getAnnotatedExecutionTrace().getModule();
        final int numOutPorts = module.getOutPorts().size();

        outPortsRequiringValue = new BitSet(numOutPorts);
        outPortsRequiringValue.set(0, numOutPorts);

        if (numOutPorts > 0) {
            for (RuntimeOutPort outPort: module.getOutPorts()) {
                startAsynchronousAction(outPort, "waiting for value to arrive at root module out-port %s",
                    outPort.getSimpleName());
            }
        } else {
            state = State.RECEIVED_OUT_PORT_VALUES;
        }

        // It is OK to close over module, stagingArea, and outPortsRequiringValue, because they are effectively
        // immutable before the future is completed.
        Future<Props> futureProps =
            getInPortsWithValues()
            .map(
                new Mapper<BitSet, Props>() {
                    @Override
                    public Props checkedApply(BitSet inPortsWithValue) throws InterpreterException {
                        int numInPorts = module.getInPorts().size();
                        if (inPortsWithValue.length() != numInPorts || inPortsWithValue.cardinality() != numInPorts) {
                            List<SimpleName> missingInPortNames = module.getInPorts().stream()
                                .filter(inPort -> !inPortsWithValue.get(inPort.getInIndex()))
                                .map(RuntimeInPort::getSimpleName)
                                .collect(Collectors.toList());
                            throw new InterpreterException(ExecutionTrace.empty(), String.format(
                                "Values for in-ports %s are missing.", missingInPortNames
                            ));
                        }

                        return parameters.getInterpreterPropsProvider()
                            .provideInterpreterProps(interpreterProperties, stagingArea,
                                0, Collections.nCopies(numInPorts, DependencyGraph.HasValue.HAS_VALUE),
                                parameters.getUpdatedInPorts(), outPortsRequiringValue);
                    }
                },
                getContext().dispatcher()
            );
        pipeResultToSelf(futureProps, "checking in-ports with values");
    }

    private void setRuntimeContext(RuntimeContext runtimeContext) {
        assert state.compareTo(State.READY) <= 0;
        assert instanceProvider != null;

        if (interpreterProperties == null) {
            interpreterProperties = new LocalInterpreterProperties(parameters.getExecutionProperties(), executionId,
                runtimeContext, asyncTaskContext, eventBus);
            try {
                stagingArea = parameters.getRuntimeStateProvider().provideStagingArea(runtimeContext, instanceProvider);
                run();
            } catch (LinkerException | InstanceProvisionException exception) {
                failure(
                    new InterpreterException(ExecutionTrace.empty(), "Failed to reconstruct staging area.", exception)
                );
            }
        } else {
            log.warning(String.format("Ignoring duplicate '%s' message.", RuntimeContext.class.getSimpleName()));
        }
    }

    private void setInstanceProvider(InstanceProvider instanceProvider) {
        assert state.compareTo(State.READY) <= 0;

        if (this.instanceProvider == null) {
            this.instanceProvider = instanceProvider;
            Future<RuntimeContext> runtimeStateFuture
                = parameters.getRuntimeStateProvider().provideRuntimeContext(instanceProvider);
            pipeResultToSelf(runtimeStateFuture, "reconstructing runtime context");
        } else {
            log.warning(String.format("Ignoring duplicate '%s' message.", InstanceProvider.class.getSimpleName()));
        }
    }

    @Override
    public void preStart() {
        String instanceProviderActorPath = parameters.getInstanceProviderActorPath();
        Future<Object> instanceProviderFuture = Patterns.ask(
            getContext().actorSelection(instanceProviderActorPath),
            InstanceProviderActorInterface.GetInstanceProviderMessage.INSTANCE,
            new Timeout(1, TimeUnit.SECONDS)
        );
        pipeResultToSelf(instanceProviderFuture, "asking actor at '%s' for '%s'", instanceProviderActorPath,
            InstanceProvider.class.getSimpleName());
    }

    /**
     * Handles a {@link TopLevelInterpreterActorInterface.Start} message (which is sent by the
     * {@link MasterInterpreterActor}).
     */
    private void startRunning() {
        if (state == State.WAITING_FOR_START_MESSAGE) {
            state = State.READY;
            run();
        } else {
            log.warning("Ignoring redundant start message.");
        }
    }

    /**
     * Handles event that a child actor terminated.
     *
     * @param childActor actor reference of child actor
     */
    private void childActorTerminated(ActorRef childActor) {
        if (childActor != rootActor) {
            log.warning(String.format("Ignoring terminated message for unknown child actor %s.", childActor));
        } else if (state != State.RECEIVED_OUT_PORT_VALUES) {
            failure(new InterpreterException(
                ExecutionTrace.empty(),
                "Root module interpreter terminated before all out-port values were received."
            ));
        } else {
            state = State.DONE;
            endAsynchronousAction(rootActor);
        }
    }

    private void publishFailure(InterpreterException exception) {
        interpreterProperties.getEventBus().publish(
            FailedExecutionTraceEvent.of(executionId, System.currentTimeMillis(), exception.getExecutionTrace(),
                exception)
        );
    }

    /**
     * Terminates this actor due to a failure.
     *
     * <p>This method will send a failure message to the workflow manager and then asynchronously terminate this actor.
     *
     * @param throwable the exception or throwable that caused the failure
     */
    private void failure(Throwable throwable) {
        InterpreterException exception = throwable instanceof InterpreterException
            ? (InterpreterException) throwable
            : new InterpreterException(
                ExecutionTrace.empty(),
                String.format("Top-level interpreter for execution ID %d terminating due to exception.", executionId),
                throwable
            );

        state = State.DONE;
        publishFailure(exception);
        administrator.tell(new AdministratorActorInterface.ExecutionFinished(executionId, exception), getSelf());

        // Time to terminate this actor
        getContext().stop(getSelf());
    }

    @Override
    protected void asynchronousActionFailed(String message, Throwable cause) throws InterpreterException {
        InterpreterException exception = cause instanceof InterpreterException
            ? (InterpreterException) cause
            : new InterpreterException(ExecutionTrace.empty(), message, cause);
        failure(exception);
    }

    @Override
    public void postStop() {
        if (state != State.DONE) {
            InterpreterException exception = new InterpreterException(
                ExecutionTrace.empty(),
                String.format("Top-level interpreter for execution ID %d terminated unexpectedly.", executionId)
            );
            publishFailure(exception);
            administrator.tell(
                new AdministratorActorInterface.ExecutionFinished(executionId, exception),
                ActorRef.noSender()
            );
        }

        if (interpreterProperties != null) {
            try {
                interpreterProperties.getRuntimeContext().close();
            } catch (IOException exception) {
                log.error(exception, "Failed to close runtime context.");
            }
        }
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message instanceof InterpreterInterface.SubmoduleOutPortHasSignal) {
            moduleOutPortHasSignal(((InterpreterInterface.SubmoduleOutPortHasSignal) message).getOutPortId());
        } else if (message instanceof InstanceProvider) {
            setInstanceProvider((InstanceProvider) message);
        } else if (message instanceof RuntimeContext) {
            setRuntimeContext((RuntimeContext) message);
        } else if (message == TopLevelInterpreterActorInterface.Start.INSTANCE) {
            startRunning();
        } else if (message instanceof Props) {
            start((Props) message);
        } else if (message instanceof Terminated) {
            childActorTerminated(((Terminated) message).actor());
        } else if (message instanceof Status.Failure) {
            failure(((Status.Failure) message).cause());
        } else {
            super.onReceive(message);
        }
    }

    /**
     * Returns how to handle the given fault that occurred in a child actor.
     *
     * <p>If an exception occurs in the child actor, we send a {@link Status.Failure} message to this actor.
     * Otherwise, if the {@link Throwable} is not an exception, the failure is escalated; that is, this actor will
     * fail itself.
     *
     * <p>The strategy deactivates the Akka-provided logging, which logs all exception as errors by default. Instead,
     * this strategy performs its own logging: If the {@link Throwable} is an {@link Exception}, the exception is logged
     * at the debug level. Otherwise, the {@link Throwable} is logged at the error level.
     */
    private SupervisorStrategy.Directive supervisorDirective(Throwable throwable) {
        if (throwable instanceof Exception) {
            InterpreterException exception = throwable instanceof InterpreterException
                ? (InterpreterException) throwable
                : new InterpreterException(
                    ExecutionTrace.empty(),
                    String.format("Failure of root-module interpreter %s.", getSender()),
                    throwable
                );

            if (log.isDebugEnabled()) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter);
                writer.println(
                    "Exception thrown in root-module interpreter and caught in top-level interpreter.");
                throwable.printStackTrace(writer);
                log.debug(stringWriter.toString());
            }

            getSelf().tell(new Status.Failure(exception), getSender());
            return SupervisorStrategy.stop();
        } else {
            log.error(throwable, "Error in root-module interpreter. Escalating... The JVM may not survive.");
            return SupervisorStrategy.escalate();
        }
    }

    /**
     * Supervisor strategy.
     *
     * @see #supervisorStrategy()
     */
    private final SupervisorStrategy supervisorStrategy
        = new AllForOneStrategy(0, Duration.Inf(), this::supervisorDirective, false);

    /**
     * Returns the supervisor strategy for this top-level actor.
     *
     * @see #supervisorDirective(Throwable)
     */
    @Override
    public SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }
}
