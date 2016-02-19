package xyz.cloudkeeper.model.bare.element.module;

import javax.annotation.Nullable;

/**
 * Visitor of connections, in the style of the visitor design pattern.
 */
public interface BareConnectionVisitor<T, P> {
    /**
     * Visits a sibling connection.
     */
    @Nullable
    T visitSiblingConnection(BareSiblingConnection connection, @Nullable P parameter);

    /**
     * Visits a parent-in-to-child-in connection.
     */
    @Nullable
    T visitParentInToChildInConnection(BareParentInToChildInConnection connection, @Nullable P parameter);

    /**
     * Visits a child-out-to-parent-out connection.
     */
    @Nullable
    T visitChildOutToParentOutConnection(BareChildOutToParentOutConnection connection, @Nullable P parameter);

    /**
     * Visits a short-circuit connection.
     */
    @Nullable
    T visitShortCircuitConnection(BareShortCircuitConnection connection, @Nullable P parameter);
}
