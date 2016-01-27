package com.svbio.cloudkeeper.model.runtime.element.module;

import javax.annotation.Nullable;

/**
 * Visitor of connections, in the style of the visitor design pattern.
 */
public interface RuntimeConnectionVisitor<T, P> {
    /**
     * Visits a sibling connection.
     */
    @Nullable
    T visitSiblingConnection(RuntimeSiblingConnection connection, @Nullable P parameter);

    /**
     * Visits a parent-in-to-child-in connection.
     */
    @Nullable
    T visitParentInToChildInConnection(RuntimeParentInToChildInConnection connection, @Nullable P parameter);

    /**
     * Visits a child-out-to-parent-out connection.
     */
    @Nullable
    T visitChildOutToParentOutConnection(RuntimeChildOutToParentOutConnection connection, @Nullable P parameter);

    /**
     * Visits a short-circuit connection.
     */
    @Nullable
    T visitShortCircuitConnection(RuntimeShortCircuitConnection connection, @Nullable P parameter);
}
