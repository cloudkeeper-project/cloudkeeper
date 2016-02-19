package xyz.cloudkeeper.model;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the model annotation type that this DSL annotation is equivalent to.
 *
 * <p>The model annotation type must have the same number of elements, and the names must be identical. However,
 * whenever the type of an element is {@link Class} or an array of {@link Class}, the corresponding element of the model
 * annotation type must have a return type of {@link String} or array of {@link String}, respectively.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModelEquivalent {
    Class<? extends Annotation> value();
}
