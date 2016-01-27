package com.svbio.cloudkeeper.model.bare.element;

import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nullable;

/**
 * A mixin interface for an element that has a qualified name.
 */
public interface BareQualifiedNameable extends BareLocatable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "reference to element";

    /**
     * Returns the qualified name of this element (or reference to an element with this name).
     */
    @Nullable
    Name getQualifiedName();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareQualifiedNameable#toString()}.
         */
        public static String toString(BareQualifiedNameable instance) {
            return String.format("%s '%s'", NAME, instance.getQualifiedName());
        }
    }
}
