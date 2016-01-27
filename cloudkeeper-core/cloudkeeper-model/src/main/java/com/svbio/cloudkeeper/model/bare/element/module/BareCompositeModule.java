package com.svbio.cloudkeeper.model.bare.element.module;

/**
 * Composite module.
 */
public interface BareCompositeModule extends BareParentModule {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "composite module";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareCompositeModule#toString()}.
         */
        public static String toString(BareCompositeModule instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
