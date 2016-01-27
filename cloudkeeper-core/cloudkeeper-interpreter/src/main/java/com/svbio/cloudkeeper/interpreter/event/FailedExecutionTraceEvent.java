package com.svbio.cloudkeeper.interpreter.event;

import com.svbio.cloudkeeper.interpreter.InterpreterException;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Signals that an exception was thrown while interpreting a workflow.
 *
 * <p>Note that while an exception in a nested execution frame also causes all enclosing execution frames to fail, this
 * event is triggered only once, and it contains the execution trace of the frame where the exception originated from.
 */
public final class FailedExecutionTraceEvent extends ExecutionTraceEvent {
    private static final long serialVersionUID = 5203152418056457154L;

    private final InterpreterException exception;

    private FailedExecutionTraceEvent(long executionId, long timestamp, RuntimeExecutionTrace executionTrace,
            InterpreterException exception) {
        super(executionId, timestamp, executionTrace);
        this.exception = Objects.requireNonNull(exception);
    }

    public static FailedExecutionTraceEvent of(long executionId, long timestamp, RuntimeExecutionTrace executionTrace,
            InterpreterException exception) {
        return new FailedExecutionTraceEvent(executionId, timestamp, executionTrace, exception);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return Objects.equals(exception, ((FailedExecutionTraceEvent) otherObject).exception);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(exception);
    }

    @Override
    public String toString() {
        return String.format("Failed '%s'.", getExecutionTrace());
    }

    public InterpreterException getException() {
        return exception;
    }
}
