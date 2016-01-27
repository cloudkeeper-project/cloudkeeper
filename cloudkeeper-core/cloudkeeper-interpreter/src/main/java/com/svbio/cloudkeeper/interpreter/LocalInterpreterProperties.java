package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.model.api.RuntimeContext;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * Local, JVM-specific, interpreter properties.
 *
 * <p>Instances of this class are <em>not</em> serializable. Any attempt to serialize an instance will cause a
 * {@link NotSerializableException}.
 */
final class LocalInterpreterProperties extends InterpreterProperties {
    private static final long serialVersionUID = 1852484879847571435L;

    private final long executionId;
    private final RuntimeContext runtimeContext;
    @Nullable private final ExecutionContext asyncTaskContext;
    private final InterpreterEventBus eventBus;

    /**
     * Constructor.
     *
     * @param machineIndependentProperties workflow-execution properties that are equal across machine boundaries
     * @param executionId id of this workflow execution
     * @param runtimeContext runtime context for this workflow execution
     * @param asyncTaskContext the {@link ExecutionContext} that is to be used for scheduling asynchronous tasks (such
     *     as futures), or {@code null} to indicate that {@code getContext().dispatcher()} should be used
     * @param eventBus event bus that module-interpreter actors will publish events to
     */
    LocalInterpreterProperties(InterpreterProperties machineIndependentProperties, long executionId,
            RuntimeContext runtimeContext, @Nullable ExecutionContext asyncTaskContext, InterpreterEventBus eventBus) {
        super(machineIndependentProperties);
        this.executionId = executionId;
        this.runtimeContext = Objects.requireNonNull(runtimeContext);
        this.asyncTaskContext = asyncTaskContext;
        this.eventBus = Objects.requireNonNull(eventBus);
    }

    private void readObject(ObjectInputStream stream) throws IOException {
        throw new NotSerializableException(getClass().getName());
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        throw new NotSerializableException(getClass().getName());
    }

    long getExecutionId() {
        return executionId;
    }

    RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    /**
     * Returns the {@link ExecutionContext} that is to be used for scheduling asynchronous tasks, or {@code null} to
     * indicate that {@code getContext().dispatcher()} should be used.
     *
     * <p>This property exists for testing purposes: It allows to schedule futures that are executed only when
     * explicitly triggered by the test code.
     */
    @Nullable
    ExecutionContext getAsyncTaskContext() {
        return asyncTaskContext;
    }

    InterpreterEventBus getEventBus() {
        return eventBus;
    }
}
