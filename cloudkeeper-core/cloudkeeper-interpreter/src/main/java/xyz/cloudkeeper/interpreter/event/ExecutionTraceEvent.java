package xyz.cloudkeeper.interpreter.event;

import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Abstract base class of events that pertain to a specific execution trace.
 */
public abstract class ExecutionTraceEvent extends Event {
    private static final long serialVersionUID = -7665939770755374032L;

    private final long executionId;
    private final ExecutionTrace executionTrace;

    ExecutionTraceEvent(long executionId, long timestamp, RuntimeExecutionTrace executionTrace) {
        super(timestamp);
        Objects.requireNonNull(executionTrace);

        this.executionId = executionId;
        this.executionTrace = ExecutionTrace.copyOf(executionTrace);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (!super.equals(otherObject)) {
            return false;
        }

        ExecutionTraceEvent other = (ExecutionTraceEvent) otherObject;
        return executionId == other.executionId
            && executionTrace.equals(other.executionTrace);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(executionId, executionTrace);
    }

    public long getExecutionId() {
        return executionId;
    }

    public ExecutionTrace getExecutionTrace() {
        return executionTrace;
    }
}
