package com.svbio.cloudkeeper.model.bare.element.module;

/**
 * Short-circuit connection.
 *
 * In a short-circuit connection, the source module and the target module are one and the same parent module. The source
 * port is an in-port, and the target port is an out-port.
 */
public interface BareShortCircuitConnection extends BareConnection {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "short-circuit connection";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareShortCircuitConnection#toString()}.
         */
        public static String toString(BareShortCircuitConnection instance) {
            return String.format("%s %s", NAME, BareConnection.Default.toString(instance, "", ""));
        }
    }
}
