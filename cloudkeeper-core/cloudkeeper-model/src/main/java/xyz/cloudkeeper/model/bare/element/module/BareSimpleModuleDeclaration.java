package xyz.cloudkeeper.model.bare.element.module;

import java.util.List;

public interface BareSimpleModuleDeclaration extends BareModuleDeclaration {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "simple-module declaration";

    /**
     * Returns the declared ports.
     */
    List<? extends BarePort> getPorts();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareSimpleModuleDeclaration#toString()}.
         */
        public static String toString(BareSimpleModuleDeclaration instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
