package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

public final class ConnectionException extends DSLException {
    private static final long serialVersionUID = 9146259936179457686L;

    public ConnectionException(String message, Location location) {
        super(message, location);
    }
}
