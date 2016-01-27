package com.svbio.cloudkeeper.model;

import com.svbio.cloudkeeper.model.util.ImmutableList;

import java.util.List;
import java.util.Objects;

/**
 * Signals that an exception occurred during linking.
 */
public class LinkerException extends Exception {
    private static final long serialVersionUID = 7123251199596227723L;

    private final ImmutableList<LinkerTraceElement> linkerTrace;

    /**
     * Constructs a new linker exception with the specified detail message and linker trace.
     *
     * <p>This method is equivalent to calling {@link #LinkerException(String, Throwable, java.util.List)} with
     * {@code null} as cause.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method).
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public LinkerException(String message, List<LinkerTraceElement> linkerTrace) {
        this(message, null, linkerTrace);
    }

    /**
     * Constructs a new linker exception with the specified detail message and linker trace.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). A {@code null}
     *     value is permitted, and indicates that the cause is nonexistent or unknown.
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method). An empty list is permitted, and indicates that the trace is unknown.
     * @throws NullPointerException if {@code message} or {@code linkerTrace} are {@code null}
     */
    public LinkerException(String message, Throwable cause, List<LinkerTraceElement> linkerTrace) {
        super(appendLinkerTrace(Objects.requireNonNull(message), Objects.requireNonNull(linkerTrace)), cause);
        this.linkerTrace = ImmutableList.copyOf(linkerTrace);
    }

    private static String appendLinkerTrace(String message, List<LinkerTraceElement> linkerTrace) {
        assert message != null && linkerTrace != null;

        if (linkerTrace.isEmpty()) {
            return message;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message).append("\n\nLinking backtrace:\n");
            for (LinkerTraceElement element: linkerTrace) {
                stringBuilder.append("    ").append(element).append('\n');
            }
            return stringBuilder.toString();
        }
    }

    /**
     * Returns the linker trace.
     *
     * The returned list cannot be modified.
     */
    public List<LinkerTraceElement> getLinkerTrace() {
        return linkerTrace;
    }
}
