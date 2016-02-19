package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

public final class CannotInstantiateException extends DSLException {
    private static final long serialVersionUID = -5448499710480413608L;

    public CannotInstantiateException(String message, Throwable cause, Location location) {
        super(message, cause, location);
    }

    public CannotInstantiateException(String message, Location location) {
        super(message, location);
    }
}
