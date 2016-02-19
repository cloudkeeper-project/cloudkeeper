package xyz.cloudkeeper.dsl;

import javax.annotation.Nullable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

/**
 * The sole purpose of Module classes is a convenient way to define CloudKeeper workflows, using a DSL on top of Java.
 * Module classes are expected to be used in build systems (and not in live production systems with a security manager).
 * They are transformed into workflow objects that use Strings (as opposed to Fields) to address modules and ports. In
 * order to ripe all the benefits from a DSL on top of Java (autocompletion, syntax checking, etc.), the transformation
 * process has to make heavy use of reflection.
 * We therefore need to make sure that all declared fields in subclasses are accessible. For the reasons mentioned
 * before, this is a deliberate design decision and is not considered a security risk or a breach of encapsulation best
 * practices.
 */
final class Fields {
    private Fields() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns an iterator over all fields declared in the given class, including superclasses up to the given upper
     * bound.
     *
     * @param clazz class whose fields to iterate through
     * @param upperBound only scan fields that are declared by this class or subclasses
     * @param <T> type of upper bound
     * @return iterable over fields
     */
    static <T> Iterable<Field> getAllFields(final Class<? extends T> clazz, final Class<T> upperBound) {
        requireNonNull(clazz);
        requireNonNull(upperBound);

        List<Field> fields = new ArrayList<>();
        Class<? extends T> currentClass = clazz;
        while (currentClass != null && upperBound.isAssignableFrom(currentClass)) {
            Field[] declaredFields = currentClass.getDeclaredFields();

            // getDeclaredFields() gives a different Field[] array each time it is called. Moreover, the Field objects
            // are not reused. Hence, the following code does not produce multi-threading issues.
            AccessibleObject.setAccessible(declaredFields, true);

            fields.addAll(Arrays.asList(declaredFields));

            @SuppressWarnings({ "unchecked", "UnnecessaryLocalVariable" })
            Class<? extends T> superclass = (Class<? extends T>) currentClass.getSuperclass();
            currentClass = superclass;
        }
        return fields;
    }

    /**
     * Returns all fields of an instance's class that contain a given value.
     *
     * @param object object whose fields to scan
     * @param upperBound only scan fields that are declared by this class or subclasses
     * @param value value to look for
     * @param <T> type of {@code object}
     * @param <U> type of upper bound
     * @return The {@link Field} object corresponding to the instance field that contains {@code value}. If there is
     *     none, {@code null}.
     * @throws IllegalStateException if a field cannot be accessed using reflection
     */
    static <T extends U, U> List<Field> getFieldsWithValue(T object, Class<U> upperBound, Object value) {
        List<Field> fieldsWithReferenceToValue = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) object.getClass();
        Field[] declaredFields = clazz.getFields();
        AccessibleObject.setAccessible(declaredFields, true);

        for (Field field: getAllFields(clazz, upperBound)) {
            Object reference;
            try {
                reference = field.get(object);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(String.format(
                    "Could not access field %s, even though AccessibleField#setAccessible(true) was called.", field
                ), exception);
            }
            if (reference == value) {
                fieldsWithReferenceToValue.add(field);
            }
        }

        return fieldsWithReferenceToValue;
    }

    /**
     * Returns the value of the given field
     *
     * @param object objects that contains the given instance field
     * @param field field whose value is to be returned
     * @return value of field in object
     * @throws IllegalStateException if the field cannot be accessed
     */
    @Nullable
    static Object valueOfField(Object object, Field field) {
        try {
            return field.get(object);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(String.format(
                "Could not access field %s, even though AccessibleField#setAccessible(true) was called.", field
            ), exception);
        }
    }

    private static final class FilteredFieldIterator<T extends U, U> implements Iterator<Field> {
        private final T object;
        private final Class<?> clazz;
        private final Iterator<Field> iterator;
        private Field currentField = null;

        private FilteredFieldIterator(T object, Class<U> upperBound, Class<?> clazz) {
            this.object = object;
            this.clazz = clazz;

            @SuppressWarnings("unchecked")
            Class<T> objectClass = (Class<T>) object.getClass();
            this.iterator = getAllFields(objectClass, upperBound).iterator();
            advance();
        }

        private Object valueOfCurrentField() {
            try {
                return currentField.get(object);
            } catch (IllegalAccessException exception) {
                throw new AssertionError(String.format(
                    "Could not access field %s, even though AccessibleField#setAccessible(true) was called.",
                    currentField
                ), exception);
            }
        }

        private void advance() {
            while (iterator.hasNext()) {
                currentField = iterator.next();

                // We want to skip implicit fields. For instance, the Oracle JVM generates a field "this$0" that
                // references the enclosing class. See also §3.8 in the Java Language Specification.
                if (!currentField.getName().contains("$")) {
                    // Stop if even the declared (static) type is at least as wide as the type we are looking for
                    if (clazz.isAssignableFrom(currentField.getType())) {
                        return;
                    }
                }
            }
            currentField = null;
        }

        @Override
        public boolean hasNext() {
            return this.currentField != null;
        }

        @Override
        public Field next() {
            // Precondition: advance() was called, i.e., valueOfCurrentField() is of type T

            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Field returnValue = currentField;
            advance();
            return returnValue;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FilteredIterator<T extends U, U, V> implements Iterator<V> {
        private final FilteredFieldIterator<T, U> fieldIterator;

        private FilteredIterator(T object, Class<U> upperBound, Class<V> clazz) {
            this.fieldIterator = new FilteredFieldIterator<>(object, upperBound, clazz);
        }

        @Override
        public boolean hasNext() {
            return fieldIterator.hasNext();
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Field currentField = fieldIterator.next();
            @SuppressWarnings({ "unchecked", "UnnecessaryLocalVariable" })
            V valueOfCurrentField = (V) valueOfField(fieldIterator.object, currentField);
            return valueOfCurrentField;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns an {@link Iterable} over all fields whose declared raw type is at least as narrow as the given raw type.
     *
     * @param object object whose fields to iterate over
     * @param upperBound only scan fields that are declared by this class or subclasses
     * @param clazz raw type to look for (including subclasses)
     * @param <T> type of {@code object}
     * @param <U> type of upper bound
     * @return iterable over fields in class
     */
    static <T extends U, U> Iterable<Field> filteredFields(final T object, final Class<U> upperBound,
        final Class<?> clazz) {

        return new Iterable<Field>() {
            @Override
            public Iterator<Field> iterator() {
                return new FilteredFieldIterator<>(object, upperBound, clazz);
            }
        };
    }

    /**
     * Returns an {@link Iterable} over the value of all fields whose declared raw type is at least as narrow as the
     * given raw type.
     *
     * @param object object whose field values to iterate over
     * @param upperBound only scan fields that are declared by this class or subclasses
     * @param clazz raw type to look for (including subclasses)
     * @param <T> type of {@code object}
     * @param <U> type of upper bound
     * @return iterable over values of object
     */
    static <T extends U, U, V> Iterable<V> filteredValues(final T object, final Class<U> upperBound,
        final Class<V> clazz) {

        return new Iterable<V>() {
            @Override
            public Iterator<V> iterator() {
                return new FilteredIterator<>(object, upperBound, clazz);
            }
        };
    }
}
