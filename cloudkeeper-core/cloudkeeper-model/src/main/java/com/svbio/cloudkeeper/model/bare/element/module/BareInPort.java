package com.svbio.cloudkeeper.model.bare.element.module;

/**
 * In-port of a module.
 */
public interface BareInPort extends BarePort {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "in-port";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareInPort#toString()}.
         */
        public static String toString(BareInPort instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
