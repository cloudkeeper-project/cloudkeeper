package xyz.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.MarshalingException;
import xyz.cloudkeeper.model.immutable.element.Key;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Marshal target (delegate) for {@link DelegatingMarshalContext}.
 *
 * <p>Instances of this interface provide a high-level parametrization for the (internal) {@link MarshalContext}
 * instances created by {@link DelegatingMarshalContext#marshal(Object, Collection, MarshalTarget)} and
 * {@link DelegatingMarshalContext#processMarshalingTree(MarshalingTreeNode.ObjectNode, Collection, MarshalTarget)}.
 * While the methods in this interface have similar names and signatures as {@link MarshalContext} methods, the
 * requirements for implementors are much lower: In particular, methods of this interface are intended to be called from
 * CloudKeeper code and not from user code, so valid arguments or correct use may be assumed.
 *
 * <p>There is a one-to-one correspondence between {@link MarshalContext} instances and instances of this interface
 * created during marshaling. That is, every call to {@link MarshalContext#writeObject(Object, Key)} will cause
 * {@link #resolve(Key)} to be invoked.
 *
 * <p>After instance creation, the first method called is always {@link #start(Marshaler)}. The following sequences of
 * method calls are possible between this call and {@link #finish()}:
 * <ul><li>
 *     {@link #putImmutableObject(Object)}. This induces a marshaled-replacement-object node (if
 *     {@link ObjectAction#MARSHAL} is returned) or an object node (if {@link ObjectAction#NONE} is returned) in the
 *     marshaling tree (see {@link MarshalContext} for a definition).
 * </li><li>
 *     {@link #marshalerChain(List)} followed by a call to {@link #putByteSequence(ByteSequence, Key)} or
 *     {@link #newOutputStream(Key)} with an empty key. This induces a byte-sequence node in the marshaling tree.
 * </li><li>
 *     {@link #marshalerChain(List)} followed by a non-empty sequence of calls to
 *     {@link #putImmutableObject(Object)}, {@link #putByteSequence(ByteSequence, Key)}, or
 *     {@link #newOutputStream(Key)} with non-empty keys each. Each {@link #putImmutableObject(Object)} call induces a
 *     marshaled-as-object node and each other put-call induces a byte-sequence node in the marshaling tree.
 * </li></ul>
 */
public interface MarshalTarget {
    /**
     * Called before any other methods are called.
     *
     * <p>This method associates a persistence plugin with this store context. It guaranteed to be called only once.
     *
     * @param marshaler the persistence plugin
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    void start(Marshaler<?> marshaler) throws IOException;

    /**
     * Called if this marshaling target represents a marshaled-as-object node or if the enclosing node is a
     * marshaled-as-replaced-object node.
     *
     * <p>This method is called before any method other than {@link #start(Marshaler)}. If this method is called, it is
     * guaranteed that {@link #resolve(Key)} will not be called with the empty key.
     *
     * @param marshalerChain The marshaling-plugin-declaration "backtrace". This list contains the marshaling plugin
     *     declarations of all marshaled-as-replacement-object nodes that enclose the current node (starting with the
     *     most distant ancestor). If the current node is a marshaled-as-object node, then the list also contains its
     *     corresponding marshaling plugin declaration as last element.
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    default void marshalerChain(List<Marshaler<?>> marshalerChain) throws IOException { }

    /**
     * Returns an {@link OutputStream} for writing a byte sequence that will be stored for this marshaling target.
     *
     * <p>The returned stream is guaranteed to be closed properly by the caller. Implementations should not close the
     * returned stream in {@link #finish()}.
     *
     * @param key index or simple name of the new byte sequence, may also be the empty key
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     *
     * @see MarshalContext#newOutputStream(Key)
     */
    OutputStream newOutputStream(Key key) throws IOException;

    /**
     * Stores the given byte sequence for this marshaling target.
     *
     * @param byteSequence byte sequence
     * @param key index or simple name of the new byte sequence, may also be the empty key
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     *
     * @see MarshalContext#putByteSequence(ByteSequence, Key)
     */
    void putByteSequence(ByteSequence byteSequence, Key key) throws IOException;

    /**
     * Action describing how {@link DelegatingMarshalContext#marshal(Object, Collection, MarshalTarget)} should proceed
     * after the call to {@link #putImmutableObject(Object)}.
     */
    enum ObjectAction {
        /**
         * Proceed directly by finishing the store context. This means that the next store-context method being
         * called is {@link #finish()}.
         */
        NONE,

        /**
         * Proceed by marshaling object, in the same manner as if {@link Marshaler#isImmutable(Object)} had
         * returned {@code false}. This will result in {@link Marshaler#put(Object, MarshalContext)} being
         * called.
         */
        MARSHAL
    }

    /**
     * Provides an opportunity to create an object node in the marshaling tree.
     *
     * <p>This method gives a marshaling target the opportunity to deal with an immutable object directly. For instance,
     * an in-memory store context may simply store an object reference and forgo further marshaling for this object. In
     * this case the method would return {@link ObjectAction#NONE}. Alternatively, if the object should be further
     * marshaled, this method must return {@link ObjectAction#MARSHAL}.
     *
     * <p>It is guaranteed that the given object is immutable according to {@link Marshaler#isImmutable(Object)}.
     *
     * @param object immutable object
     * @return action describing how to proceed
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    ObjectAction putImmutableObject(Object object) throws IOException;

    /**
     * Returns a child marshal target for the given key.
     *
     * <p>If called with an empty key, then it is guaranteed that neither {@link #newOutputStream(Key)} nor
     * {@link #putByteSequence(ByteSequence, Key)} will be called for the returned marshaling target. Moreover, in this
     * case, this method has not been called before and will not be called again.
     *
     * @param key index or simple name for the store context, may also be the empty key
     * @return the new child store context for the given key
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    MarshalTarget resolve(Key key) throws IOException;

    /**
     * Called when the marshal target is closed.
     *
     * <p>This method is called both in case of success and in case marshaling has failed previously (that is, if any of
     * the possible sequences of method invocations ended prematurely).
     *
     * <p>It is guaranteed that this method is called only <em>after</em> all marshaling targets created with
     * {@link #resolve(Key)} have been finished.
     *
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    void finish() throws IOException;
}
