package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

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
