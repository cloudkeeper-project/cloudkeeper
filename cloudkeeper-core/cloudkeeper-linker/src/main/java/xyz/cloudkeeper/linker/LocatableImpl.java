package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.immutable.Location;

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
