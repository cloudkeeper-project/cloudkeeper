package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;

import javax.annotation.Nullable;

/**
 * Proxy module.
 */
public interface BareProxyModule extends BareModule {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "proxy module";

    /**
     * Returns the module declaration.
     */
    @Nullable
    BareQualifiedNameable getDeclaration();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareProxyModule#toString()}.
         */
        public static String toString(BareProxyModule instance) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(NAME).append(" '").append(instance.getSimpleName()).append('\'');

            @Nullable BareQualifiedNameable declaration = instance.getDeclaration();
            if (declaration != null) {
                stringBuilder.append(" (").append(declaration.getQualifiedName()).append(')');
            }
            return stringBuilder.toString();
        }
    }
}
