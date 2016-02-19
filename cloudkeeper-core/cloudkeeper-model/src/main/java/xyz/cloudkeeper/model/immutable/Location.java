package xyz.cloudkeeper.model.immutable;

import xyz.cloudkeeper.model.Immutable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Interface that represents a source-code locations.
 *
 * <p>This class is similar to {@link org.xml.sax.Locator} and {@link javax.xml.stream.Location}.
 */
public final class Location implements Immutable, Serializable {
    private static final long serialVersionUID = -56648239628654837L;

    private final String systemId;
    private final int lineNumber;
    private final int columnNumber;

    public Location(String systemId, int lineNumber, int columnNumber) {
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        Location other = (Location) otherObject;
        return Objects.equals(systemId, other.systemId)
            && lineNumber == other.lineNumber
            && columnNumber == other.columnNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId, lineNumber, columnNumber);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(systemId != null
            ? systemId
            : "<unknown>"
        );
        if (lineNumber >= 0) {
            stringBuilder.append('(').append(lineNumber);
            if (columnNumber >= 0) {
                stringBuilder.append(':').append(columnNumber);
            }
            stringBuilder.append(')');
        }
        return stringBuilder.toString();
    }

    /**
     * Return the system identifier for the location.
     *
     * <p>A file name must always be provided as a {@code file:...} URL.
     *
     * @return string containing the system identifier, or null if none is available.
     * @see org.xml.sax.Locator#getSystemId()
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the line number of this location.
     *
     * <p>This class does not define line delimiters, so the precise meaning depends on the underlying source code
     * (for instance, Java or XML).
     *
     * @return The line number, or -1 if none is available.
     * @see org.xml.sax.Locator#getLineNumber()
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the column number of this location.
     *
     * <p>This is a one-based number of Java {@code char} values since the last line end.
     *
     * @return The column number, or -1 if none is available.
     * @see org.xml.sax.Locator#getColumnNumber()
     */
    public int getColumnNumber() {
        return columnNumber;
    }
}
