package com.svbio.cloudkeeper.model.bare.element.module;

import javax.annotation.Nullable;

/**
 * Composite-module declaration.
 */
public interface BareCompositeModuleDeclaration extends BareModuleDeclaration {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "composite module declaration";

    /**
     * Name of of the template element that every composite-module declaration implicitly contains.
     */
    String TEMPLATE_ELEMENT_NAME = "$template";

    /**
     * Returns the template composite-module instance of this declaration.
     */
    @Nullable
    BareCompositeModule getTemplate();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareCompositeModuleDeclaration#toString()}.
         */
        public static String toString(BareCompositeModuleDeclaration instance) {
            return String.format("%s %s", NAME, instance.getSimpleName());
        }
    }
}
