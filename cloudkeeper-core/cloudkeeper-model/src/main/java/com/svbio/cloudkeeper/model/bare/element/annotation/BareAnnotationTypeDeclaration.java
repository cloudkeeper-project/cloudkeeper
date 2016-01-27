package com.svbio.cloudkeeper.model.bare.element.annotation;

import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;

import java.util.List;

/**
 * CloudKeeper annotation type declaration.
 *
 * <p>CloudKeeper annotation declarations are equivlanet to Java Annotation Types as specified by §9.6 of the Java
 * Language Specification (JLS).
 */
public interface BareAnnotationTypeDeclaration extends BarePluginDeclaration {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "annotation type declaration";

    /**
     * Returns the annotation type elements of this annotation declaration.
     */
    List<? extends BareAnnotationTypeElement> getElements();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareAnnotationTypeDeclaration#toString()}.
         */
        public static String toString(BareAnnotationTypeDeclaration instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
