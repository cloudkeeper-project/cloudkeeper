package com.svbio.cloudkeeper.model.bare.element.annotation;

import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.bare.element.BareSimpleNameable;

import javax.annotation.Nullable;

/**
 * Annotation entry (member) of a CloudKeeper annotation.
 *
 * <p>CloudKeeper annotations are equivalent to Java Annotations as specified by §9.7 of the Java Language Specification
 * (JLS).
 *
 * <p>This interface models a bare annotation entry. It is similar to an entry of the map returned by
 * {@link javax.lang.model.element.AnnotationMirror#getElementValues()}. The method names are chosen so that
 * subinterfaces may implement both this interface and {@link java.util.Map.Entry} with covariant return types.
 */
public interface BareAnnotationEntry extends BareLocatable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "annotation entry";

    /**
     * Returns the annotation type element.
     */
    @Nullable
    BareSimpleNameable getKey();

    /**
     * Returns the value for the annotation element specified by {@link #getKey()}.
     */
    @Nullable
    BareAnnotationValue getValue();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareAnnotationEntry#toString()}.
         */
        public static String toString(BareAnnotationEntry instance) {
            return String.format("%s=%s", instance.getKey(), instance.getValue());
        }
    }
}
