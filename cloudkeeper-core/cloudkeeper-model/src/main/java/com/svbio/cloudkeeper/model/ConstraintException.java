package com.svbio.cloudkeeper.model;

import java.util.List;

/**
 * Signals that a CloudKeeper model constraint is violated.
 */
public class ConstraintException extends LinkerException {
    private static final long serialVersionUID = 4796186854250428853L;

    /**
     * Constructs a new exception with the specified detail message and linker trace.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method).
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public ConstraintException(String message, List<LinkerTraceElement> linkerTrace) {
        super(message, linkerTrace);
    }
}
