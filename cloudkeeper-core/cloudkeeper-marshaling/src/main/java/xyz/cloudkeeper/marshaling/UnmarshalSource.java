package xyz.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.MarshalingException;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.immutable.element.Key;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Unmarshal source (delegate) for {@link DelegatingUnmarshalContext}.
 *
 * <p>Instances of this interface provide a high-level parametrization for {@link DelegatingUnmarshalContext} instances.
 * While the methods in this interface have similar names and signatures as {@link UnmarshalContext} methods, the
 * requirements for implementors are much lower: In particular, methods of this interface are intended to be called from
 * library code and not from user code, so valid arguments or correct use may be assumed.
 *
 * <p>There is a one-to-one correspondence between {@link MarshalContext} instances and instances of this interface
 * created during marshaling. That is, every call to {@link MarshalContext#writeObject(Object, Key)} will cause
 * {@link #resolve(Key)} to be invoked.
 *
 * <p>An unmarshal source is typically used to create an unmarshal context using
 * {@link DelegatingUnmarshalContext#create(UnmarshalSource, ClassLoader)}. In this case, there are only three possible
 * sequences of method invocations:
 * <ul><li>
 *     {@link #getObject()} is called and returns a non-null object. That is, the current instance represents a
 *     raw-object node.
 * </li><li>
 *     {@link #getObject()} is called and returns null. Subsequently, {@link #getMarshaler()} is called, followed by one
 *     (and only one) call to either {@link #resolve(Key)} or {@link #getByteSequence(Key)} with the empty key. That is,
 *     the current instance represents a marshaled-replacement-object node.
 * </li><li>
 *     {@link #getObject()} is called and returns null. Subsequently, {@link #getMarshaler()} is called, followed by a
 *     non-empty sequence of calls to either {@link #resolve(Key)} or {@link #getByteSequence(Key)}, each with a
 *     non-empty key. That is, the current instance represents a marshaled-object node.
 * </li></ul>
 *
 * <p>If the instances of a subclass are only passed to
 * {@link DelegatingUnmarshalContext#create(UnmarshalSource, ClassLoader)} and used in no other way, it is not necessary
 * for that subclass to validate state or arguments. It is acceptable to unconditionally throw
 * {@link UnsupportedOperationException} in these "impossible" cases.
 */
public interface UnmarshalSource {
    /**
     * Returns the marshaler associated with the current unmarshal source.
     *
     * @return the persistence plugin
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if called in an "impossible" state (see class-level description)
     */
    Marshaler<?> getMarshaler() throws IOException;

    /**
     * Returns the object represented by the current unmarshal source, or {@code null} to indicate that instead
     * {@link Marshaler#get(UnmarshalContext)} should be called on the marshaler returned by {@link #getMarshaler()}.
     *
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    @Nullable
    Object getObject() throws IOException;

    /**
     * Returns the byte sequence identified by the given key.
     *
     * @param key key identifying the requested byte sequence
     * @return the byte sequence represented by the given key, or {@code null} if the given key does not represent a
     *     byte sequence
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if called in an "impossible" state (see class-level description)
     *
     * @see UnmarshalContext#getByteSequence(Key)
     */
    @Nullable
    ByteSequence getByteSequence(Key key) throws IOException;

    /**
     * Returns a new child unmarshal source for the given key.
     *
     * @param key index or simple name for the load context, may also be the empty key
     * @return the new child unmarshal context for the given key, or {@code null} if the given key does not represent a
     *     node in the tree representation
     * @throws MarshalingException if an marshaling error occurs
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if called in an "impossible" state (see class-level description)
     */
    @Nullable
    UnmarshalSource resolve(Key key) throws IOException;
}
