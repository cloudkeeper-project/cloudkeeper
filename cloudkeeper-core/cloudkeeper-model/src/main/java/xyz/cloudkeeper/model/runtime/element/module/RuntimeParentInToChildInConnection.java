package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareParentInToChildInConnection;

import javax.annotation.Nonnull;

public interface RuntimeParentInToChildInConnection extends RuntimeConnection, BareParentInToChildInConnection {
    @Override
    @Nonnull
    RuntimeInPort getFromPort();

    @Override
    @Nonnull
    RuntimeInPort getToPort();
}
