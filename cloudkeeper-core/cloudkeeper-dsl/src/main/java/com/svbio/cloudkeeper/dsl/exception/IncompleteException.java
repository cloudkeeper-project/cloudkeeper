package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.model.immutable.Location;

public class IncompleteException extends DSLException {
    private static final long serialVersionUID = 3806285640497389441L;

    public IncompleteException(String field, Location location) {
        super(String.format("%s cannot be null.", field), location);
    }
}
