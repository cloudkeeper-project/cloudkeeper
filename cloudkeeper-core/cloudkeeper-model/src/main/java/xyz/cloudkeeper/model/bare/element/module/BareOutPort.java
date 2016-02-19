package xyz.cloudkeeper.model.bare.element.module;

/**
 * Out-port of a module.
 */
public interface BareOutPort extends BarePort {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "out-port";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareOutPort#toString()}.
         */
        public static String toString(BareOutPort instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
