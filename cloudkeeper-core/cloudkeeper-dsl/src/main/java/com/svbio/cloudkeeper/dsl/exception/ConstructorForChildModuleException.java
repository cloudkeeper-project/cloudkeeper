package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.dsl.CompositeModule;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.model.immutable.Location;

public final class ConstructorForChildModuleException extends DSLException {
    private static final long serialVersionUID = -4868983429101337074L;

    public ConstructorForChildModuleException(Location location) {
        super(String.format(
            "Modules must be created using %s#create() or %s#child().",
            ModuleFactory.class.getSimpleName(), CompositeModule.class.getSimpleName()
        ), location);
    }
}
