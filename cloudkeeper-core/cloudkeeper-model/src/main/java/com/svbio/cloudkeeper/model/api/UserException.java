package com.svbio.cloudkeeper.model.api;

/**
 * Signals that an exception occurred while executing user-defined code.
 *
 * <p>Note that exceptions may have other CloudKeeper exceptions in the cause chain; for instance, if a module fails
 * because it called a {@link com.svbio.cloudkeeper.model.api.ModuleConnector} method that threw an exception, then the
 * cause chain could contain an {@link com.svbio.cloudkeeper.model.api.staging.StagingException}.
 */
public class UserException extends ExecutionException {
    private static final long serialVersionUID = -4084143140406749997L;

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public UserException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public UserException(Throwable cause) {
        super(cause);
    }
}
