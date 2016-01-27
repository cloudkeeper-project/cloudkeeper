package com.svbio.cloudkeeper.model.api.staging;

import java.io.IOException;

/**
 * Signals that an exception has occurred while accessing a staging area.
 *
 * This exception is a high-level exception that may have a wide range of causes.
 */
public class StagingException extends IOException {
    private static final long serialVersionUID = -6021945648711805388L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public StagingException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public StagingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public StagingException(Throwable cause) {
        super(cause);
    }
}
