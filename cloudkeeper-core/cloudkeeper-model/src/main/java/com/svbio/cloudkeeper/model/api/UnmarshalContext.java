package com.svbio.cloudkeeper.model.api;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.immutable.element.Key;

import java.io.IOException;

/**
 * Unmarshal context.
 *
 * <p>This interface provides methods available to {@link Marshaler} instances when unmarshaling a tree representation
 * back into a Java object. For every implementing subclass there is typically a corresponding {@link MarshalContext}
 * subclass that must be used for marshaling.
 *
 * <p>The corresponding {@link MarshalContext} needs to make known which {@link Marshaler} instance should be used to
 * unmarshal the tree representation.
 *
 * <p>For instance, a simple strategy a marshal context could employ is to write the name of the {@link Marshaler} class
 * next to the collection of byte sequences. The {@link #readObject(Key)} in the unmarshal context could simply read
 * this class name first, in order to then call the {@link Marshaler#get(UnmarshalContext)} method of the corresponding
 * {@link Marshaler} subclass.
 *
 * <p>Nodes in the tree representation are labeled with a {@link Key}. The key arguments passed to methods in this
 * interface must match exactly those passed to the corresponding methods in {@link MarshalContext} during marshaling.
 *
 * @see MarshalContext
 */
public interface UnmarshalContext {
    /**
     * Returns the byte sequence identified by the given key.
     *
     * <p>The {@link ByteSequence} instance returned by this method will remain valid even after the current unmarshal
     * context is closed. Callers of this method ({@link Marshaler} implementations) are free to store the returned
     * {@link ByteSequence} instance or pass it to other components. Therefore, any {@link ByteSequence} instance
     * returned by this method is expected to not contain unneeded references that could potentially result in a memory
     * leak.
     *
     * <p>Since the returned {@link ByteSequence} instance does not depend on the current unmarshal context, any
     * {@link java.io.InputStream} obtained via {@link ByteSequence#newInputStream()} will <em>not</em> automatically be
     * closed. It is therefore a good idea to only call {@link ByteSequence#newInputStream()} in the header of a
     * try-with-resources statement.
     *
     * @param key key of the existing byte sequence
     * @return input stream
     * @throws MarshalingException if the given key cannot be found, or if the tree node corresponding to the given key
     *     is of the wrong type (for instance, a leaf node instead of internal node, or vice versa)
     * @throws IOException If the operation failed because of an I/O error. Callers should not usually catch this
     *     exception, but let it propagate.
     * @see MarshalContext#newOutputStream(Key)
     */
    ByteSequence getByteSequence(Key key) throws IOException;

    /**
     * Returns the class loader that should be used for loading classes referenced by marshaled objects.
     *
     * @return the class loader
     * @throws IOException If the operation failed because of an I/O error. Callers should not usually catch this
     *     exception, but let it propagate.
     */
    ClassLoader getClassLoader() throws IOException;

    /**
     * Reads the object with the specified key.
     *
     * <p>This method is potentially recursive. In particular, it may call
     * {@link Marshaler#get(UnmarshalContext)}.
     *
     * @param key key of the object to unmarshal
     * @return object read from the specified key
     * @throws MarshalingException if the given key cannot be found, or if the tree node corresponding to the given key
     *     is of the wrong type (for instance, a leaf node instead of internal node, or vice versa), or if unmarshaling
     *     the given object fails (for instance, when a user-defined {@link Marshaler#get(UnmarshalContext)}
     *     implementation throws or causes a {@link MarshalingException} or if a marshaler cannot be found)
     * @throws IOException If the operation failed because of an I/O error. Implementations should not usually
     *     catch this exception, but let it propagate.
     * @see MarshalContext#writeObject(Object, Key)
     */
    Object readObject(Key key) throws IOException;
}
