package com.svbio.cloudkeeper.maven;

import javax.annotation.Nullable;

/**
 * Signals that an exception occurred while synchronizing access to an Aether repository.
 */
public class SyncContextException extends RuntimeException {
    private static final long serialVersionUID = 7306233793706500133L;

    /**
     * Constructs a new exception with the specified detail message and linker trace.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). A {@code null}
     *     value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public SyncContextException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
