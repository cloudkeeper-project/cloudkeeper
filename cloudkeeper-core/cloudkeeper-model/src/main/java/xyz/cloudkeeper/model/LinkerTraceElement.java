package xyz.cloudkeeper.model;

import xyz.cloudkeeper.model.immutable.Location;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Element of a linker trace.
 *
 * The linker trace is similar in concept to a stack trace: Each CloudKeeper entity (except for the root entity) has an
 * enclosing entity (for instance, a port may be contained in a module). The linker trace contains, in reverse order,
 * all enclosing CloudKeeper entities.
 */
public final class LinkerTraceElement implements Serializable {
    private static final long serialVersionUID = -254553239240341801L;

    private final String description;
    @Nullable private final Location location;

    /**
     * Constructor.
     *
     * @param description description of this linker trace element
     * @param location location of this linker trance element, may be {@code null} if unknown
     * @throws NullPointerException if {@code description} is null
     */
    public LinkerTraceElement(String description, @Nullable Location location) {
        this.description = Objects.requireNonNull(description);
        this.location = location;
    }

    /**
     * Returns the human-readable description of this linker trace element.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the source-code location that defines the CloudKeeper entity associated with the current linker trace
     * element.
     *
     * <p>This method may return null if the source-code location is not available. Otherwise, the returned location is
     * guaranteed to be immutable.
     */
    @Nullable
    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(256);
        stringBuilder.append(description);
        if (location != null) {
            stringBuilder.append("; ").append(location);
        }
        return stringBuilder.toString();
    }
}
