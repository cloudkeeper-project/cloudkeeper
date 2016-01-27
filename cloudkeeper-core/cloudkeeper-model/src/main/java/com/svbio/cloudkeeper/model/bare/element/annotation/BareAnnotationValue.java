package com.svbio.cloudkeeper.model.bare.element.annotation;

import java.util.Arrays;

/**
 * Value of an annotation type element.
 *
 * <p>An annotation value represents a value of one of the following Java types:
 * <ul><li>
 *     a primitive type
 * </li><li>
 *     {@link String}
 * </li><li>
 *     an array of one of the previous types (note that this precludes arrays of arrays).
 * </li></ul>
 *
 * <p>This interface roughly corresponds to {@link javax.lang.model.element.AnnotationValue}. However,
 * {@link #toNativeValue()} returns the native value (as calling the method of a Java annotation type would), whereas
 * {@link javax.lang.model.element.AnnotationValue#getValue()} may return {@code List<? extends AnnotationValue>}
 * instead of arrays. Since empty lists preclude inference of the component type at runtime (due to type erasure), this
 * interface instead returns native arrays.
 *
 * @see javax.lang.model.element.AnnotationValue
 */
public interface BareAnnotationValue {
    /**
     * Returns the native value represented by this instance. Guaranteed non-null.
     */
    Object toNativeValue();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareAnnotation#toString()}.
         */
        public static String toString(BareAnnotationValue instance) {
            Object value = instance.toNativeValue();
            if (value.getClass().isArray()) {
                if (value instanceof boolean[]) {
                    return Arrays.toString((boolean[]) value);
                } else if (value instanceof char[]) {
                    return Arrays.toString((char[]) value);
                } else if (value instanceof byte[]) {
                    return Arrays.toString((byte[]) value);
                } else if (value instanceof short[]) {
                    return Arrays.toString((short[]) value);
                } else if (value instanceof int[]) {
                    return Arrays.toString((int[]) value);
                } else if (value instanceof long[]) {
                    return Arrays.toString((long[]) value);
                } else if (value instanceof float[]) {
                    return Arrays.toString((float[]) value);
                } else if (value instanceof double[]) {
                    return Arrays.toString((double[]) value);
                } else {
                    return Arrays.toString((Object[]) value);
                }
            } else {
                return value.toString();
            }
        }
    }
}
