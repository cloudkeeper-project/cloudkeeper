package com.svbio.cloudkeeper.model.api.staging;

/**
 * Signals that an {@link InstanceProvider} was unable to provide an instance of the
 * specified kind.
 */
public class InstanceProvisionException extends Exception {
    private static final long serialVersionUID = 5383847672938436622L;

    /**
     * Constructs a new exception with the specified detail message, but without cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method
     */
    public InstanceProvisionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public InstanceProvisionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public InstanceProvisionException(Throwable cause) {
        super(cause);
    }
}
