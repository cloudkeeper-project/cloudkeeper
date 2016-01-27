package com.svbio.cloudkeeper.model.bare.type;

import javax.annotation.Nullable;

/**
 * Array type.
 *
 * This interface corresponds to {@link javax.lang.model.type.ArrayType}, and both interfaces can be implemented with
 * covariant return types.
 */
public interface BareArrayType extends BareTypeMirror {
    /**
     * Returns the component type of this array type.
     *
     * @see javax.lang.model.type.ArrayType#getComponentType()
     */
    @Nullable
    BareTypeMirror getComponentType();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareArrayType#toString()}.
         */
        public static String toString(BareArrayType instance) {
            @Nullable BareTypeMirror componentType = instance.getComponentType();
            return componentType != null
                ? componentType + "[]"
                : "(null)[]";
        }
    }
}
