package com.svbio.cloudkeeper.model.bare.type;

import com.svbio.cloudkeeper.model.bare.element.BareSimpleNameable;

import javax.annotation.Nullable;

/**
 * Type variable.
 *
 * <p>This interface corresponds to {@link javax.lang.model.type.TypeVariable}, and both interfaces can be implemented
 * with covariant return types.
 *
 * <p>Note that a type parameter cannot include an explicit lower bound declaration. Therefore, this interface does not
 * contain a method called {@code getLowerBound()}. Only capture conversion (see JLS §5.1.10) can produce a type
 * variable with a non-trivial lower bound.
 */
public interface BareTypeVariable extends BareTypeMirror {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "type variable";

    /**
     * Returns the element corresponding to this type variable.
     *
     * @see javax.lang.model.type.TypeVariable#asElement()
     */
    @Nullable
    BareSimpleNameable getFormalTypeParameter();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareTypeVariable#toString()}.
         */
        public static String toString(BareTypeVariable instance) {
            @Nullable BareSimpleNameable typeParameterElement = instance.getFormalTypeParameter();

            return typeParameterElement == null || typeParameterElement.getSimpleName() == null
                ? "(null)"
                : typeParameterElement.getSimpleName().toString();
        }
    }
}
