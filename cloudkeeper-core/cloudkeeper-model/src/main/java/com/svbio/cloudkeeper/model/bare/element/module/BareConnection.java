package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.bare.element.BareSimpleNameable;

import javax.annotation.Nullable;

/**
 * Connection between two ports.
 *
 * Each instance is guaranteed to implement one (and only one) of the following interfaces:
 * <ul><li>
 *     {@link BareSiblingConnection}
 * </li><li>
 *     {@link BareParentInToChildInConnection}
 * </li><li>
 *     {@link BareChildOutToParentOutConnection}
 * </li><li>
 *     {@link BareShortCircuitConnection}
 * </li></ul>
 */
public interface BareConnection extends BareLocatable {
    /**
     * Applies a visitor to this connection.
     *
     * @param visitor the visitor operating on this connection
     * @param parameter additional parameter to the visitor
     * @param <T> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @return a visitor-specified result
     */
    @Nullable
    <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter);

    /**
     * Returns the from-port.
     *
     * If this is {@code null} and the from-module has only a single port allowing an outgoing connection, then it
     * is implicitly assumed to be the from-port of this connection.
     *
     * @return the from-port
     */
    @Nullable
    BareSimpleNameable getFromPort();

    /**
     * Returns the to-port.
     *
     * If this is {@code null} and the to-module has only a single port allowing an incoming connection, then it is
     * implicitly assumed to be the to-port of this connection.
     *
     * @return the to-port
     */
    @Nullable
    BareSimpleNameable getToPort();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareConnection#toString()}.
         */
        static String toString(BareConnection instance, String fromModuleName, String toModuleName) {
            @Nullable BareSimpleNameable fromPort = instance.getFromPort();
            @Nullable BareSimpleNameable toPort = instance.getToPort();
            String fromPortName = fromPort == null || fromPort.getSimpleName() == null
                ? "(null)"
                : fromPort.getSimpleName().toString();
            String toPortName = toPort == null || toPort.getSimpleName() == null
                ? "(null)"
                : toPort.getSimpleName().toString();

            return String.format("%s#%s -> %s#%s", fromModuleName, fromPortName, toModuleName, toPortName);
        }
    }
}
