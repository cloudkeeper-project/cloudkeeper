package com.svbio.cloudkeeper.model.immutable;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationValue;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value of an annotation-type element.
 *
 * <p>CloudKeeper annotation values are slightly more restrictive than Java annotation values. Specifically, an
 * annotation value has one of the following native types:
 * <ul><li>
 *     a primitive type
 * </li><li>
 *     {@link String}
 * </li><li>
 *     an array of one of the previous types (note that this precludes arrays of arrays).
 * </li></ul>
 */
@XmlJavaTypeAdapter(JAXBAdapters.AnnotationValueAdapter.class)
public final class AnnotationValue implements BareAnnotationValue, Immutable, Serializable {
    private static final long serialVersionUID = 6125285505656303027L;

    private static final List<Class<?>> ELEMENTARY_BOXED_TYPES = Collections.unmodifiableList(Arrays.<Class<?>>asList(
        Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, String.class
    ));

    private final Serializable nativeValue;

    private AnnotationValue(Serializable nativeValue) {
        this.nativeValue = nativeValue;
    }

    private static boolean isValidType(Class<?> clazz) {
        if (ELEMENTARY_BOXED_TYPES.contains(clazz)) {
            return true;
        }
        if (!clazz.isArray()) {
            return false;
        }
        Class<?> componentType = clazz.getComponentType();
        return componentType.isPrimitive() || String.class.equals(clazz.getComponentType());
    }

    public static AnnotationValue of(Object nativeValue) {
        Objects.requireNonNull(nativeValue);
        if (!isValidType(nativeValue.getClass())) {
            throw new IllegalArgumentException(String.format(
                "Expected object that is a boxed primitive, a String, or an array whose component type is a "
                + "primitive type or String. However, got %s.", nativeValue
            ));
        }

        return new AnnotationValue((Serializable) copyOf(nativeValue));
    }

    public static AnnotationValue copyOf(BareAnnotationValue original) {
        Objects.requireNonNull(original);
        return original instanceof AnnotationValue
            ? (AnnotationValue) original
            : of(original.toNativeValue());
    }

    /**
     * Returns a copy if the given object is an array, or otherwise simply returns the given object.
     *
     * <p>Annotation element values can assume only a restricted set of values, as described in
     * {@link com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement#getReturnType()}. If the
     * given object is of a primitive-wrapper class or a {@link String}, the object is immutable and simply returned.
     * Otherwise, the given object must be an array, and a copy is returned.
     *
     * @param object object to copy (if necessary)
     * @return copy if the given object is an array, or otherwise simply the given object
     */
    private static Object copyOf(Object object) {
        Class<?> clazz = object.getClass();
        if (clazz.isArray()) {
            int length = Array.getLength(object);
            Object newArray = Array.newInstance(clazz.getComponentType(), length);
            System.arraycopy(object, 0, newArray, 0, length);
            return newArray;
        } else {
            return object;
        }
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        AnnotationValue other = (AnnotationValue) otherObject;
        return isEqual(nativeValue, other.nativeValue);
    }

    private static boolean isEqualArrays(Object object, Object other) {
        if (object instanceof boolean[]) {
            return Arrays.equals((boolean[]) object, (boolean[]) other);
        } else if (object instanceof char[]) {
            return Arrays.equals((char[]) object, (char[]) other);
        } else if (object instanceof byte[]) {
            return Arrays.equals((byte[]) object, (byte[]) other);
        } else if (object instanceof short[]) {
            return Arrays.equals((short[]) object, (short[]) other);
        } else if (object instanceof int[]) {
            return Arrays.equals((int[]) object, (int[]) other);
        } else if (object instanceof long[]) {
            return Arrays.equals((long[]) object, (long[]) other);
        } else if (object instanceof float[]) {
            return Arrays.equals((float[]) object, (float[]) other);
        } else if (object instanceof double[]) {
            return Arrays.equals((double[]) object, (double[]) other);
        } else {
            return Arrays.equals((Object[]) object, (Object[]) other);
        }
    }

    /**
     * Returns whether the two given (unwrapped) annotation element values are equal.
     *
     * <p>Annotation element values can assume only a restricted set of values, as described in
     * {@link com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement#getReturnType()}.
     * This method performs a comparison according to {@link java.lang.annotation.Annotation#equals(Object)}.
     *
     * @param object primitive type, String, or an array type whose component type is one of the preceding types. Must
     *     not be null.
     * @param other primitive type, String, or an array type whose component type is one of the preceding types. Must
     *     not be null.
     * @return whether the two given (unwrapped) annotation element values are equal
     * @throws NullPointerException if an argument is null
     */
    private static boolean isEqual(Object object, Object other) {
        if (object == other) {
            return true;
        }

        Class<?> clazz = object.getClass();
        if (!clazz.equals(other.getClass())) {
            return false;
        }

        if (clazz.isArray()) {
            return isEqualArrays(object, other);
        } else {
            return object.equals(other);
        }
    }

    @Override
    public int hashCode() {
        if (nativeValue.getClass().isArray()) {
            if (nativeValue instanceof boolean[]) {
                return Arrays.hashCode((boolean[]) nativeValue);
            } else if (nativeValue instanceof char[]) {
                return Arrays.hashCode((char[]) nativeValue);
            } else if (nativeValue instanceof byte[]) {
                return Arrays.hashCode((byte[]) nativeValue);
            } else if (nativeValue instanceof short[]) {
                return Arrays.hashCode((short[]) nativeValue);
            } else if (nativeValue instanceof int[]) {
                return Arrays.hashCode((int[]) nativeValue);
            } else if (nativeValue instanceof long[]) {
                return Arrays.hashCode((long[]) nativeValue);
            } else if (nativeValue instanceof float[]) {
                return Arrays.hashCode((float[]) nativeValue);
            } else if (nativeValue instanceof double[]) {
                return Arrays.hashCode((double[]) nativeValue);
            } else {
                return Arrays.hashCode((Object[]) nativeValue);
            }
        } else {
            return nativeValue.hashCode();
        }
    }

    @Override
    public String toString() {
        if (nativeValue.getClass().isArray()) {
            if (nativeValue instanceof boolean[]) {
                return Arrays.toString((boolean[]) nativeValue);
            } else if (nativeValue instanceof char[]) {
                return Arrays.toString((char[]) nativeValue);
            } else if (nativeValue instanceof byte[]) {
                return Arrays.toString((byte[]) nativeValue);
            } else if (nativeValue instanceof short[]) {
                return Arrays.toString((short[]) nativeValue);
            } else if (nativeValue instanceof int[]) {
                return Arrays.toString((int[]) nativeValue);
            } else if (nativeValue instanceof long[]) {
                return Arrays.toString((long[]) nativeValue);
            } else if (nativeValue instanceof float[]) {
                return Arrays.toString((float[]) nativeValue);
            } else if (nativeValue instanceof double[]) {
                return Arrays.toString((double[]) nativeValue);
            } else {
                return Arrays.toString((Object[]) nativeValue);
            }
        } else {
            return nativeValue.toString();
        }
    }

    @Override
    public Object toNativeValue() {
        return copyOf(nativeValue);
    }
}
