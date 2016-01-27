package com.svbio.cloudkeeper.model.bare.type;

import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Declared type.
 *
 * <p>This interface corresponds to {@link javax.lang.model.type.DeclaredType}, and both interfaces can be implemented
 * with covariant return types.
 */
public interface BareDeclaredType extends BareTypeMirror {
    /**
     * Returns the type of the innermost enclosing instance, or either a {@link BareNoType} instance or {@code null} if
     * there is no enclosing instance.
     *
     * <p>Only types corresponding to inner classes have an enclosing instance.
     *
     * @see javax.lang.model.type.DeclaredType#getEnclosingType()
     */
    @Nullable
    BareTypeMirror getEnclosingType();

    /**
     * Returns the type declaration corresponding to this declared type.
     *
     * @see javax.lang.model.type.DeclaredType#asElement()
     */
    @Nullable
    BareQualifiedNameable getDeclaration();

    /**
     * Returns the actual type arguments of this type.
     *
     * CloudKeeper does not support raw types. The returned list must contain all actual type arguments of this type.
     * The returned list may be empty or {@code null} if the type declaration is not generic.
     *
     * @see javax.lang.model.type.DeclaredType#getTypeArguments()
     */
    List<? extends BareTypeMirror> getTypeArguments();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareDeclaredType#toString()}.
         */
        @Nonnull
        public static String toString(BareDeclaredType instance) {
            @Nullable BareQualifiedNameable typeDeclarationReference = instance.getDeclaration();
            if (typeDeclarationReference == null || typeDeclarationReference.getQualifiedName() == null) {
                return "(null)";
            }

            List<? extends BareTypeMirror> typeArguments = instance.getTypeArguments();
            return typeArguments.isEmpty()
                ? typeDeclarationReference.getQualifiedName().toString()
                : String.format(
                    "%s<%s>",
                    instance.getDeclaration().getQualifiedName(),
                    BareTypeMirror.Default.toString(instance.getTypeArguments(), ", ")
                );
        }
    }
}
