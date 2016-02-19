package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareChildOutToParentOutConnection;

import javax.annotation.Nonnull;

public interface RuntimeChildOutToParentOutConnection extends RuntimeConnection, BareChildOutToParentOutConnection {
    @Override
    @Nonnull
    RuntimeOutPort getFromPort();

    @Override
    @Nonnull
    RuntimeOutPort getToPort();
}
