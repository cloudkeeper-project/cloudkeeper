package com.svbio.cloudkeeper.interpreter.event;

import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

/**
 * Signals that the interpreter started a new execution frame.
 */
public final class BeginExecutionTraceEvent extends ExecutionTraceEvent {
    private static final long serialVersionUID = 3479703392271701260L;

    private BeginExecutionTraceEvent(long executionId, long timestamp, RuntimeExecutionTrace executionTrace) {
        super(executionId, timestamp, executionTrace);
    }

    public static BeginExecutionTraceEvent of(long executionId, long timestamp, RuntimeExecutionTrace executionTrace) {
        return new BeginExecutionTraceEvent(executionId, timestamp, executionTrace);
    }

    @Override
    public String toString() {
        return String.format("Begin '%s'", getExecutionTrace());
    }
}
