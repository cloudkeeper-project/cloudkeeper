package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.interpreter.DependencyGraph.DependencyGraphNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.HasValue;
import xyz.cloudkeeper.interpreter.DependencyGraph.InPortNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.PortState;
import xyz.cloudkeeper.interpreter.DependencyGraph.SubmoduleInPortNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.SubmoduleNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.ValueNode;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeChildOutToParentOutConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeConnectionVisitor;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentInToChildInConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimePort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeShortCircuitConnection;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeSiblingConnection;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static xyz.cloudkeeper.interpreter.InterpreterInterface.InPortHasSignal;
import static xyz.cloudkeeper.interpreter.InterpreterInterface.SubmoduleOutPortHasSignal;

/**
 * Interpreter of composite modules.
 */
final class CompositeModuleInterpreterActor extends AbstractModuleInterpreterActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);

    private final RuntimeParentModule module;
    private final StagingArea stagingArea;
    private final InterpreterPropsProvider interpreterPropsProvider;

    /**
     * Set of all in-ports that require state {@link PortState#RECOMPUTE}.
     *
     * <p>This set is never modified.
     */
    private final BitSet recomputedInPorts;

    /**
     * Set of all out-ports for that a {@link SubmoduleOutPortHasSignal} needs to be sent.
     *
     * <p>All out-ports in this set require a state that is not {@link PortState#IRRELEVANT}.
     *
     * <p>This set is never modified.
     */
    private final BitSet requestedOutPorts;

    /**
     * Map from child actor references to the submodule that they were started for.
     *
     * <p>Entries to this map are added from {@link #getChildExecutor(RuntimeModule)} and entries are removed in
     * {@link #childActorTerminated(ActorRef)}.
     */
    private final Map<ActorRef, RuntimeModule> childActorMap;

    /**
     * Array that contains an actor reference for every submodule of {@link #module}.
     *
     * <p>Initially, all elements of this array are null, and set to a non-null value the first time
     * {@link #getChildExecutor(RuntimeModule)} is called.
     */
    private final ActorRef[] childExecutors;

    /**
     * Set of the indices of all in-ports that received a value.
     */
    private final BitSet inPortReceivedMessage;

    /**
     * Array of sets, one for each submodule, containing the indices of all out-ports that have provided a value.
     */
    private final BitSet[] submoduleOutPortsReceivedMessage;

    /**
     * Set of all out-port that still require to receive a value.
     *
     * <p>This set is modified whenever an output receives a value. Interpretation of the current module is finished
     * once this set becomes empty.
     */
    private final BitSet outPortsRequiringValue;

    /**
     * The dependency graph constituted by the current parent module.
     */
    private final DependencyGraph dependencyGraph;

    /**
     * Instance of the <em>ComputeResumeState</em> algorithm.
     */
    private final ComputeResumeState computeResumeState;


    enum State {
        /**
         * Finding out from where to resume. No child executors are started yet.
         */
        STARTING,

        /**
         * Replaying the {@link InPortHasSignal} messages that were accumulated during the previous {@link #STARTING}
         * state.
         */
        REPLAY,

        /**
         * Running normally, child executors are running and invoked whenever a new signal arrives.
         */
        RUNNING,

        /**
         * Produced all outputs and notified parent interpreter about it.
         *
         * <p>This means that this interpreter is essentially done, but there may still be child interpreters that are
         * processing submodules that were not needed to compute the outputs of the current parent module. This
         * interpreter will only terminate once all child actors have terminated.
         */
        ALL_OUTPUTS,

        /**
         * Produced all outputs, notified parent interpreter, and all child interpreters have finished.
         *
         * <p>This means this actor is ready to terminate itself, and no failure occurred.
         */
        DONE
    }
    private State state = State.STARTING;


    /**
     * Factory for creating a composite-module interpreter.
     *
     * <p>This class is not meant to be serialized because actor creators for input-module interpreters will noly be
     * used within the same JVM. Any attempt to serialize a factory will cause a {@link NotSerializableException}.
     */
    static final class Factory implements Creator<UntypedActor> {
        private static final long serialVersionUID = -8810722067650577437L;

        private final LocalInterpreterProperties interpreterProperties;
        private final StagingArea stagingArea;
        private final InterpreterPropsProvider interpreterPropsProvider;
        private final RuntimeParentModule module;
        private final int moduleId;
        private final ImmutableList<HasValue> inPortsHasValueList;
        private final BitSet recomputedInPorts;
        private final BitSet requestedOutPorts;

        Factory(LocalInterpreterProperties interpreterProperties, StagingArea stagingArea,
                InterpreterPropsProvider interpreterPropsProvider, int moduleId,
                ImmutableList<HasValue> inPortsHasValueList, BitSet recomputedInPorts, BitSet requestedOutPorts) {
            Objects.requireNonNull(interpreterProperties);
            Objects.requireNonNull(stagingArea);
            Objects.requireNonNull(interpreterPropsProvider);
            Objects.requireNonNull(inPortsHasValueList);
            Objects.requireNonNull(recomputedInPorts);
            Objects.requireNonNull(requestedOutPorts);

            this.interpreterProperties = interpreterProperties;
            this.stagingArea = stagingArea;
            this.interpreterPropsProvider = interpreterPropsProvider;
            RuntimeModule untypedModule = stagingArea.getAnnotatedExecutionTrace().getModule();
            if (untypedModule instanceof RuntimeProxyModule) {
                RuntimeProxyModule proxyModule = (RuntimeProxyModule) untypedModule;
                RuntimeCompositeModuleDeclaration declaration
                    = (RuntimeCompositeModuleDeclaration) proxyModule.getDeclaration();
                module = declaration.getTemplate();
            } else {
                module = (RuntimeParentModule) untypedModule;
            }
            this.moduleId = moduleId;
            this.inPortsHasValueList = inPortsHasValueList;
            this.recomputedInPorts = (BitSet) Objects.requireNonNull(recomputedInPorts).clone();
            this.requestedOutPorts = (BitSet) Objects.requireNonNull(requestedOutPorts).clone();
        }

        private void readObject(ObjectInputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        @Override
        public UntypedActor create() {
            return new CompositeModuleInterpreterActor(this);
        }
    }

    private CompositeModuleInterpreterActor(Factory factory) {
        super(factory.interpreterProperties, factory.stagingArea.getAnnotatedExecutionTrace(), factory.moduleId);

        module = factory.module;
        stagingArea = factory.stagingArea;
        interpreterPropsProvider = factory.interpreterPropsProvider;
        recomputedInPorts = factory.recomputedInPorts;
        requestedOutPorts = factory.requestedOutPorts;

        int numSubmodules = module.getModules().size();
        childExecutors = new ActorRef[numSubmodules];
        outPortsRequiringValue = (BitSet) requestedOutPorts.clone();
        childActorMap = new HashMap<>(numSubmodules);

        dependencyGraph = new DependencyGraph(module, requestedOutPorts);
        for (InPortNode inPortNode: dependencyGraph.inPortNodes()) {
            HasValue hasValue = factory.inPortsHasValueList.get(inPortNode.getElement().getInIndex());
            assert hasValue != HasValue.PENDING_VALUE_CHECK;
            inPortNode.setHasValue(hasValue);
        }

        inPortReceivedMessage = new BitSet(module.getInPorts().size());
        submoduleOutPortsReceivedMessage = new BitSet[numSubmodules];
        for (RuntimeModule submodule: module.getModules()) {
            submoduleOutPortsReceivedMessage[submodule.getIndex()] = new BitSet(submodule.getOutPorts().size());
        }

        computeResumeState
            = new ComputeResumeState(dependencyGraph, recomputedInPorts, this::asynchronousHasValueCheck);
    }

    /**
     * Returns whether the given submodule's out-port is needed.
     *
     * @param outPort submodule's out-port
     */
    private boolean isOutPortNeeded(RuntimeOutPort outPort) {
        assert outPort.getModule().getParent() == module;

        return computeResumeState
            .getSubmodulesNeededOutPorts()[outPort.getModule().getIndex()]
            .get(outPort.getOutIndex());
    }

    /**
     * Triggers all in-ports that either are in state {@link PortState#READY} or that received a value while the state
     * of this actor was {@link State#STARTING}.
     */
    private void initialInPortsTrigger() {
        assert state == State.STARTING;

        state = State.REPLAY;

        // Trigger all in-ports whose state is READY
        dependencyGraph.inPortNodes().stream()
            .filter(node -> node.getPortState() == PortState.READY)
            .forEach(node -> transmitValueFromInPort(node.getElement()));

        // Trigger all in-ports who received a value while state was STARTING
        inPortReceivedMessage.stream().forEach(this::inPortHasSignal);
    }

    /**
     * Starts the interpretation of the current parent module if algorithm ComputeResumeState has finished.
     *
     * <p>This method sets the state of this actor to {@link State#RUNNING} and thus also finished the asynchronous
     * action that was started in {@link #preStart()}.
     */
    private void startRunningIfQueueEmpty() {
        assert state == State.STARTING;

        if (!computeResumeState.isFinished()) {
            return;
        }

        // Trigger in-ports
        initialInPortsTrigger();
        state = State.RUNNING;

        // Start child interpreters for all submodules that have at least one in-port whose state is READY
        submodules: for (List<SubmoduleInPortNode> submoduleInPortNodes: dependencyGraph.submodulesInPortNodes()) {
            for (SubmoduleInPortNode submoduleInPortNode : submoduleInPortNodes) {
                if (submoduleInPortNode.getPortState() == PortState.READY) {
                    getChildExecutor(submoduleInPortNode.getElement().getModule());
                    continue submodules;
                }
            }
        }

        // Start child interpreters for all submodules that have no in-ports and whose state is READY
        for (SubmoduleNode submoduleNode: dependencyGraph.submoduleNodes()) {
            if (submoduleNode.getElement().getInPorts().isEmpty()
                    && submoduleNode.getPortState() == PortState.READY) {
                getChildExecutor(submoduleNode.getElement());
            }
        }

        // Trigger all out-ports whose state is READY
        dependencyGraph.outPortNodes().stream()
            .filter(node -> node.getPortState() == PortState.READY)
            .forEach(node -> outportCarriesSignal(node.getElement().getOutIndex()));

        if (outPortsRequiringValue.isEmpty()) {
            state = State.ALL_OUTPUTS;
        }

        // Finish the asynchronous action started in #preStart().
        checkIfNoAsynchronousActions();
    }

    /**
     * Performs the initialization steps of algorithm <em>ComputeResumeState</em>.
     *
     * <p>This method sets the state of this actor to {@link State#STARTING} and starts the asynchronous initialization
     * action, which is completed in {@link #startRunningIfQueueEmpty()}.
     *
     * <p>This method starts one asynchronous action for every in-port in {@link #recomputedInPorts}. These asynchronous
     * actions are finished in {@link #inPortHasSignal(int)}.
     */
    @Override
    public void preStart() {
        assert state == State.STARTING;
        assert !requestedOutPorts.isEmpty();
        // Another precondition: getPortState() is IRRELEVANT for all nodes in the dependency graph.

        publishStartModule();

        // Start an asynchronous action for each in-port that is promised to receive a value
        recomputedInPorts.stream()
            .mapToObj(module.getInPorts()::get)
            .forEach(inPort -> startAsynchronousAction(inPort, "waiting for %s to receive value", inPort));

        // Run algorithm ComputeResumeState
        computeResumeState.run();
        startRunningIfQueueEmpty();
    }

    @Override
    public void postStop() {
        publishStopModule(state == State.DONE);
    }

    /**
     * Callback for {@link ComputeResumeState} in order to determine whether a dependency-graph node has a value or not.
     *
     * <p>This method is guaranteed to be called during processing of a message this actor received (in particular,
     * on the same thread).
     */
    private void asynchronousHasValueCheck(ValueNode valueNode) {
        ExecutionTrace executionTrace = valueNode.getExecutionTrace();
        CompletableFuture<Object> messageFuture = stagingArea.exists(executionTrace)
            .thenApply(exists -> new FinishedStagingAreaOperation(valueNode, exists));
        pipeResultToSelf(messageFuture, "checking if staging area contains execution trace '%s'",
            executionTrace);
    }

    /**
     * Handles event that a future returned by {@link StagingArea#exists(RuntimeExecutionTrace)} (called by
     * {@link #asynchronousHasValueCheck(ValueNode)}) was completed successfully.
     *
     * @param node node in the dependency graph
     * @param hasValue whether the node in the dependency graph has a value
     */
    private void finishedStagingAreaOperation(ValueNode node, boolean hasValue) {
        computeResumeState.updateHasValue(node, hasValue);
        startRunningIfQueueEmpty();
    }

    /**
     * Handles event that an out-port of the module represented by this actor received a value.
     *
     * <p>If a value for the given out-port is not needed (for instance, because it was not requested when this actor
     * was started), this method is a no-op. Otherwise, a {@link SubmoduleOutPortHasSignal} message is sent to the
     * parent actor.
     *
     * @param outPortId index of the out-port in {@link RuntimeModule#getOutPorts()}
     */
    private void outportCarriesSignal(int outPortId) {
        assert outPortsRequiringValue.get(outPortId) : "out-port cannot receive value twice";
        assert requestedOutPorts.get(outPortId)
            : "only requested out-ports have incoming connections in the dependency graph";

        getContext().parent().tell(new SubmoduleOutPortHasSignal(getModuleId(), outPortId), getSelf());

        outPortsRequiringValue.set(outPortId, false);
        if (outPortsRequiringValue.isEmpty()) {
            state = State.ALL_OUTPUTS;
        }
    }

    /**
     * Handles event that a child actor terminated.
     *
     * <p>This event finished the asynchronous action started by {@link #getChildExecutor(RuntimeModule)}.
     *
     * @param childActor actor reference of child actor
     */
    private void childActorTerminated(ActorRef childActor) {
        if (childActorMap.remove(childActor) != null) {
            endAsynchronousAction(childActor);
        } else {
            log.warning(String.format(
                "Ignoring unexpected %s message for unknown child actor %s.",
                Terminated.class.getSimpleName(), childActor
            ));
        }
    }

    /**
     * Returns, and if necessary creates, the submodule interpreter actor for the given submodule.
     *
     * <p>A submodule interpreter actor is created at most once during the lifetime of the current actor. If the
     * submodule actor is created, it will be supervised by the current actor.
     *
     * <p>This method starts one asynchronous action for each out-port of the submodule that satisfies
     * {@link #isOutPortNeeded(RuntimeOutPort)}; these will be finished in {@link #submoduleOutPortNoLongerNeeded}.
     * This prevents this actor from terminating even though a child interpreter may still send values for out-ports.
     *
     * <p>One additional asynchronous action is started for the child actor, which will be finished in
     * {@link #childActorTerminated(ActorRef)}.
     *
     * @param submodule submodule
     * @return reference to the submodule actor
     */
    private ActorRef getChildExecutor(RuntimeModule submodule) {
        assert state == State.REPLAY || state == State.RUNNING || state == State.ALL_OUTPUTS;

        int submoduleId = submodule.getIndex();
        if (childExecutors[submoduleId] == null) {
            List<SubmoduleInPortNode> submoduleInPortNodes = dependencyGraph.submodulesInPortNodes().get(submoduleId);

            ImmutableList<HasValue> submoduleInPortHasValueList = submoduleInPortNodes.stream()
                .map(SubmoduleInPortNode::getHasValue)
                .collect(ImmutableList.collector());

            BitSet submoduleRecomputedInPorts = new BitSet(submodule.getInPorts().size());
            submoduleInPortNodes.stream()
                .filter(node -> node.getPortState() == PortState.RECOMPUTE)
                .forEach(node -> submoduleRecomputedInPorts.set(node.getElement().getInIndex()));

            BitSet submoduleRequestedOutPorts = computeResumeState.getSubmodulesNeededOutPorts()[submoduleId];
            submoduleRequestedOutPorts.stream()
                .mapToObj(id -> submodule.getOutPorts().get(id))
                .forEach(
                    outPort -> startAsynchronousAction(
                        outPort, "waiting for value to pass through submodule out-port %s#%s",
                        submodule.getSimpleName(), outPort.getSimpleName()
                    )
                );

            childExecutors[submoduleId] = getContext().actorOf(
                interpreterPropsProvider.provideInterpreterProps(
                    getInterpreterProperties(),
                    stagingArea.resolveDescendant(
                        ExecutionTrace.empty().resolveContent().resolveModule(submodule.getSimpleName())
                    ),
                    submoduleId,
                    submoduleInPortHasValueList,
                    submoduleRecomputedInPorts,
                    submoduleRequestedOutPorts
                ),
                submodule.getSimpleName().toString()
            );
            getContext().watch(childExecutors[submoduleId]);
            childActorMap.put(childExecutors[submoduleId], submodule);
            startAsynchronousAction(childExecutors[submoduleId], "supervising interpreter for submodule %s",
                submodule.getSimpleName());
        }
        return childExecutors[submoduleId];
    }

    /**
     * Visitor of connections that is called when the source port has a new value.
     *
     * <p>This visitor is only called while a message is being processed. It is never called from a callback. Therefore,
     * it is always safe to modify fields of the actor instance.
     *
     * <p>This visitor copies the value to the target port of the connection and then sends a message to the appropriate
     * entity; that is, either to the submodule (if the connection is parent-in-to-child-in or sibling) or this actor
     * (if the connection is short-circuit or child-out-to-parent-out).
     *
     * <p>The visitor returns a future that will be completed with the target execution trace.
     */
    private final class TransmitFromSourcePortVisitor
            implements RuntimeConnectionVisitor<CompletableFuture<Void>, RuntimePort> {
        /**
         * Returns a future representing the staging-area copy operation (which will send the given message upon
         * completion).
         *
         * <p>Strictly speaking, it would not be necessary for parent-in-to-child-in or sibling connections to start an
         * asynchronous action (by calling {@link #pipeResultToSelf}), because every child actor started by
         * {@link #getChildExecutor(RuntimeModule)} is in itself an asynchronous action that ends when the child actor
         * terminates and {@link #childActorTerminated(ActorRef)} is called.
         *
         * <p>For simplicity, however, this method always starts an asynchronous action.
         */
        private CompletableFuture<Void> startCopyAndSendMessage(ExecutionTrace copyFromExecutionTrace,
                ExecutionTrace copyToExecutionTrace, final Object message, final ActorRef messageTarget) {
            CompletableFuture<Void> copyFuture = stagingArea
                .copy(copyFromExecutionTrace, copyToExecutionTrace)
                .thenApply(ignored -> {
                    messageTarget.tell(message, getSelf());
                    return null;
                });
            awaitAsynchronousAction(copyFuture, "copying from '%s' to '%s'", copyFromExecutionTrace,
                copyToExecutionTrace);
            return copyFuture;
        }

        private CompletableFuture<Void> triggerSubmodule(RuntimeConnection connection,
                ExecutionTrace copyFromExecutionTrace) {
            RuntimeInPort toPort = (RuntimeInPort) connection.getToPort();
            RuntimeModule toModule = toPort.getModule();

            ExecutionTrace copyToExecutionTrace = ExecutionTrace.empty().resolveContent()
                .resolveModule(toModule.getSimpleName()).resolveInPort(toPort.getSimpleName());
            Object message = new InPortHasSignal(toPort.getInIndex());
            ActorRef messageTarget = getChildExecutor(toModule);

            return startCopyAndSendMessage(copyFromExecutionTrace, copyToExecutionTrace, message, messageTarget);
        }

        @Override
        public CompletableFuture<Void> visitSiblingConnection(RuntimeSiblingConnection connection,
                @Nullable RuntimePort fromPort) {
            assert fromPort != null;
            return triggerSubmodule(
                connection,
                ExecutionTrace.empty().resolveContent()
                    .resolveModule(fromPort.getModule().getSimpleName()).resolveOutPort(fromPort.getSimpleName())
            );
        }

        @Override
        public CompletableFuture<Void> visitParentInToChildInConnection(
                RuntimeParentInToChildInConnection connection, @Nullable RuntimePort fromPort) {
            assert fromPort != null;
            return triggerSubmodule(connection, ExecutionTrace.empty().resolveInPort(fromPort.getSimpleName()));
        }

        private CompletableFuture<Void> triggerSelf(RuntimeConnection connection,
                ExecutionTrace copyFromExecutionTrace) {
            RuntimeOutPort toPort = (RuntimeOutPort) connection.getToPort();
            ExecutionTrace copyToExecutionTrace = ExecutionTrace.empty().resolveOutPort(toPort.getSimpleName());

            Object message = new OutPortHasSignal(toPort.getOutIndex());
            ActorRef messageTarget = getSelf();

            return startCopyAndSendMessage(copyFromExecutionTrace, copyToExecutionTrace, message, messageTarget);
        }

        @Override
        public CompletableFuture<Void> visitChildOutToParentOutConnection(
                RuntimeChildOutToParentOutConnection connection, @Nullable RuntimePort fromPort) {
            assert fromPort != null;
            return triggerSelf(
                connection,
                ExecutionTrace.empty().resolveContent()
                    .resolveModule(fromPort.getModule().getSimpleName()).resolveOutPort(fromPort.getSimpleName())
            );
        }

        @Override
        public CompletableFuture<Void> visitShortCircuitConnection(RuntimeShortCircuitConnection connection,
                @Nullable RuntimePort fromPort) {
            assert fromPort != null;
            return triggerSelf(connection, ExecutionTrace.empty().resolveInPort(fromPort.getSimpleName()));
        }
    }

    /**
     * Instance of {@link TransmitFromSourcePortVisitor} that is called when either a submodule's out-port receives a
     * value or when an in-port receives a value.
     */
    private final TransmitFromSourcePortVisitor transmitFromSourcePortVisitor = new TransmitFromSourcePortVisitor();

    /**
     * Handles event that a submodule's out-port received a value.
     *
     * <p>This method starts copying the value to all ports that are connected via an outgoing connection. Once all
     * copy operations are completed (asynchronously), a {@link SubmoduleOutPortNoLongerNeeded} message will be send to
     * this actor.
     *
     * <p>This method does not finish any asynchronous actions. Instead, the asynchronous action waiting for the
     * submodule out-port is finished in {@link #submoduleOutPortNoLongerNeeded(RuntimeOutPort)}.
     *
     * @param moduleId index of the module in {@link RuntimeParentModule#getModules()} that contains the out-port
     * @param outPortId index of the out-port in {@link RuntimeModule#getOutPorts()}
     */
    private void submoduleOutPortHasSignal(int moduleId, int outPortId) {
        assert state.compareTo(State.RUNNING) >= 0;

        final RuntimeOutPort outPort = module.getModules().get(moduleId).getOutPorts().get(outPortId);

        if (submoduleOutPortsReceivedMessage[moduleId].get(outPortId)) {
            log.warning(String.format(
                "Ignoring redundant message that %s of submodule '%s' provided a value.",
                outPort, outPort.getModule().getSimpleName()
            ));
        } else if (isOutPortNeeded(outPort)) {
            submoduleOutPortsReceivedMessage[moduleId].set(outPortId);
            List<CompletableFuture<Void>> copyFutures = outPort.getOutConnections().stream()
                .filter(this::isConnectionToRecomputedNode)
                .map(connection -> connection.accept(transmitFromSourcePortVisitor, outPort))
                .collect(Collectors.toList());
            CompletableFuture<Object> messageFuture = Futures.collect(copyFutures)
                .thenApply(ignored -> new SubmoduleOutPortNoLongerNeeded(outPort));
            pipeResultToSelf(messageFuture, "copying value of %s of submodule to out-connections",
                outPort, outPort.getSimpleName());
        } else {
            log.warning(String.format(
                "Ignoring unexpected message that %s of submodule '%s' provided a value because it is not needed.",
                outPort, outPort.getSimpleName()
            ));
        }
    }

    /**
     * Returns whether the given connection ends in a port that is a node in the dependency graph with state
     * {@link PortState#RECOMPUTE}.
     *
     * <p>This method always returns false if the connection is not within the current parent module.
     */
    private boolean isConnectionToRecomputedNode(RuntimeConnection runtimeConnection) {
        if (runtimeConnection.getParentModule() == module) {
            DependencyGraphNode portNode = dependencyGraph.targetNode(runtimeConnection.getToPort());
            if (portNode.getPortState() == PortState.RECOMPUTE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Triggers the given in-port by sending the port value to all out-connections.
     *
     * @param inPort the in-port to trigger
     */
    private void transmitValueFromInPort(RuntimeInPort inPort) {
        assert state == State.REPLAY || state == State.RUNNING || state == State.ALL_OUTPUTS;
        assert inPort.getModule() == module;

        inPort.getOutConnections()
            .stream().filter(this::isConnectionToRecomputedNode)
            .forEach(connection -> connection.accept(transmitFromSourcePortVisitor, inPort));
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the current state of this actor is still {@link State#STARTING}, the message will be queued. It will be
     * handled in {@link #startRunningIfQueueEmpty()} when the state is changed to {@link State#RUNNING}.
     *
     * <p>If the current state is {@link State#RUNNING}, this method finishes the asynchronous action started for the
     * given port in {@link #preStart()}.
     *
     * @param inPortId index of the in-port in {@link RuntimeModule#getInPorts()}
     */
    @Override
    void inPortHasSignal(int inPortId) {
        if (state != State.REPLAY && inPortReceivedMessage.get(inPortId)) {
            log.warning(String.format(
                "Ignoring redundant message that %s received value.",
                module.getInPorts().get(inPortId)
            ));
        } else if (!recomputedInPorts.get(inPortId)) {
            log.warning(String.format(
                "Ignoring unexpected message that %s received a value. "
                    + "In-ports that are expected to receive a value: %s",
                module.getInPorts().get(inPortId),
                recomputedInPorts.stream()
                    .mapToObj(module.getInPorts()::get)
                    .map(RuntimeInPort::getSimpleName)
                    .collect(Collectors.toList())
            ));
        } else {
            inPortReceivedMessage.set(inPortId);
            if (state != State.STARTING) {
                RuntimeInPort inPort = module.getInPorts().get(inPortId);
                transmitValueFromInPort(inPort);
                // Finishes the asynchronous action started in #preStart()
                endAsynchronousAction(inPort);
            }
        }
    }

    /**
     * Handles event that the value of a submodule's out-port is no longer needed.
     *
     * <p>If the submodule of the given out-port no longer has any (other) out-port whose value is required, this method
     * starts (asynchronously) cleaning all intermediate results for this module.
     *
     * <p>This method also finishes the asynchronous action (waiting for submodule out-port) started previously in
     * {@link #getChildExecutor}.
     *
     * @param outPort out-port whose value is no longer needed
     */
    private void submoduleOutPortNoLongerNeeded(RuntimeOutPort outPort) {
        assert state.compareTo(State.RUNNING) >= 0;

        RuntimeModule submodule = outPort.getModule();
        BitSet neededOutPorts = computeResumeState.getSubmodulesNeededOutPorts()[submodule.getIndex()];
        neededOutPorts.set(outPort.getOutIndex(), false);
        if (neededOutPorts.isEmpty() && getInterpreterProperties().isCleaningRequested()) {
            ExecutionTrace executionTrace
                = ExecutionTrace.empty().resolveContent().resolveModule(submodule.getSimpleName());
            CompletableFuture<Void> future = stagingArea.delete(executionTrace);
            awaitAsynchronousAction(future, "cleaning up intermediate output of submodule '%s'",
                submodule.getSimpleName());
        }

        endAsynchronousAction(outPort);
    }

    @Override
    void onEmptySetOfAsynchronousActions() {
        if (state == State.ALL_OUTPUTS) {
            state = State.DONE;
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    @Override
    InterpreterException mapChildException(Exception exception) {
        ActorRef childActor = getSender();
        @Nullable RuntimeModule childModule = childActorMap.get(childActor);
        InterpreterException interpreterException;
        if (childModule != null) {
            RuntimeExecutionTrace failedTrace
                = getAbsoluteTrace().resolveContent().resolveModule(childModule.getSimpleName());
            interpreterException = new InterpreterException(
                failedTrace,
                String.format("An exception occurred while interpreting %s.", childModule),
                exception
            );
        } else {
            interpreterException = super.mapChildException(exception);
        }
        return interpreterException;
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message instanceof FinishedStagingAreaOperation) {
            FinishedStagingAreaOperation arguments = (FinishedStagingAreaOperation) message;
            finishedStagingAreaOperation(arguments.node, arguments.hasValue);
        } else if (message instanceof OutPortHasSignal) {
            outportCarriesSignal(((OutPortHasSignal) message).outPortId);
        } else if (message instanceof SubmoduleOutPortHasSignal) {
            SubmoduleOutPortHasSignal arguments = (SubmoduleOutPortHasSignal) message;
            submoduleOutPortHasSignal(arguments.getModuleId(), arguments.getOutPortId());
        } else if (message instanceof SubmoduleOutPortNoLongerNeeded) {
            submoduleOutPortNoLongerNeeded(((SubmoduleOutPortNoLongerNeeded) message).outPort);
        } else if (message instanceof Terminated) {
            childActorTerminated(((Terminated) message).getActor());
        } else {
            super.onReceive(message);
        }
    }

    private static final class FinishedStagingAreaOperation {
        private final ValueNode node;
        private final boolean hasValue;

        private FinishedStagingAreaOperation(ValueNode node, boolean hasValue) {
            this.node = node;
            this.hasValue = hasValue;
        }
    }

    private static final class OutPortHasSignal {
        private final int outPortId;

        OutPortHasSignal(int outPortId) {
            this.outPortId = outPortId;
        }
    }

    private static final class SubmoduleOutPortNoLongerNeeded {
        private final RuntimeOutPort outPort;

        private SubmoduleOutPortNoLongerNeeded(RuntimeOutPort outPort) {
            this.outPort = outPort;
        }
    }

    /**
     * Returns the child executor for the submodule with the given ID.
     *
     * <p>This method is only intended for unit/integration tests.
     */
    ActorRef getChildExecutor(int submoduleId) {
        return childExecutors[submoduleId];
    }
}
