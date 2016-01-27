package com.svbio.cloudkeeper.model;

import java.util.List;
import java.util.Objects;

/**
 * Signals that a referenced CloudKeeper element (like a repository, plugin declaration, or port) could not be found
 * during linking.
 */
public class NotFoundException extends LinkerException {
    private static final long serialVersionUID = 7403867005759361475L;

    private final String kind;
    private final String name;

    private static String toMessage(String kind, String name) {
        return name == null
            ? String.format("Could not find %s with default name.", kind)
            : String.format("Could not find %s with name '%s'.", kind, name);
    }

    /**
     * Constructs a new not-found linker exception.
     *
     * @param kind kind of the element that could not be found
     * @param name name of the element that could not be found
     * @param linkerTrace linker trace (which is saved for later retrieval by the {@link #getLinkerTrace()}
     *     method).
     * @throws NullPointerException if any of the arguments is {@code null}
     */
    public NotFoundException(String kind, String name, List<LinkerTraceElement> linkerTrace) {
        super(toMessage(kind, name), linkerTrace);
        this.kind = Objects.requireNonNull(kind);
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
