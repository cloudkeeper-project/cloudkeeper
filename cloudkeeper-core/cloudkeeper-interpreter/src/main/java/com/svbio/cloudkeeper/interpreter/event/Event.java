package com.svbio.cloudkeeper.interpreter.event;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base class of all interpreter events.
 *
 * <p>All event classes are <em>value-based</em> classes that are immutable and thread-safe. Its implementations of
 * {@link #equals(Object)}, {@link #hashCode()}, and {@link #toString()} are computed solely from the instance's state.
 */
public abstract class Event implements Serializable {
    private static final long serialVersionUID = 5516376615673702929L;

    private final long timestamp;

    Event(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        return !(otherObject == null || getClass() != otherObject.getClass())
            && timestamp == ((Event) otherObject).timestamp;

    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }

    @Override
    public abstract String toString();

    public long getTimestamp() {
        return timestamp;
    }
}
