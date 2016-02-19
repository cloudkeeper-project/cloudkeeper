package xyz.cloudkeeper.model;

import java.util.List;

/**
 * Signals that an exception occurred while the linker was initializing the runtime state.
 */
public class PreprocessingException extends LinkerException {
    private static final long serialVersionUID = -2997566625138221380L;

    /**
     * Constructs a new linker pre-processing exception with the specified detail message and linker trace.
     *
     * This method is equivalent to calling {@link #PreprocessingException(String, Throwable, java.util.List)} with
     * {@code null} as cause.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method).
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public PreprocessingException(String message, List<LinkerTraceElement> linkerTrace) {
        super(message, linkerTrace);
    }

    /**
     * Constructs a new linker pre-processing exception with the specified detail message and linker trace.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). A {@code null}
     *     value is permitted, and indicates that the cause is nonexistent or unknown.
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method). An empty list is permitted, and indicates that the trace is unknown.
     * @throws NullPointerException if {@code message} or {@code linkerTrace} are {@code null}
     */
    public PreprocessingException(String message, Throwable cause, List<LinkerTraceElement> linkerTrace) {
        super(message, cause, linkerTrace);
    }
}
