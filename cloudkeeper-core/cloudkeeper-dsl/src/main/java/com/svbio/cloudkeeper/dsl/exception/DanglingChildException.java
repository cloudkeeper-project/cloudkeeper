package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.model.immutable.Location;

public final class DanglingChildException extends DSLException {
    private static final long serialVersionUID = 9097610443815136079L;

    public DanglingChildException(Location location) {
        super("Child module is not assigned to any field.", location);
    }
}
