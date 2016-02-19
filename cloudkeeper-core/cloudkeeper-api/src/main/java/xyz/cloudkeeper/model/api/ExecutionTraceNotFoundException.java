package xyz.cloudkeeper.model.api;

import xyz.cloudkeeper.model.api.staging.StagingException;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;

/**
 * Signals that no object in the staging area is associated with an execution trace.
 */
public final class ExecutionTraceNotFoundException extends StagingException {
    private static final long serialVersionUID = 5096202575918769719L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public ExecutionTraceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified execution trace as detail message (and no cause).
     *
     * @param trace string representation of the execution trace that was not found
     */
    public ExecutionTraceNotFoundException(ExecutionTrace trace) {
        super(trace.toString());
    }
}
