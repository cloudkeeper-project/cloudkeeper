package xyz.cloudkeeper.model.bare.element;

import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;

/**
 * A mixin interface for an element that has a simple name.
 */
public interface BareSimpleNameable extends BareLocatable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "reference to element";

    /**
     * Returns the simple name of this element (or reference to an element with this name).
     */
    @Nullable
    SimpleName getSimpleName();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareSimpleNameable#toString()}.
         */
        public static String toString(BareSimpleNameable instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
