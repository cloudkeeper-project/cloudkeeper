package xyz.cloudkeeper.model.api;

import net.florianschoppmann.java.reflect.ReflectionTypes;

import javax.annotation.Nullable;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.List;

import static xyz.cloudkeeper.model.api.Singletons.SERIALIAZATION_CLASS_MIRROR;
import static xyz.cloudkeeper.model.api.Singletons.TYPES;

/**
 * Marshaler for transforming Java objects into tree representations and back.
 *
 * <p>Marshaler implementations are used in order to transform a Java object into a tree representation suitable for
 * storage or transmission. Similarly, a marshaler implementation is also used to transform such a tree representations
 * back into a Java object.
 *
 * <p>Marshaler implementations are thread-safe.
 *
 * @param <T> type of objects that this marshaler implementation can handle
 *
 * @see MarshalContext
 * @see UnmarshalContext
 */
public interface Marshaler<T> {
    /**
     * Returns whether this marshaler can handle the given object.
     *
     * <p>If this method returns {@code true}, the type of the given object must be a subtype of {@code T} (that is, the
     * actual type argument for this interface).
     *
     * <p>The default implementation returns {@code true} if and only if {@code object.getClass()} represents a subtype
     * of {@code T}. Since the result of {@link Object#getClass()} has undergone type-erasure, generic type arguments
     * for {@code T} may yield unintended results. For instance, if {@code object} was an instance created as
     * {@code new ArrayList<String>()}, the class returned by {@code object.getClass()} would represent only the raw
     * type {@code ArrayList}, which is not a subtype of {@code List<String>}. Accordingly, the default implementation
     * would return {@code false} if the current instance is of type {@code Marshaler<List<String>>}.
     *
     * <p>See JLS §4.10.2 for a precise definitions of the subtype relationship.
     *
     * @param object object to be marshaled
     * @return whether this marshaler can handle the given object
     */
    default boolean canHandle(Object object) {
        Class<?> objectClass = object.getClass();
        Class<?> marshalerClass = getClass();
        @Nullable List<? extends TypeMirror> actualTypeParameters
            = TYPES.resolveActualTypeArguments(SERIALIAZATION_CLASS_MIRROR, TYPES.typeMirror(marshalerClass));
        if (actualTypeParameters == null || actualTypeParameters.size() != 1) {
            throw new IllegalArgumentException(String.format(
                "Could not resolve formal type parameter of %s to actual type arguments, given %s.",
                Marshaler.class, marshalerClass
            ));
        }
        TypeMirror actualTypeParameter = actualTypeParameters.get(0);
        return ReflectionTypes.getInstance().isSubtype(TYPES.typeMirror(objectClass), actualTypeParameter);
    }

    /**
     * Returns whether the given object (and all its transitive references) is immutable.
     *
     * <p>An object is <em>immutable</em> if and only if:
     * <ul><li>
     *     the object is thread-safe and
     * </li><li>
     *     during the lifetime of the current JVM, the state of the given object, all (observable) transitively
     *     referenced objects, and all transitively referenced external data (files, URLs, etc.) are guaranteed to never
     *     change.
     * </li></ul>
     * This definition typically excludes objects that are mutable or not <em>self-contained</em>: For example, a byte
     * sequence backed by a file in the filesystem is not self-contained (unless in a controlled area of the file
     * system), because the file may be moved or deleted. Hence, even if the Java object cannot be modified, it is not
     * immutable according to this definition.
     *
     * <p>If this method returns {@code true}, this allows marshal contexts to just store the given object (instead of a
     * marshaled tree representation) if marshaling is not needed.
     *
     * @param object object to be examined
     * @return whether the given object is immutable
     */
    boolean isImmutable(T object);

    /**
     * Writes the given object within the current marshal context.
     *
     * @param object object to be written
     * @param context marshal context that provides methods for writing individual pieces of the given object
     * @throws MarshalingException if the object cannot be marshaled because of invalid arguments or because of
     *     implementation-defined reasons
     * @throws IOException if writing the object caused an I/O exception
     */
    void put(T object, MarshalContext context) throws IOException;

    /**
     * Reads and returns an object from the unmarshal context.
     *
     * <p>If a required class cannot be found or instantiated, a {@link MarshalingException} will be thrown, which
     * must contain the original exception (such as {@link ClassNotFoundException}) as cause.
     *
     * @param context unmarshal context that provides methods for reading individual pieces of the new object
     * @return the new object
     * @throws MarshalingException if an object could not be unmarshaled because of invalid arguments or because of
     *     implementation-defined reasons
     * @throws IOException if reading the object caused an I/O exception
     */
    T get(UnmarshalContext context) throws IOException;
}
