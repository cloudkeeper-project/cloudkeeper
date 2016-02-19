package xyz.cloudkeeper.model.api;

/**
 * Signals that a workflow execution failed.
 */
public class WorkflowExecutionException extends Exception {
    private static final long serialVersionUID = -7631284784360830877L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public WorkflowExecutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public WorkflowExecutionException(Throwable cause) {
        super(cause);
    }
}
