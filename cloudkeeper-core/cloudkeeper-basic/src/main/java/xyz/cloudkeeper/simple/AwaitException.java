package xyz.cloudkeeper.simple;

import xyz.cloudkeeper.model.api.WorkflowExecutionException;

/**
 * Signals that the result of a {@link xyz.cloudkeeper.model.api.WorkflowExecution} awaited by a
 * {@link WorkflowExecutions} operation has failed.
 */
public class AwaitException extends WorkflowExecutionException {
    private static final long serialVersionUID = -3555764773351110858L;

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public AwaitException(Throwable cause) {
        super(cause);
    }
}
