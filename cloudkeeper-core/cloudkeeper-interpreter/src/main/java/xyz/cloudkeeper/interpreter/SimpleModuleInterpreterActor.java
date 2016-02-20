package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Status;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import xyz.cloudkeeper.interpreter.InterpreterInterface.SubmoduleOutPortHasSignal;
import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.Objects;

/**
 * Executor for simple modules.
 */
final class SimpleModuleInterpreterActor extends AbstractModuleInterpreterActor {
    private final RuntimeProxyModule module;
    private final int moduleId;
    private final StagingArea stagingArea;
    private final ActorRef executor;
    private final BitSet requestedOutPorts;

    private State state = State.WAITING_FOR_INPUTS;
    @Nullable private SimpleModuleExecutorResult result = null;

    private enum State {
        /**
         * Waiting to receive {@link InterpreterInterface.InPortHasSignal} messages for in-ports that still require
         * a value; that is, whose id was contained in {@link Factory#recomputedInPorts}.
         */
        WAITING_FOR_INPUTS,

        /**
         * The simple module has been submitted to the executor.
         */
        RUNNING,

        /**
         * The executor has returned a result.
         */
        DONE
    }

    /**
     * All executors for a single execution id run in a single JVM. Hence, serialization is not an issue.
     */
    static final class Factory implements Creator<UntypedActor> {
        private static final long serialVersionUID = 5801703242566683540L;

        private final LocalInterpreterProperties interpreterProperties;
        private final StagingArea stagingArea;
        private final RuntimeProxyModule proxyModule;
        private final int moduleId;
        private final BitSet recomputedInPorts;
        private final BitSet requestedOutPorts;

        Factory(LocalInterpreterProperties interpreterProperties, StagingArea stagingArea, int moduleId,
                BitSet recomputedInPorts, BitSet requestedOutPorts) {
            Objects.requireNonNull(interpreterProperties);
            Objects.requireNonNull(stagingArea);
            Objects.requireNonNull(recomputedInPorts);
            Objects.requireNonNull(requestedOutPorts);

            this.interpreterProperties = interpreterProperties;
            this.stagingArea = stagingArea;
            proxyModule = (RuntimeProxyModule) stagingArea.getAnnotatedExecutionTrace().getModule();
            this.moduleId = moduleId;
            this.recomputedInPorts = (BitSet) recomputedInPorts.clone();
            this.requestedOutPorts = (BitSet) requestedOutPorts.clone();
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            throw new NotSerializableException(getClass().getName());
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        @Override
        public UntypedActor create() {
            return new SimpleModuleInterpreterActor(this);
        }
    }

    private SimpleModuleInterpreterActor(Factory factory) {
        super(factory.interpreterProperties, factory.stagingArea.getAnnotatedExecutionTrace(), factory.moduleId);

        module = factory.proxyModule;
        moduleId = factory.moduleId;
        stagingArea = factory.stagingArea;
        executor = factory.interpreterProperties.getExecutor();

        factory.recomputedInPorts.stream()
            .forEach(inPortId -> {
                RuntimeInPort inPort = module.getInPorts().get(inPortId);
                startAsynchronousAction(inPort, "waiting for in-port %s", inPort.getSimpleName());
            });

        // requestedOutPorts is never modified, so no need to clone()
        requestedOutPorts = factory.requestedOutPorts;
    }

    @Override
    void onEmptySetOfAsynchronousActions() {
        if (state == State.WAITING_FOR_INPUTS) {
            LocalInterpreterProperties interpreterProperties = getInterpreterProperties();
            long executionId = interpreterProperties.getExecutionId();
            RuntimeStateProvider runtimeStateProvider
                = RuntimeStateProvider.of(interpreterProperties.getRuntimeContext(), stagingArea);
            executor.tell(new ExecutorActorInterface.ExecuteTrace(executionId, runtimeStateProvider), getSelf());
            state = State.RUNNING;
        } else {
            throw new IllegalStateException(String.format("Asynchronous action ended while in state %s.", state));
        }
    }

    @Override
    void inPortHasSignal(int inPortId) {
        endAsynchronousAction(module.getInPorts().get(inPortId));
    }

    private void failure(Throwable throwable) throws InterpreterException {
        throw throwable instanceof InterpreterException
            ? (InterpreterException) throwable
            : new InterpreterException(getAbsoluteTrace(), "Executor failed unexpectedly.", throwable);
    }

    private void executionFinished(SimpleModuleExecutorResult executorResult) throws InterpreterException {
        assert state == State.RUNNING;

        state = State.DONE;
        result = executorResult;

        @Nullable ExecutionException executionException = executorResult.getExecutionException();
        if (executionException != null) {
            failure(new InterpreterException(
                getAbsoluteTrace(),
                String.format("Exception while executing %s.", module),
                executionException
            ));
        }

        ActorRef parent = getContext().parent();
        module.getOutPorts().stream()
            .filter(outPort -> requestedOutPorts.get(outPort.getOutIndex()))
            .forEach(outPort -> parent.tell(new SubmoduleOutPortHasSignal(moduleId, outPort.getOutIndex()), getSelf()));

        // We are done, so we can stop ourselves now.
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    @Override
    public void preStart() {
        publishStartModule();
        checkIfNoAsynchronousActions();
    }

    @Override
    public void postStop() {
        // If result != null, then necessarily state == State.DONE (only set in executionFinished)
        assert result == null || state == State.DONE;
        publishStopSimpleModule(result);

        if (state != State.DONE) {
            // We terminated early. We are a good citizen and notify the executor.
            executor.tell(
                new ExecutorActorInterface.CancelExecution("Simple-module interpreter terminated."),
                getSelf()
            );
        }
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message instanceof SimpleModuleExecutorResult) {
            executionFinished((SimpleModuleExecutorResult) message);
        } else if (message instanceof Status.Failure) {
            failure(((Status.Failure) message).cause());
        } else {
            super.onReceive(message);
        }
    }
}
