package xyz.cloudkeeper.model.bare.element.module;

import xyz.cloudkeeper.model.bare.element.BareSimpleNameable;

import javax.annotation.Nullable;

/**
 * Child-out-to-parent-out connection.
 *
 * <p>In a child-out-to-parent-out connection, the target module is the parent of the source module. Both the source and
 * the target port are out-ports.
 */
public interface BareChildOutToParentOutConnection extends BareConnection {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "child-out-to-parent-out connection";

    /**
     * Returns the module containing the from-port.
     */
    @Nullable
    BareSimpleNameable getFromModule();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareChildOutToParentOutConnection#toString()}.
         */
        public static String toString(BareChildOutToParentOutConnection instance) {
            @Nullable BareSimpleNameable fromModule = instance.getFromModule();
            String fromModuleName = fromModule == null || fromModule.getSimpleName() == null
                ? "(null)"
                : fromModule.getSimpleName().toString();

            return String.format("%s %s", NAME, BareConnection.Default.toString(instance, fromModuleName, ""));
        }
    }
}
