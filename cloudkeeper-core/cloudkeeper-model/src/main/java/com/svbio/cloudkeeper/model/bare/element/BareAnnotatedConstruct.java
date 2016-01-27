package com.svbio.cloudkeeper.model.bare.element;

import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotation;

import java.util.List;

/**
 * Construct that can be annotated.
 *
 * <p>This interface is similar to {@code javax.lang.model.element.AnnotatedConstruct} (since Java 1.8).
 */
public interface BareAnnotatedConstruct extends BareLocatable {
    /**
     * Returns the list of annotations directly present on this construct.
     *
     * <p>This method is similar to {@link javax.lang.model.element.Element#getAnnotationMirrors()} and
     * {@link java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()}.
     */
    List<? extends BareAnnotation> getDeclaredAnnotations();
}
