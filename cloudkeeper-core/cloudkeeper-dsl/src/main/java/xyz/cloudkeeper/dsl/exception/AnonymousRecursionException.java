package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

public final class AnonymousRecursionException extends DSLException {
    private static final long serialVersionUID = 7259923312168809950L;

    public AnonymousRecursionException(Location location) {
        super("Recursion not allowed for anonymous modules.", location);
    }
}
