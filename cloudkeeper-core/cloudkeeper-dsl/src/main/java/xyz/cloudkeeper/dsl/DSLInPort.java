package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nonnull;

/**
 * Tagging interface for DSL in-ports.
 *
 * @param <T> type of the port
 */
public interface DSLInPort<T> extends BareInPort {
    @Nonnull
    @Override
    SimpleName getSimpleName();
}
