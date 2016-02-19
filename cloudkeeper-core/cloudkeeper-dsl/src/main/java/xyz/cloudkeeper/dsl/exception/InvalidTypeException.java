package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

public class InvalidTypeException extends DSLException {
    private static final long serialVersionUID = 8899148807211047984L;

    public InvalidTypeException(String message, Location location) {
        super(message, location);
    }
}
