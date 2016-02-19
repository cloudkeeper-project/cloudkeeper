package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.linker.CopyContext.CopyContextSupplier;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationValue;
import xyz.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotationValue;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

final class AnnotationValueImpl extends AbstractFreezable implements RuntimeAnnotationValue {
    private final Object nativeValue;
    @Nullable private final ImmutableList<AnnotationValueImpl> list;

    @Nullable private volatile IElementImpl element;

    /**
     * Private constructor only used within this class in order to create the list elements in the list
     * representation of array values, as returned by {@link #getValue()}.
     */
    private AnnotationValueImpl(@Nullable Object nativeValue, CopyContext parentContext) throws LinkerException {
        super(State.CREATED, parentContext);
        Preconditions.requireCondition(nativeValue != null, parentContext,
            "Arrays in annotations must not contain null.");
        this.nativeValue = nativeValue;
        list = null;
    }

    AnnotationValueImpl(@Nullable BareAnnotationValue original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        assert original != null;

        CopyContext context = getCopyContext();
        nativeValue = copyOf(Preconditions.requireNonNull(
            original.toNativeValue(), context.newContextForProperty("nativeValue")));
        if (nativeValue.getClass().isArray()) {
            CopyContextSupplier elementContextSupplier = context.newContextForListProperty("nativeValue").supplier();
            int size = Array.getLength(nativeValue);
            List<AnnotationValueImpl> newList = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                newList.add(new AnnotationValueImpl(Array.get(nativeValue, i), elementContextSupplier.get()));
            }
            list = ImmutableList.copyOf(newList);
        } else {
            list = null;
        }
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return isEqual(nativeValue, ((AnnotationValueImpl) otherObject).nativeValue);
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
     * {@link xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement#getReturnType()}.
     * This method performs a comparison according to {@link java.lang.annotation.Annotation#equals(Object)}.
     *
     * @param object primitive type, String, or an array type whose component type is one of the preceding types. Must
     *     not be null.
     * @param other primitive type, String, or an array type whose component type is one of the preceding types. Must
     *     not be null.
     * @return whether the two given (unwrapped) annotation element values are equal
     * @throws NullPointerException if an argument is null
     */
    static boolean isEqual(Object object, Object other) {
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

    private static IllegalArgumentException invalidCopyOfArgumentException(Object original) {
        return new IllegalArgumentException(String.format(
            "Expected primitive type, String, or an array type whose component type is one of the preceding types. "
            + "However, got %s.", original
        ));
    }

    private static Object copyOfArray(Object original) {
        assert original.getClass().isArray();

        if (original instanceof boolean[]) {
            boolean[] typedOriginal = (boolean[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof char[]) {
            char[] typedOriginal = (char[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof byte[]) {
            byte[] typedOriginal = (byte[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof short[]) {
            short[] typedOriginal = (short[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof int[]) {
            int[] typedOriginal = (int[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof long[]) {
            long[] typedOriginal = (long[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof float[]) {
            float[] typedOriginal = (float[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof double[]) {
            double[] typedOriginal = (double[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else if (original instanceof String[]) {
            String[] typedOriginal = (String[]) original;
            return Arrays.copyOf(typedOriginal, typedOriginal.length);
        } else {
            throw invalidCopyOfArgumentException(original);
        }
    }

    /**
     * Returns the given value if it is immutable, or creates and returns a new copy if it is mutable.
     *
     * <p>Annotation element values can assume only a restricted set of values, as described in
     * {@link xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement#getReturnType()}. If the
     * given object is of a primitive-wrapper class or a {@link String}, the object is immutable and simply returned.
     * Otherwise, the given object must be an array, and a copy is returned.
     *
     * @param original primitive type, String, or an array type whose component type is one of the preceding types.
     *     May be null.
     * @return the given object if it is immutable, or a new copy if it is mutable.
     * @throws IllegalArgumentException if the given object is neither null nor of the required type.
     */
    private static Object copyOf(Object original) {
        Objects.requireNonNull(original);
        if (original.getClass().isArray()) {
            return copyOfArray(original);
        } else if (
            Arrays.<Class<?>>asList(
                Boolean.class,
                Character.class,
                Byte.class, Short.class, Integer.class, Long.class,
                Float.class, Double.class,
                String.class
            ).contains(original.getClass())
        ) {
            return original;
        }

        throw invalidCopyOfArgumentException(original);
    }

    @Override
    public Object toNativeValue() {
        return copyOf(nativeValue);
    }

    @Override
    public Object getValue() {
        return list != null
            ? list
            : nativeValue;
    }

    @Override
    @Nullable
    public IElementImpl getElement() {
        return element;
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        if (list != null) {
            freezables.addAll(list);
        }
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) throws LinkerException {
        element = context.getElement(this);
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
