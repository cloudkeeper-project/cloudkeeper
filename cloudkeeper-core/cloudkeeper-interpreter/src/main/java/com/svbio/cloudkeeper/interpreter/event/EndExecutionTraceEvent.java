package com.svbio.cloudkeeper.interpreter.event;

import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Signals that the interpreter finished an execution frame.
 */
public class EndExecutionTraceEvent extends ExecutionTraceEvent {
    private static final long serialVersionUID = -3367118720202809782L;

    private final boolean successful;

    EndExecutionTraceEvent(long executionId, long timestamp, RuntimeExecutionTrace executionTrace, boolean successful) {
        super(executionId, timestamp, executionTrace);
        this.successful = successful;
    }

    public static EndExecutionTraceEvent of(long executionId, long timestamp, RuntimeExecutionTrace executionTrace,
            boolean successful) {
        return new EndExecutionTraceEvent(executionId, timestamp, executionTrace, successful);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return successful == ((EndExecutionTraceEvent) otherObject).successful;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(successful);
    }

    @Override
    public String toString() {
        return String.format("End '%s' (%s)", getExecutionTrace(), successful ? "successful" : "unsuccessful");
    }

    public boolean isSuccessful() {
        return successful;
    }
}
