package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nonnull;

/**
 * Tagging interface for DSL out-ports.
 *
 * @param <T> type of the port
 */
public interface DSLOutPort<T> extends BareOutPort {
    @Nonnull
    @Override
    SimpleName getSimpleName();
}
