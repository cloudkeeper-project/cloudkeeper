package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.module.BareConnection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Connection between two ports.
 *
 * <p>Each instance is guaranteed to implement one (and only one) of the following interfaces:
 * <ul><li>
 *     {@link RuntimeSiblingConnection}
 * </li><li>
 *     {@link RuntimeParentInToChildInConnection}
 * </li><li>
 *     {@link RuntimeChildOutToParentOutConnection}
 * </li><li>
 *     {@link RuntimeShortCircuitConnection}
 * </li></ul>
 */
public interface RuntimeConnection extends BareConnection, Immutable {
    @Nullable
    <T, P> T accept(RuntimeConnectionVisitor<T, P> visitor, @Nullable P parameter);

    /**
     * Returns the index of this port in the connection list of the enclosing parent module.
     *
     * <p>Calling this method on a connection instance {@code connection} is equivalent to the following:
     * {@code connection.getParentModule().getConnections().indexOf(connection)}
     *
     * @see RuntimeParentModule#getConnections()
     *
     * @return the index of this connection in the connection list of the enclosing parent module
     */
    int getIndex();

    /**
     * Returns the enclosing parent module of this connection.
     *
     * Calling this method on a connection instance {@code connection} is equivalent to the following:
     * <ul><li>
     *     {@code connection.getFromPort().getModule()} if this connection is an instance of
     *     {@link RuntimeParentInToChildInConnection} or {@link RuntimeShortCircuitConnection}
     * </li><li>
     *     {@code connection.getFromPort().getParent().getModule()} if this connection is an instance of
     *     {@link RuntimeSiblingConnection} or {@link RuntimeChildOutToParentOutConnection}
     * </li></ul>
     *
     * @return the enclosing parent module of this connection
     */
    @Nonnull
    RuntimeParentModule getParentModule();

    /**
     * Returns the module containing the from-port.
     */
    @Nonnull
    RuntimeModule getFromModule();

    /**
     * Returns the port that the connection starts from.
     */
    @Override
    @Nonnull
    RuntimePort getFromPort();

    /**
     * Returns the module containing the to-port.
     */
    @Nonnull
    RuntimeModule getToModule();

    /**
     * Returns the port that the connection goes to.
     */
    @Override
    @Nonnull
    RuntimePort getToPort();

    /**
     * Returns the {@link TypeRelationship} of this connection.
     *
     * @return type relationship
     */
    @Nonnull
    TypeRelationship getTypeRelationship();

    /**
     * Returns the kind of this connection.
     *
     * @return kind of this connection
     */
    @Nonnull
    ConnectionKind getKind();
}
