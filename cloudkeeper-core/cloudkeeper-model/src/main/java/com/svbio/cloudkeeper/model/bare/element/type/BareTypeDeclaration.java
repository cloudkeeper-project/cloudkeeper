package com.svbio.cloudkeeper.model.bare.element.type;

import com.svbio.cloudkeeper.model.bare.element.BareParameterizable;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * CloudKeeper type declaration.
 *
 * This class store the CloudKeeper-relevant parts of a Java {@link Class} instance.
 *
 * This interface corresponds to {@link javax.lang.model.element.TypeElement}, and both interfaces can be implemented
 * with covariant return types.
 */
public interface BareTypeDeclaration extends BarePluginDeclaration, BareParameterizable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "type declaration";

    /**
     * The concrete kind of a type declaration.
     *
     * @see javax.lang.model.element.ElementKind
     */
    enum Kind {
        /**
         * A Java class as defined by {@link javax.lang.model.element.ElementKind#CLASS}. This excludes, for instance,
         * enums.
         */
        CLASS,

        /**
         * A Java interface as defined by {@link javax.lang.model.element.ElementKind#INTERFACE}. This excluded, for
         * instance, annotation types.
         */
        INTERFACE
    }

    /**
     * Returns the direct superclass of this type element.
     *
     * <p>If this methods returns {@code null} this type declaration is assumed to not have a super class. If this type
     * declaration represents an interface, this method is required to return {@code null} or an instance of
     * {@link com.svbio.cloudkeeper.model.bare.type.BareNoType}.
     *
     * @see javax.lang.model.element.TypeElement#getSuperclass()
     */
    @Nullable
    BareTypeMirror getSuperclass();

    /**
     * Returns the interface types directly implemented by this class or extended by this interface.
     *
     * @see javax.lang.model.element.TypeElement#getInterfaces()
     */
    List<? extends BareTypeMirror> getInterfaces();

    /**
     * Returns the {@code kind} of this element.
     *
     * @see javax.lang.model.element.TypeElement#getKind()
     */
    Kind getTypeDeclarationKind();

    /**
     * Returns the (directly) nested type declarations.
     */
    List<? extends BareTypeDeclaration> getNestedTypeDeclarations();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareTypeDeclaration#toString()}.
         */
        public static String toString(BareTypeDeclaration instance) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(NAME).append(" '").append(instance.getSimpleName());
            List<? extends BareTypeParameterElement> typeParameters = instance.getTypeParameters();
            if (!typeParameters.isEmpty()) {
                stringBuilder.append('<');
                Iterator<? extends BareTypeParameterElement> typeParameterIterator = typeParameters.iterator();
                while (typeParameterIterator.hasNext()) {
                    stringBuilder.append(typeParameterIterator.next());
                    if (typeParameterIterator.hasNext()) {
                        stringBuilder.append(", ");
                    }
                }
                stringBuilder.append('>');
            }
            return stringBuilder.append('\'').toString();
        }
    }
}
