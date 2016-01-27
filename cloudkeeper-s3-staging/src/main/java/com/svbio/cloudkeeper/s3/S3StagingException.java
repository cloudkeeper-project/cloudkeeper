package com.svbio.cloudkeeper.s3;

import com.svbio.cloudkeeper.model.api.staging.StagingException;

/**
 * Signals an exception in the S3-based staging area.
 */
public class S3StagingException extends StagingException {
    private static final long serialVersionUID = -5166776951192340848L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public S3StagingException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public S3StagingException(String message, Throwable cause) {
        super(message, cause);
    }
}
