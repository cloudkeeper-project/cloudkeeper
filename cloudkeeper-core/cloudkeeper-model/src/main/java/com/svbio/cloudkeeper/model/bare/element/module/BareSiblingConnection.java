package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.element.BareSimpleNameable;

import javax.annotation.Nullable;

/**
 * Connection between sibling modules.
 *
 * <p>In a sibling connection, the source module and the target module have the same parent. The source port is an
 * out-port, and the target port is an in-port.
 */
public interface BareSiblingConnection extends BareConnection {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "sibling connection";

    /**
     * Returns the module containing the from-port.
     */
    @Nullable
    BareSimpleNameable getFromModule();

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
         * Default implementation for {@link BareSiblingConnection#toString()}.
         */
        public static String toString(BareSiblingConnection instance) {
            @Nullable BareSimpleNameable fromModule = instance.getFromModule();
            String fromModuleName = fromModule == null || fromModule.getSimpleName() == null
                ? "(null)"
                : fromModule.getSimpleName().toString();
            @Nullable BareSimpleNameable toModule = instance.getToModule();
            String toModuleName = toModule == null || toModule.getSimpleName() == null
                ? "(null)"
                : toModule.getSimpleName().toString();

            return String.format("%s %s", NAME,
                BareConnection.Default.toString(instance, fromModuleName, toModuleName));
        }
    }
}
