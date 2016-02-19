package xyz.cloudkeeper.simple;

/**
 * Signals that an exception occurred while constructing a new object from a builder.
 */
public class BuilderException extends RuntimeException {
    private static final long serialVersionUID = 6526024474244574940L;

    private final String className;

    public BuilderException(String className, Throwable cause) {
        super(String.format("Could not build instance of %s.", className), cause);
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
