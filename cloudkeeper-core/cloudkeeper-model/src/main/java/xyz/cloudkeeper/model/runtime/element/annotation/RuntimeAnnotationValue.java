package xyz.cloudkeeper.model.runtime.element.annotation;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationValue;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;

import javax.annotation.Nullable;

/**
 * Value of an annotation type element.
 *
 * <p>This interface is the linked immutable correspondence to
 * {@link BareAnnotationValue}.
 */
public interface RuntimeAnnotationValue extends BareAnnotationValue, Immutable {
    @Override
    boolean equals(@Nullable Object otherObject);

    /**
     * Returns the hash code for this annotation value as specified for
     * {@link java.lang.annotation.Annotation#hashCode()}.
     */
    @Override
    int hashCode();

    /**
     * Returns the value.
     *
     * <p>A value is of one of the following types:
     * <ul><li>
     *     A wrapper class (such as {@link Integer}) for a primitive type
     * </li><li>
     *     {@code String}
     * </li><li>
     *     A {@link java.util.List} instances of this interface (representing the elements, in declared order, if the
     *     value is an array)
     * </ul>
     *
     * @see javax.lang.model.element.AnnotationValue#getValue()
     */
    Object getValue();

    /**
     * Returns this annotation value interpreted as CloudKeeper element, or null if not applicable.
     *
     * <p>This method returns a non-null value if and only if both of the following conditions hold:
     * <ul><li>
     *     The corresponding annotation type element has a {@link cloudkeeper.annotations.CloudKeeperElementReference}
     *     annotation.
     * </li><li>
     *     {@link #getValue()} returns an object of type {@link String}.
     * </li></ul>
     */
    @Nullable
    RuntimeElement getElement();
}
