package com.svbio.cloudkeeper.model.bare.element.module;

/**
 * I/O-port of a loop module.
 */
public interface BareIOPort extends BareInPort, BareOutPort {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "i/o-port";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareIOPort#toString()}.
         */
        public static String toString(BareIOPort instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
