package com.svbio.cloudkeeper.model.api;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.immutable.element.Key;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Marshal context.
 *
 * <p>This interface provides methods available to {@link Marshaler} instances when marshaling Java objects. For
 * every implementing subclass there is typically a corresponding {@link UnmarshalContext} subclass that must be
 * used for unmarshaling.
 *
 * <p>Marshal contexts are used to transform Java objects into a tree representation consisting of object nodes and
 * byte-sequence nodes. This interface does not specify the precise marshaled form, but it does define the (virtual)
 * <em>marshaling tree</em> that is induced by calls to its methods when marshaling a Java object. The nodes in a
 * marshaling tree have labels that are either a simple name, an index, or empty. Each node is of one of four kinds:
 * <ul><li>
 *     A <em>byte-sequence node</em> is a leaf node that represents a byte sequence. It is induced by a call to
 *     {@link #newOutputStream(Key)} or {@link #putByteSequence(ByteSequence, Key)}, and the {@link Key} argument
 *     determines the label.
 * </li><li>
 *     An <em>raw-object node</em> is a leaf node that represents a Java object. It is induced by a call to
 *     {@link #writeObject(Object, Key)} if the marshal context decides to store the object "as-is" or in an
 *     implementation-defined way, without further recursive marshaling.
 * </li><li>
 *     An <em>marshaled-object node</em> is an internal node that represents a Java object. It has a non-empty set of
 *     child nodes, and all child nodes have a non-empty label. No two child nodes may have the same label. It is
 *     induced by a call to {@link #writeObject(Object, Key)} if the marshaling context decides to recursively marshal
 *     the object.
 * </li><li>
 *     A <em>marshaled-replacement-object node</em> is an internal node that represents a Java object. It has exactly
 *     one child node, which has an empty label. It is induced by a call to {@link #writeObject(Object, Key)} if the
 *     marshaling context decides to recursively marshal the object.
 * </li></ul>
 *
 * <p>Note that the distinction of marshaled-replacement-object nodes exists because some marshaling contexts may choose
 * to coalesce sequences of marshaled-replacement-object nodes when producing their specific marshaled form. As an
 * example, when writing a marshaling tree to the file system, it may be desirable to create directories only for
 * marshaled-object nodes and store sequences of marshaled-replacement-object nodes as metadata instead.
 *
 * <p>Instances of this interface are called from user code, so all methods perform rigorous validation of current state
 * and input arguments. User-defined {@link Marshaler} implementations are not supposed to close a marshal context.
 *
 * @see UnmarshalContext
 */
public interface MarshalContext extends Closeable {
    /**
     * Returns an {@link OutputStream} for writing a byte sequence with the given key.
     *
     * <p>It is generally the caller's responsibility to close the returned stream as soon as possible, in order to free
     * system resources. Therefore, this method should usually be used only in the header of a try-with-resources
     * statement. However, if the returned stream is <em>not</em> closed by the caller, it will be closed when the
     * marshaling context is closed. A {@link MarshalingException} will be thrown afterward to indicate the contract
     * violation.
     *
     * <p>This method provides "push"-semantics when writing a byte stream to a marshaling context. Method
     * {@link #putByteSequence(ByteSequence, Key)} may be used instead for "pull"-semantics, where the
     * marshaling context pulls the bytes to be written out of an input stream.
     *
     * @param key key of the new byte stream
     * @return output stream
     * @throws MarshalingException if either the given key or the empty key was passed to a method of this marshaling
     *     context before
     * @throws IOException If the operation failed because of an I/O error. Implementations should not usually
     *     catch this exception, but let it propagate.
     * @see UnmarshalContext#getByteSequence(Key)
     * @see ByteSequence#newInputStream()
     */
    OutputStream newOutputStream(Key key) throws IOException;

    /**
     * Writes the byte sequence with the given key.
     *
     * <p>This method provides "pull"-semantics when writing a byte sequence to a marshaling context. Method
     * {@link #newOutputStream(Key)} may be used instead for "push"-semantics, where the caller is responsible
     * for writing bytes to an output stream.
     *
     * @param byteSequence byte sequence
     * @param key key of the new byte sequence
     * @throws MarshalingException if either the given key or the empty key was passed to a method of this marshaling
     *     context before
     * @throws IOException If the operation failed because of an I/O error. Implementations should not usually
     *     catch this exception, but let it propagate.
     * @see UnmarshalContext#getByteSequence(Key)
     */
    void putByteSequence(ByteSequence byteSequence, Key key) throws IOException;

    /**
     * Writes the given object with the given key.
     *
     * <p>This interface does not specify how the {@link Marshaler} instance is determined that will be used for
     * marshaling. A possible strategy could be to maintain a list of {@link Marshaler} classes (as property of the
     * marshaling context), and then choose the first one that can handle the dynamic type of {@code object}, as
     * indicated by {@code object.getClass()}.
     *
     * <p>This method also takes appropriate measures to make the actually used {@link Marshaler} subclass known to
     * the {@link UnmarshalContext} corresponding to this marshaling context.
     *
     * <p>This method is potentially recursive. In particular, the marshaling context may invoke
     * {@link Marshaler#put(Object, MarshalContext)} with the given object as argument. However, this is by no
     * means required. For instance, a marshaling context may choose to create an object node in the marshaling tree;
     * that is, stop the recursion and store the object "as-is" or in an implementation-defined way.
     *
     * @param object object to be written
     * @param key index or simple name of the new byte stream, may be the empty key
     * @throws NullPointerException if an argument is null
     * @throws MarshalingException if the given key or the empty key was passed to a method of this marshal context
     *     before, or if marshaling the given object fails (for instance, when a user-defined
     *     {@link Marshaler#put(Object, MarshalContext)} implementation throws or causes a
     *     {@link MarshalingException})
     * @throws IOException If the operation failed because of an I/O error. Implementations should not usually
     *     catch this exception, but let it propagate.
     */
    void writeObject(Object object, Key key) throws IOException;
}
