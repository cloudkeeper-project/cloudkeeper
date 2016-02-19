package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

import java.util.Objects;

public class DSLException extends RuntimeException {
    private static final long serialVersionUID = -6870365176035657957L;

    private final Location location;

    /**
     * Constructor.
     *
     * None of the arguments may be null.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *     method.
     * @param location the location containing the invalid declaration.
     */
    public DSLException(String message, Location location) {
        this(message, null, location);
    }

    /**
     * Constructor.
     *
     * The first two arguments may not be null.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *     method.
     * @param cause The cause (which is saved for later retrieval by the {@link #getCause()} method). A {@code null}
     *     value is permitted, and indicates that the cause is nonexistent or unknown.
     * @param location the location containing the invalid declaration. A null value is permitted and indicates that
     *     location information is unavailable.
     */
    public DSLException(String message, Throwable cause, Location location) {
        super(Objects.requireNonNull(message), cause);
        this.location = location;
    }

    public final Location getLocation() {
        return location;
    }
}
