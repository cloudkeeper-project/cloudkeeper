package com.svbio.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * Interpreter for input modules.
 */
final class InputModuleInterpreterActor extends AbstractModuleInterpreterActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);

    private final RuntimeInputModule inputModule;
    private final StagingArea staging;
    private boolean success = false;

    /**
     * Factory for creating an input-module interpreter.
     *
     * <p>This class is not meant to be serialized because actor creators for input-module interpreters will only be
     * used within the same JVM. Any attempt to serialize a factory will cause a {@link NotSerializableException}.
     */
    static final class Factory implements Creator<UntypedActor> {
        private static final long serialVersionUID = 3655104041838113239L;

        private final LocalInterpreterProperties interpreterProperties;
        private final RuntimeInputModule inputModule;
        private final int moduleId;
        private final StagingArea stagingArea;

        Factory(LocalInterpreterProperties interpreterProperties, StagingArea stagingArea, int moduleId) {
            Objects.requireNonNull(interpreterProperties);
            Objects.requireNonNull(stagingArea);

            this.interpreterProperties = interpreterProperties;
            inputModule = (RuntimeInputModule) stagingArea.getAnnotatedExecutionTrace().getModule();
            this.moduleId = moduleId;
            this.stagingArea = stagingArea;
        }

        private void readObject(ObjectInputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        @Override
        public UntypedActor create() {
            return new InputModuleInterpreterActor(this);
        }
    }

    private InputModuleInterpreterActor(Factory factory) {
        super(factory.interpreterProperties, factory.stagingArea.getAnnotatedExecutionTrace(), factory.moduleId);
        inputModule = factory.inputModule;
        staging = factory.stagingArea;
    }

    @Override
    public void preStart() {
        publishStartModule();
        getSelf().tell(LocalMessage.PUT_VALUE, getSelf());
    }

    @Override
    public void postStop()  {
        publishStopModule(success);
    }

    void putValue() {
        SimpleName outPortName = SimpleName.identifier(BareInputModule.OUT_PORT_NAME);
        ExecutionTrace target = ExecutionTrace.empty().resolveOutPort(outPortName);
        @Nullable RuntimeOutPort outPort = inputModule.getEnclosedElement(RuntimeOutPort.class, outPortName);
        assert outPort != null;
        int outPortId = outPort.getOutIndex();

        @Nullable Object value = inputModule.getValue();
        String kind;
        Future<RuntimeExecutionTrace> stagingFuture;
        if (value != null) {
            kind = "value";
            stagingFuture = staging.putObject(target, value);
        } else {
            @Nullable RuntimeSerializationRoot raw = inputModule.getRaw();
            assert raw != null;
            kind = "serialization tree";
            stagingFuture = staging.putSerializationTree(target, raw);
        }

        // If all goes well, create a PoisonPill message and have it sent to this actor. This will stop this actor, as
        // its work is done.
        ActorRef parent = getContext().parent();
        Future<RuntimeExecutionTrace> successFuture = stagingFuture.map(
            new Mapper<RuntimeExecutionTrace, RuntimeExecutionTrace>() {
                @Override
                public RuntimeExecutionTrace apply(RuntimeExecutionTrace parameter) {
                    parent.tell(
                        new InterpreterInterface.SubmoduleOutPortHasSignal(getModuleId(), outPortId),
                        getSelf()
                    );
                    return parameter;
                }
            },
            getAsyncTaskContext()
        );
        awaitAsynchronousAction(successFuture, "writing %s to staging area", kind);
    }

    @Override
    void onEmptySetOfAsynchronousActions() {
        success = true;
        getSelf().tell(PoisonPill.getInstance(), getSelf());
    }

    @Override
    void inPortHasSignal(int inPortId) {
        log.warning(String.format(
            "Ignoring message that non-existing in-port with index %d received input.", inPortId
        ));
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message == LocalMessage.PUT_VALUE) {
            putValue();
        } else {
            super.onReceive(message);
        }
    }

    /**
     * Requests writing the value represented by this input module to the staging area.
     */
    private enum LocalMessage {
        PUT_VALUE
    }
}
