package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.model.immutable.Location;

public final class AnonymousRecursionException extends DSLException {
    private static final long serialVersionUID = 7259923312168809950L;

    public AnonymousRecursionException(Location location) {
        super("Recursion not allowed for anonymous modules.", location);
    }
}
