package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareParentInToChildInConnection;

import javax.annotation.Nonnull;

public interface RuntimeParentInToChildInConnection extends RuntimeConnection, BareParentInToChildInConnection {
    @Override
    @Nonnull
    RuntimeInPort getFromPort();

    @Override
    @Nonnull
    RuntimeInPort getToPort();
}
