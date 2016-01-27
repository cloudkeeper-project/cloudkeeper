package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareChildOutToParentOutConnection;

import javax.annotation.Nonnull;

public interface RuntimeChildOutToParentOutConnection extends RuntimeConnection, BareChildOutToParentOutConnection {
    @Override
    @Nonnull
    RuntimeOutPort getFromPort();

    @Override
    @Nonnull
    RuntimeOutPort getToPort();
}
