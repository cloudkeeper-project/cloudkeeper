package com.svbio.cloudkeeper.model.api;

/**
 * Signals that an exception has occurred while trying to provide the CloudKeeper runtime state.
 *
 * <p>This exception is a relatively high-level exception, and it is guaranteed to either have a detailed message or a
 * non-null cause.
 */
public class RuntimeStateProvisionException extends Exception {
    private static final long serialVersionUID = 6414993743595001458L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public RuntimeStateProvisionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public RuntimeStateProvisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
