package xyz.cloudkeeper.model;

import java.util.List;

/**
 * Signals that a required property has not been specified.
 */
public class MissingPropertyException extends LinkerException {
    private static final long serialVersionUID = 4049170127183133081L;

    /**
     * Constructs a new exception with the specified detail message and linker trace.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method).
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public MissingPropertyException(String message, List<LinkerTraceElement> linkerTrace) {
        super(message, linkerTrace);
    }
}
