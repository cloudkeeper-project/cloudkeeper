package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.model.immutable.Location;

public final class ConnectionException extends DSLException {
    private static final long serialVersionUID = 9146259936179457686L;

    public ConnectionException(String message, Location location) {
        super(message, location);
    }
}
