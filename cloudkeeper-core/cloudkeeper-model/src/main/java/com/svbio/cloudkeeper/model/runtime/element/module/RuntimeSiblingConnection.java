package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareSiblingConnection;

import javax.annotation.Nonnull;

public interface RuntimeSiblingConnection extends RuntimeConnection, BareSiblingConnection {
    @Override
    @Nonnull
    RuntimeOutPort getFromPort();

    @Override
    @Nonnull
    RuntimeInPort getToPort();
}
