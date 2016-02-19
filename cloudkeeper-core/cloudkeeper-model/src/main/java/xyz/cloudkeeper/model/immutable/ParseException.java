package xyz.cloudkeeper.model.immutable;

public final class ParseException extends IllegalArgumentException {
    private static final long serialVersionUID = -4848299353563544990L;

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }
}
