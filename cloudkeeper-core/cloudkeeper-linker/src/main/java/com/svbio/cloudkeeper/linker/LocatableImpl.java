package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.immutable.Location;

import javax.annotation.Nullable;

abstract class LocatableImpl extends AbstractFreezable implements BareLocatable {
    @Nullable private final Location location;

    LocatableImpl(State initialState, @Nullable CopyContext parentContext) {
        super(initialState, parentContext);
        location = null;
    }

    LocatableImpl(@Nullable BareLocatable original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        assert original != null;
        location = original.getLocation();
    }

    @Override
    @Nullable
    public final Location getLocation() {
        return location;
    }
}
