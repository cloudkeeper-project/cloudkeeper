package xyz.cloudkeeper.interpreter;

final class UncaughtThrowableException extends RuntimeException {
    private static final long serialVersionUID = 2117850730248938436L;

    UncaughtThrowableException(Throwable cause) {
        super(cause);
    }
}
