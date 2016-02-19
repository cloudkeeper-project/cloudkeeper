package xyz.cloudkeeper.model.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * An immutable, random-access list. Does not permit null elements.
 *
 * <p>Unlike {@link java.util.Collections#unmodifiableList(List)}, which is a view of a separate list that can still
 * change, an instance of this class is backed by its own private data and will never change.
 *
 * @param <E> the type of elements in this list
 */
public abstract class ImmutableList<E> extends AbstractList<E> implements Serializable {
    private static final long serialVersionUID = -1141421109703076753L;

    private static final String NO_NULL_ELEMENTS_MESSAGE
        = String.format("Instances of %s do not permit null elements.", ImmutableList.class);

    private ImmutableList() { }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <E> ImmutableList<E> copyOf(Collection<? extends E> original) {
        Objects.requireNonNull(original);
        if (original instanceof SubList<?>) {
            SubList<? extends E> list = (SubList<? extends E>) original;
            return new ArrayBackedList<>(
                Arrays.copyOfRange(list.originalArray, list.originalFromIndex, list.originalToIndex)
            );
        } else if (original instanceof ImmutableList<?>) {
            return (ImmutableList<E>) original;
        } else if (original.isEmpty()) {
            return of();
        } else if (original.size() == 1) {
            @Nullable E originalElement = original instanceof List<?>
                ? ((List<? extends E>) original).get(0)
                : original.iterator().next();
            if (originalElement == null) {
                throw new IllegalArgumentException(NO_NULL_ELEMENTS_MESSAGE);
            }
            return new SingletonList<>(originalElement);
        } else {
            Object[] newArray = new Object[original.size()];
            int i = 0;
            for (@Nullable E element: original) {
                if (element == null) {
                    throw new IllegalArgumentException(NO_NULL_ELEMENTS_MESSAGE);
                }
                newArray[i] = element;
                i++;
            }
            return new ArrayBackedList<>(newArray);
        }
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new {@code ImmutableList}.
     *
     * <p>There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code List} returned; if more
     * control over the returned {@code List} is required, use {@link #toCollection(Supplier)}.
     *
     * @param <E> the type of the input elements
     * @return a {@code Collector} which collects all the input elements into an immutable list, in encounter order
     */
    public static <E> Collector<E, ?, ImmutableList<E>> collector() {
        return Collector.<E, ArrayList<E>, ImmutableList<E>>of(
            ArrayList::new,
            List::add,
            (left, right) -> { left.addAll(right); return left; },
            ImmutableList::copyOf
        );
    }

    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of() {
        return (ImmutableList<E>) EmptyList.INSTANCE;
    }

    public static <E> ImmutableList<E> of(E singleton) {
        return new SingletonList<>(singleton);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list, or -1 if this list does not
     * contain the element.
     *
     * <p>More formally, returns the lowest index {@code i} such that {@code object.equals(get(i))}, or -1 if there is
     * no such index.
     *
     * @param object element to search for
     * @return the index of the first occurrence of the specified element in this list, or -1 if this list does not
     *     contain the element (a {@code null} argument will always result in -1 being returned)
     */
    @Override
    public abstract int indexOf(@Nullable Object object);

    /**
     * Returns the index of the last occurrence of the specified element in this list, or -1 if this list does not
     * contain the element.
     *
     * <p>More formally, returns the highest index {@code i} such that {@code object.equals(get(i))}, or -1 if there is
     * no such index.
     *
     * @param object element to search for
     * @return the index of the last occurrence of the specified element in this list, or -1 if this list does not
     *      contain the element (a {@code null} argument will always result in -1 being returned)
     */
    @Override
    public abstract int lastIndexOf(@Nullable Object object);

    @Override
    public abstract ImmutableList<E> subList(int fromIndex, int toIndex);

    private static String outOfBoundsMsg(int index, int size) {
        return "Index: " + index + ", Size: " + size;
    }

    private static void requireValidIndex(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index, size));
        }
    }

    private static void requireValidSubListArguments(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        } else if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        } else if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ')');
        }
    }

    static final class EmptyList<E> extends ImmutableList<E> {
        private static final long serialVersionUID = -2157527890236446801L;

        private static final ImmutableList<?> INSTANCE = new EmptyList<>();

        private static final Object[] EMPTY_ARRAY = {};

        private Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }

        @Override
        public E get(int index) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index, 0));
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int indexOf(@Nullable Object object) {
            Objects.requireNonNull(object);
            return -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object object) {
            Objects.requireNonNull(object);
            return -1;
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            requireValidSubListArguments(fromIndex, toIndex, 0);
            return this;
        }

        @Override
        public Object[] toArray() {
            return EMPTY_ARRAY;
        }

        @Override
        public <T> T[] toArray(T[] targetArray) {
            Objects.requireNonNull(targetArray);
            return Objects.requireNonNull(targetArray);
        }
    }

    static final class SingletonList<E> extends ImmutableList<E> {
        private static final long serialVersionUID = -7452368762256835352L;

        private final E singleton;

        private SingletonList(E singleton) {
            Objects.requireNonNull(singleton, NO_NULL_ELEMENTS_MESSAGE);
            this.singleton = singleton;
        }

        @Override
        public E get(int index) {
            requireValidIndex(index, 1);
            return singleton;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public int indexOf(@Nullable Object object) {
            return singleton.equals(object)
                ? 0
                : -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object object) {
            return singleton.equals(object)
                ? 0
                : -1;
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            requireValidSubListArguments(fromIndex, toIndex, 1);
            return fromIndex == 0 && toIndex == 1
                ? this
                : ImmutableList.<E>of();
        }
    }

    static final class ArrayBackedList<E> extends ImmutableList<E> {
        private static final long serialVersionUID = 4212701518478728878L;

        private final Object[] array;

        private ArrayBackedList(Object[] array) {
            this.array = array;
        }

        @SuppressWarnings("unchecked")
        @Override
        public E get(int index) {
            // If an index is out of bounds, an ArrayIndexOutOfBounds exceptions will be thrown.
            return (E) array[index];
        }

        @Override
        public int size() {
            return array.length;
        }

        @Override
        public int indexOf(@Nullable Object object) {
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(object)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object object) {
            for (int i = array.length - 1; i >= 0; --i) {
                if (array[i].equals(object)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            if (fromIndex == 0 && toIndex == array.length) {
                return this;
            }

            requireValidSubListArguments(fromIndex, toIndex, array.length);
            if (fromIndex == toIndex) {
                return ImmutableList.of();
            } else if (fromIndex + 1 == toIndex) {
                @SuppressWarnings("unchecked")
                E element = (E) array[fromIndex];
                return new SingletonList<>(element);
            } else {
                return new SubList<>(array, fromIndex, toIndex);
            }
        }
    }

    static final class SubList<E> extends ImmutableList<E> {
        private static final long serialVersionUID = -1614112608924916458L;

        private final Object[] originalArray;
        private final int originalFromIndex;
        private final int originalToIndex;
        private final int size;

        private SubList(Object[] originalArray, int originalFromIndex, int originalToIndex) {
            assert originalFromIndex >= 0
                && originalFromIndex <= originalToIndex
                && originalToIndex <= originalArray.length;

            this.originalArray = originalArray;
            this.originalFromIndex = originalFromIndex;
            this.originalToIndex = originalToIndex;
            size = originalToIndex - originalFromIndex;
        }

        @SuppressWarnings("unchecked")
        @Override
        public E get(int index) {
            requireValidIndex(index, size);
            return (E) originalArray[originalFromIndex + index];
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int indexOf(@Nullable Object object) {
            for (int i = originalFromIndex; i < originalToIndex; i++) {
                if (originalArray[i].equals(object)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int lastIndexOf(@Nullable Object object) {
            Objects.requireNonNull(object);
            for (int i = originalToIndex - 1; i >= originalFromIndex; --i) {
                if (originalArray[i].equals(object)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            if (fromIndex == 0 && toIndex == size) {
                return this;
            }

            requireValidSubListArguments(fromIndex, toIndex, size);
            if (fromIndex == toIndex) {
                return ImmutableList.of();
            } else if (fromIndex + 1 == toIndex) {
                @SuppressWarnings("unchecked")
                E element = (E) originalArray[originalFromIndex + fromIndex];
                return new SingletonList<>(element);
            } else {
                return new SubList<>(originalArray, originalFromIndex + fromIndex, originalFromIndex + toIndex);
            }
        }
    }
}
