package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.bare.element.module.BareLoopModule;
import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeIOPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeLoopModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import net.florianschoppmann.java.futures.Futures;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Interpreter of loop modules.
 */
final class LoopModuleInterpreterActor extends AbstractModuleInterpreterActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);

    private final RuntimeLoopModule module;
    private final LocalInterpreterProperties interpreterProperties;
    private final StagingArea stagingArea;
    private final InterpreterPropsProvider interpreterPropsProvider;

    /**
     * Set of all in-ports for that this actor will receive a {@link InterpreterInterface.InPortHasSignal} message.
     *
     * <p>This set is never modified.
     */
    private final BitSet recomputedInPorts;

    /**
     * Set of all out-ports for that a {@link InterpreterInterface.SubmoduleOutPortHasSignal} needs to be sent.
     *
     * <p>This set is never modified.
     */
    private final BitSet requestedOutPorts;

    /**
     * Set of all in-ports that need to have a value so that a new iteration can be started.
     *
     * <p>This set is never modified.
     */
    private final BitSet requiredInPortsForIterations;

    /**
     * Set of all out-ports for which an {@link InterpreterInterface.SubmoduleOutPortHasSignal} is expected from the
     * per-iteration interpreter.
     *
     * <p>This set is never modified.
     */
    private final BitSet neededOutPortsForIterations;

    /**
     * Set of all in-ports that still require to receive a value.
     *
     * <p>This set is modified whenever an in-port receives a value.
     */
    private final BitSet inPortsRequiringValue;

    /**
     * Set of all in-ports for which this actor received an {@link InterpreterInterface.InPortHasSignal} message
     * (handled in {@link #inPortHasSignal(int)}) and then subsequently copied the input to the execution trace
     * corresponding to the first iteration (see {@link #copiedInPortToFirstIteration(int)}), all while the iteration
     * child actor had not yet been started by {@link #startChildExecutor(Index)}.
     */
    private final BitSet inPortsCopiedToFirstIteration;

    /**
     * Iteration from which interpretation was started.
     *
     * <p>This value is modified only during state {@link State#FIND_NEXT_ITERATION}, and becomes effectively immutable
     * after that.
     */
    @Nullable private Index startIteration = null;

    /**
     * The iteration the child actor {@link #iterationActorRef} is currently interpreting.
     */
    @Nullable private Index currentIteration = null;

    /**
     * Reference to actor that interprets the current iteration.
     */
    @Nullable private ActorRef iterationActorRef = null;

    private enum State {
        /**
         * Finding out from where to resume. No child interpreter has been started yet.
         */
        FIND_NEXT_ITERATION,

        /**
         * Currently interpreting the first iteration.
         *
         * <p>The first iteration is special because the iteration child actor is started before all inputs are
         * available.
         */
        FIRST_ITERATION,

        /**
         * Running normally, child interpreters are running and invoked whenever a new signal arrives.
         */
        RUNNING,

        /**
         * Produced all outputs and notified parent executor about it.
         *
         * <p>This means this actor is ready to terminate itself, and no failure occurred.
         */
        DONE
    }
    private State state = State.FIND_NEXT_ITERATION;

    /**
     * Factory for creating a loop-module interpreter.
     *
     * <p>This class is not meant to be serialized because actor creators for input-module interpreters will only be
     * used within the same JVM. Any attempt to serialize a factory will cause a {@link NotSerializableException}.
     */
    static final class Factory implements Creator<UntypedActor> {
        private static final long serialVersionUID = -6754845251933067995L;

        private final RuntimeLoopModule module;
        private final LocalInterpreterProperties interpreterProperties;
        private final StagingArea stagingArea;
        private final InterpreterPropsProvider interpreterPropsProvider;
        private final int moduleId;
        private final BitSet recomputedInPorts;
        private final BitSet requestedOutPorts;

        Factory(LocalInterpreterProperties interpreterProperties, StagingArea stagingArea,
                InterpreterPropsProvider interpreterPropsProvider, int moduleId,
                BitSet recomputedInPorts, BitSet requestedOutPorts) {
            Objects.requireNonNull(interpreterProperties);
            Objects.requireNonNull(stagingArea);
            Objects.requireNonNull(interpreterPropsProvider);
            Objects.requireNonNull(recomputedInPorts);
            Objects.requireNonNull(requestedOutPorts);

            this.interpreterProperties = interpreterProperties;
            this.stagingArea = stagingArea;
            module = (RuntimeLoopModule) stagingArea.getAnnotatedExecutionTrace().getModule();
            this.interpreterPropsProvider = interpreterPropsProvider;
            this.moduleId = moduleId;
            this.recomputedInPorts = (BitSet) recomputedInPorts.clone();
            this.requestedOutPorts = (BitSet) requestedOutPorts.clone();
        }

        private void readObject(ObjectInputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        @Override
        public UntypedActor create() {
            return new LoopModuleInterpreterActor(this);
        }
    }

    LoopModuleInterpreterActor(Factory factory) {
        super(factory.interpreterProperties, factory.stagingArea.getAnnotatedExecutionTrace(), factory.moduleId);

        module = factory.module;
        stagingArea = factory.stagingArea;
        interpreterProperties = factory.interpreterProperties;
        interpreterPropsProvider = factory.interpreterPropsProvider;
        recomputedInPorts = factory.recomputedInPorts;
        requestedOutPorts = factory.requestedOutPorts;

        List<? extends RuntimeOutPort> outPorts = module.getOutPorts();
        neededOutPortsForIterations = (BitSet) requestedOutPorts.clone();
        for (RuntimeOutPort outPort: outPorts) {
            if (outPort instanceof RuntimeIOPort) {
                neededOutPortsForIterations.set(outPort.getOutIndex());
            }
        }
        neededOutPortsForIterations.set(module.getContinuePort().getOutIndex());

        List<? extends RuntimeInPort> inPorts = module.getInPorts();
        requiredInPortsForIterations = new BitSet(inPorts.size());
        for (
            int outPortId = neededOutPortsForIterations.nextSetBit(0);
            outPortId >= 0;
            outPortId = neededOutPortsForIterations.nextSetBit(outPortId + 1)
        ) {
            for (RuntimeInPort inPort: outPorts.get(outPortId).getInPortDependencies()) {
                requiredInPortsForIterations.set(inPort.getInIndex());
            }
        }

        inPortsRequiringValue = (BitSet) requiredInPortsForIterations.clone();
        inPortsCopiedToFirstIteration = new BitSet(inPorts.size());
    }

    /**
     * Handles event that an in-port value was copied to the respective execution trace of the first iteration.
     *
     * <p>If the first-iteration interpreter is not yet running, this method caches the information that the in-port
     * value has been copied in field {@link #inPortsCopiedToFirstIteration}.
     *
     * @param inPortId index of in-port in the list returned by {@link RuntimeLoopModule#getInPorts()}
     */
    private void copiedInPortToFirstIteration(int inPortId) {
        assert state == State.FIRST_ITERATION || state == State.RUNNING;

        if (iterationActorRef == null) {
            inPortsCopiedToFirstIteration.set(inPortId);
        } else {
            iterationActorRef.tell(new InterpreterInterface.InPortHasSignal(inPortId), getSelf());
        }
    }

    @Override
    void onEmptySetOfAsynchronousActions() { }

    @Override
    void inPortHasSignal(final int inPortId) {
        if (state == State.FIRST_ITERATION && inPortsRequiringValue.get(inPortId)) {
            inPortsRequiringValue.set(inPortId, false);

            RuntimeInPort inPort = module.getInPorts().get(inPortId);
            SimpleName inPortName = inPort.getSimpleName();
            ExecutionTrace fromTrace = ExecutionTrace.empty().resolveInPort(inPortName);
            ExecutionTrace toTrace
                = ExecutionTrace.empty().resolveContent().resolveIteration(Index.index(0)).resolveInPort(inPortName);
            CompletableFuture<Object> messageFuture = stagingArea
                .copy(fromTrace, toTrace)
                .thenApply(ignored -> new CopiedInPortToFirstIteration(inPortId));
            pipeResultToSelf(messageFuture, "copying value for in-port %s to first iteration",
                inPort.getSimpleName());
        } else {
            log.warning(String.format(
                "Ignoring message that %s received value while in state %s.",
                module.getInPorts().get(inPortId), state
            ));
        }
    }

    private void findNextResumeCandidate() {
        CompletableFuture<Object> messageFuture = stagingArea
            .getMaximumIndex(ExecutionTrace.empty().resolveContent(), startIteration)
            .thenApply(
                optionalMaxIteration -> optionalMaxIteration.isPresent()
                    ? new FoundIterationInStagingArea(optionalMaxIteration.get())
                    : new IterationStatus(Index.index(0), false)
            );
        pipeResultToSelf(messageFuture, "finding iteration to resume execution from");
    }

    @Override
    public void preStart() {
        assert state == State.FIND_NEXT_ITERATION;

        publishStartModule();
        if (recomputedInPorts.isEmpty()) {
            findNextResumeCandidate();
        } else {
            state = State.FIRST_ITERATION;
            startFirstIteration();
        }
    }

    @Override
    public void postStop() {
        publishStopModule(state == State.DONE);
    }

    private void startFirstIteration() {
        assert state == State.FIRST_ITERATION;

        final Index firstIteration = Index.index(0);
        List<? extends RuntimeInPort> inPorts = module.getInPorts();

        BitSet copyableInPorts = (BitSet) requiredInPortsForIterations.clone();
        copyableInPorts.andNot(recomputedInPorts);

        List<CompletableFuture<Void>> futures = new ArrayList<>(copyableInPorts.cardinality());
        for (
            int inPortId = copyableInPorts.nextSetBit(0);
            inPortId >= 0;
            inPortId = copyableInPorts.nextSetBit(inPortId + 1)
        ) {
            final SimpleName inPortName = inPorts.get(inPortId).getSimpleName();
            final RuntimeExecutionTrace iterationInPortTrace
                = ExecutionTrace.empty().resolveIteration(firstIteration).resolveInPort(inPortName);
            CompletableFuture<Void> future = stagingArea.exists(iterationInPortTrace)
                .thenCompose(
                    exists -> exists
                        ? CompletableFuture.completedFuture(null)
                        : stagingArea.copy(ExecutionTrace.empty().resolveInPort(inPortName), iterationInPortTrace)
                );
            futures.add(future);
        }
        CompletableFuture<Object> messageFuture = Futures.collect(futures)
            .thenApply(ignored -> new IterationStatus(firstIteration, true));
        pipeResultToSelf(messageFuture, "starting first loop iteration (index 0)");
    }

    /**
     * Handles event that the staging area contains an entry in the staging area that could potentially have values for
     * all required in-ports.
     *
     * <p>This method verifies (asynchronously) that indeed values for all required in-ports are present. The
     * asynchronous action started in this method will be finished in {@link #iterationStatus(Index, boolean)}.
     *
     * <p>This method itself finishes the asynchronous action started previously in {@link #findNextResumeCandidate()}.
     *
     * @param iteration iteration that has an entry in the staging area
     */
    private void foundIterationInStagingArea(final Index iteration) {
        assert state == State.FIND_NEXT_ITERATION;

        startIteration = iteration;

        List<? extends RuntimeInPort> inPorts = module.getInPorts();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(module.getInPorts().size());
        for (
            int inPortId = requiredInPortsForIterations.nextSetBit(0);
            inPortId >= 0;
            inPortId = requiredInPortsForIterations.nextSetBit(inPortId + 1)
        ) {
            futures.add(stagingArea.exists(
                ExecutionTrace.empty().resolveIteration(iteration).resolveInPort(inPorts.get(inPortId).getSimpleName())
            ));
        }
        CompletableFuture<Object> messageFuture = Futures.collect(futures)
            .thenApply(hasValuesList -> {
                boolean hasAllInputs = true;
                for (boolean hasValue: hasValuesList) {
                    if (!hasValue) {
                        hasAllInputs = false;
                        break;
                    }
                }
                return new IterationStatus(iteration, hasAllInputs);
            });
        pipeResultToSelf(messageFuture, "examining whether interpretation can be resumed from iteration %s",
            iteration);
    }

    private ActorRef startChildExecutor(Index iteration) {
        assert EnumSet.of(State.FIRST_ITERATION, State.FIND_NEXT_ITERATION, State.RUNNING).contains(state);
        assert iterationActorRef == null && currentIteration == null;

        BitSet iterationRecomputedInPorts;
        BitSet inPortsInNeedOfMessage;
        if (state == State.FIRST_ITERATION) {
            iterationRecomputedInPorts = recomputedInPorts;
            inPortsInNeedOfMessage = inPortsCopiedToFirstIteration;
        } else if (state == State.FIND_NEXT_ITERATION) {
            iterationRecomputedInPorts = new BitSet();
            inPortsInNeedOfMessage = iterationRecomputedInPorts;
        } else {
            iterationRecomputedInPorts = requiredInPortsForIterations;
            inPortsInNeedOfMessage = iterationRecomputedInPorts;
        }
        iterationActorRef = getContext().actorOf(
            interpreterPropsProvider.provideInterpreterProps(
                interpreterProperties,
                stagingArea.resolveDescendant(
                    ExecutionTrace.empty().resolveContent().resolveIteration(iteration)
                ),
                0,
                Collections.nCopies(module.getInPorts().size(), DependencyGraph.HasValue.UNKNOWN),
                iterationRecomputedInPorts,
                neededOutPortsForIterations
            ),
            iteration.toString()
        );
        assert iterationActorRef != null;
        currentIteration = iteration;
        getContext().watch(iterationActorRef);

        for (
            int inPortId = inPortsInNeedOfMessage.nextSetBit(0);
            inPortId >= 0;
            inPortId = inPortsInNeedOfMessage.nextSetBit(inPortId + 1)
        ) {
            iterationActorRef.tell(new InterpreterInterface.InPortHasSignal(inPortId), getSelf());
        }

        return iterationActorRef;
    }

    /**
     * Handles event that it is now known whether the given iteration has all inputs.
     *
     * <p>This method starts the iteration interpreter if all inputs for the given iteration are available. Otherwise,
     * if the given iteration number is larger than 1, it tries to find (asynchronously) a previous iteration which
     * appears in the staging area. Otherwise (if the given iteration number is 1 or less), his method starts the first
     * iteration by calling {@link #startFirstIteration()}.
     *
     * This method finishes the asynchronous action previously started in
     * {@link #foundIterationInStagingArea(Index)}.
     *
     * @param iteration iteration that has an entry in the staging area
     * @param hasAllInputs {@code true} if the staging area also has values for all required in-ports, {@code false} if
     *     not
     */
    private void iterationStatus(Index iteration, boolean hasAllInputs) {
        if (hasAllInputs) {
            startChildExecutor(iteration);
        } else {
            int iterationInt = iteration.intValue();
            if (iterationInt > 1) {
                assert state == State.FIND_NEXT_ITERATION;

                startIteration = Index.index(iterationInt - 1);
                findNextResumeCandidate();
            } else {
                startIteration = Index.index(0);
                state = State.FIRST_ITERATION;
                startFirstIteration();
            }
        }
    }

    /**
     * Starts copying the values for all required in-ports for the given iteration.
     *
     * <p>If an in-port is actually an I/O-port, then the value is copied from the previous iteration. Otherwise, the
     * input value is copied from the values passed to the loop module. The asynchronous action started by this method
     * is finished by {@link #iterationStatus(Index, boolean)}.
     *
     * @param iteration iteration for which the in-port values are copied
     */
    private void copyInputsForIteration(final Index iteration) {
        assert iteration.intValue() > 0;

        int iterationInt = iteration.intValue();
        Index previousIteration = Index.index(iterationInt - 1);
        List<? extends RuntimeInPort> inPorts = module.getInPorts();
        List<CompletableFuture<Void>> copyFutures = new ArrayList<>(inPorts.size());
        for (
            int inPortId = requiredInPortsForIterations.nextSetBit(0);
            inPortId >= 0;
            inPortId = requiredInPortsForIterations.nextSetBit(inPortId + 1)
        ) {
            RuntimeInPort inPort = inPorts.get(inPortId);
            SimpleName inPortName = inPort.getSimpleName();
            ExecutionTrace copyFrom = inPort instanceof RuntimeIOPort
                ? ExecutionTrace.empty().resolveContent().resolveIteration(previousIteration).resolveOutPort(inPortName)
                : ExecutionTrace.empty().resolveInPort(inPortName);
            ExecutionTrace copyTo = ExecutionTrace.empty().resolveContent().resolveIteration(iteration)
                .resolveInPort(inPortName);

            copyFutures.add(stagingArea.copy(copyFrom, copyTo));
        }
        CompletableFuture<Object> messageFuture = Futures.collect(copyFutures)
            .thenApply(ignored -> new IterationStatus(iteration, true));
        pipeResultToSelf(messageFuture, "copying inputs for iteration %s", iteration);
    }

    /**
     * Handles event that the last iteration yielded value {@code false} for the continue-port.
     *
     * <p>This method starts copying all requested out-ports.
     *
     * @param iteration iteration that yielded value {@code false} for the continue-port
     */
    private void finished(Index iteration) {
        List<? extends RuntimeOutPort> outPorts = module.getOutPorts();
        List<CompletableFuture<Void>> copyFutures = new ArrayList<>(outPorts.size());
        for (
            int outPortId = requestedOutPorts.nextSetBit(0);
            outPortId >= 0;
            outPortId = requestedOutPorts.nextSetBit(outPortId + 1)
        ) {
            SimpleName outPortName = outPorts.get(outPortId).getSimpleName();
            ExecutionTrace copyFrom = ExecutionTrace.empty().resolveContent().resolveIteration(iteration)
                .resolveOutPort(outPortName);
            ExecutionTrace copyTo = ExecutionTrace.empty().resolveOutPort(outPortName);

            CompletableFuture<Void> copyFuture = stagingArea.copy(copyFrom, copyTo);
            int finalOutPortId = outPortId;
            // We send the SubmoduleOutPortHasSignal messages individually because out-port value may vary greatly in
            // size, and copying may thus take widely different amounts of times.
            ActorRef parent = getContext().parent();
            copyFuture.thenRun(
                () -> parent.tell(
                    new InterpreterInterface.SubmoduleOutPortHasSignal(getModuleId(), finalOutPortId),
                    getSelf()
                )
            );
            copyFutures.add(copyFuture);
        }
        CompletableFuture<LocalMessages> messageFuture = Futures.collect(copyFutures)
            .thenApply(ignore -> LocalMessages.PREPARE_TO_TERMINATE);
        pipeResultToSelf(messageFuture, "copying outputs from iteration %s", iteration);
    }

    /**
     * Handles event that a child actor terminated.
     *
     * <p>This method retrieves the value for the loop module's {@code continue} port. If this value is {@code true},
     * a {@link CopyInputsForIteration} message will be sent to this actor, otherwise a {@link FinishedLastIteration}
     * message will be sent.
     *
     * @param childActor actor reference of child actor
     */
    private void childActorTerminated(ActorRef childActor) {
        assert state == State.FIRST_ITERATION || state == State.RUNNING;

        if (childActor == iterationActorRef) {
            state = State.RUNNING;

            // We set the current iteration immediately after child actor was created, hence it must not be null.
            assert currentIteration != null;
            Index finishedIteration = currentIteration;
            iterationActorRef = null;
            currentIteration = null;

            ExecutionTrace continueTrace = ExecutionTrace.empty()
                .resolveContent().resolveIteration(finishedIteration)
                .resolveOutPort(SimpleName.identifier(BareLoopModule.CONTINUE_PORT_NAME));
            CompletableFuture<Object> messageFuture = stagingArea.getObject(continueTrace)
                .thenApply(
                    shouldContinue -> (boolean) shouldContinue
                        ? new CopyInputsForIteration(Index.index(finishedIteration.intValue() + 1))
                        : new FinishedLastIteration(finishedIteration)
                );
            pipeResultToSelf(messageFuture, "examining value at %s", continueTrace);
        } else {
            log.warning(String.format("Ignoring terminated message for unknown child actor %s.", childActor));
        }
    }

    private void prepareToTerminate() {
        state = State.DONE;
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message instanceof IterationStatus) {
            IterationStatus arguments = (IterationStatus) message;
            iterationStatus(arguments.iteration, arguments.hasAllInputs);
        } else if (message instanceof FoundIterationInStagingArea) {
            foundIterationInStagingArea(((FoundIterationInStagingArea) message).iteration);
        } else if (message instanceof CopyInputsForIteration) {
            copyInputsForIteration(((CopyInputsForIteration) message).iteration);
        } else if (message instanceof CopiedInPortToFirstIteration) {
            copiedInPortToFirstIteration(((CopiedInPortToFirstIteration) message).inPortId);
        } else if (message instanceof Terminated) {
            childActorTerminated(((Terminated) message).getActor());
        } else if (message instanceof FinishedLastIteration) {
            finished(((FinishedLastIteration) message).iteration);
        } else if (message == LocalMessages.PREPARE_TO_TERMINATE) {
            prepareToTerminate();
        } else {
            super.onReceive(message);
        }
    }

    private static final class FoundIterationInStagingArea {
        private final Index iteration;

        private FoundIterationInStagingArea(Index iteration) {
            this.iteration = iteration;
        }
    }

    private static final class CopyInputsForIteration {
        private final Index iteration;

        private CopyInputsForIteration(Index iteration) {
            this.iteration = iteration;
        }
    }

    private static final class CopiedInPortToFirstIteration {
        private final int inPortId;

        private CopiedInPortToFirstIteration(int inPortId) {
            this.inPortId = inPortId;
        }
    }

    private static final class FinishedLastIteration {
        private final Index iteration;

        private FinishedLastIteration(Index iteration) {
            this.iteration = iteration;
        }
    }

    enum LocalMessages {
        PREPARE_TO_TERMINATE
    }

    private static final class IterationStatus {
        private final Index iteration;
        private final boolean hasAllInputs;

        private IterationStatus(Index iteration, boolean hasAllInputs) {
            this.iteration = iteration;
            this.hasAllInputs = hasAllInputs;
        }
    }
}
