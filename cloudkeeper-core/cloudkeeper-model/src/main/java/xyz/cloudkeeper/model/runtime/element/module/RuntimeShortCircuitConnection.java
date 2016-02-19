package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareShortCircuitConnection;

import javax.annotation.Nonnull;

public interface RuntimeShortCircuitConnection extends RuntimeConnection, BareShortCircuitConnection {
    @Override
    @Nonnull
    RuntimeInPort getFromPort();

    @Override
    @Nonnull
    RuntimeOutPort getToPort();
}
