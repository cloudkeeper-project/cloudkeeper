package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.element.BareSimpleNameable;

import javax.annotation.Nullable;

/**
 * Parent-in-to-child-in connection.
 *
 * In a parent-in-to-child-in connection, the source module is the parent of the target module. Both the source and the
 * target port are in-ports.
 */
public interface BareParentInToChildInConnection extends BareConnection {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "parent-in-to-child-in connection";

    /**
     * Returns the module containing the to-port.
     */
    @Nullable
    BareSimpleNameable getToModule();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareParentInToChildInConnection#toString()}.
         */
        public static String toString(BareParentInToChildInConnection instance) {
            @Nullable BareSimpleNameable toModule = instance.getToModule();
            String toModuleName = toModule == null || toModule.getSimpleName() == null
                ? "(null)"
                : toModule.getSimpleName().toString();

            return String.format("%s %s", NAME, BareConnection.Default.toString(instance, "", toModuleName));
        }
    }
}
