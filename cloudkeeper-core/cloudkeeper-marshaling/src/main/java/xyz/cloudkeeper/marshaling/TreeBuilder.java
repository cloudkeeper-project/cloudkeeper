package xyz.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.immutable.element.Key;

import java.io.IOException;
import java.util.Map;

/**
 * Tree builder.
 *
 * <p>This interface is used to transform the (virtual) marshaling tree (see {@link MarshalContext} for a definition)
 * into an actual tree-like data structure.
 *
 * <p>A tree builder is typically used to create a marshal target using
 * {@link TreeBuilderMarshalTarget#create(TreeBuilder)}. In this case, there are only four possible sequences of method
 * invocations:
 * <ul><li>
 *     An invocation of {@link #shouldMarshal(Marshaler, Object)} that returns {@code false}, followed by
 *     {@link #createObjectNode(Marshaler, Object)}.
 * </li><li>
 *     An invocation of {@link #shouldMarshal(Marshaler, Object)} that returns {@code true}, followed by a single
 *     invocation of {@link #resolve(Key)} with the empty key, followed by
 *     {@link #createRedirectNode(Marshaler, Object)}.
 * </li><li>
 *     An invocation of {@link #shouldMarshal(Marshaler, Object)} that returns {@code true}, followed by a non-empty
 *     sequence of {@link #resolve(Key)} invocations each with a non-empty key, followed by
 *     {@link #createParentNode(Marshaler, Map)}.
 * </li><li>
 *     An invocation of {@link #createByteSequenceNode(ByteSequence)}.
 * </li></ul>
 *
 * <p>If the instances of a subclass are only passed to {@link TreeBuilderMarshalTarget#create(TreeBuilder)} and used in
 * no other way, it is not necessary for that subclass to validate state or arguments.
 *
 * @param <N> type of the nodes in the built tree
 * @see TreeBuilderMarshalTarget#create(TreeBuilder)
 */
public interface TreeBuilder<N> {
    /**
     * Returns whether the given marshaler should be used to marshal the given immutable object ({@code true}), or if
     * the object should be stored "as-is" ({@code false}).
     *
     * @param marshaler marshaler
     * @param object object to be examined
     * @return whether the given object should be marshaled
     */
    boolean shouldMarshal(Marshaler<?> marshaler, Object object);

    /**
     * Returns a new tree node that represents an object node in the marshaling tree.
     *
     * @param marshaler Marshaler for the object represented by the node to be returned. This would have been used to
     *     marshal the given object if, for instance, {@link #shouldMarshal(Marshaler, Object)} had returned
     *     {@code true}.
     * @param object object
     * @return the new tree node
     */
    N createObjectNode(Marshaler<?> marshaler, Object object);

    /**
     * Returns a new tree node that represents a byte-sequence node in the marshaling tree.
     *
     * @param byteSequence byte sequence, not guaranteed to be self-contained
     * @return the new tree node
     * @throws IOException if an I/O error occurs
     */
    N createByteSequenceNode(ByteSequence byteSequence) throws IOException;

    /**
     * Returns a new tree node that represents a marshaled-replacement-object node in the marshaling tree.
     *
     * @param marshaler marshaler for the object represented by the node to be returned
     * @param node previously created tree node that represents the single child of the marshaled-replacement-object
     *     node to be returned
     * @return the new tree node
     */
    N createRedirectNode(Marshaler<?> marshaler, N node);

    /**
     * Returns a new tree node that represents a marshaled-object node in the marshaling tree.
     *
     * <p>This method is guaranteed to be called only for targets that are either the root target or a target created
     * by passing a non-empty key to {@link #resolve(Key)}.
     *
     * @param marshaler marshaler for the object represented by the node to be returned
     * @param children previously created tree nodes that represent the child nodes of the marshaled-object node to be
     *     returned
     * @return the new tree node
     */
    N createParentNode(Marshaler<?> marshaler, Map<Key, N> children);

    /**
     * Returns a subtree builder for the given key.
     *
     * @param key key
     * @return the subtree builder
     */
    TreeBuilder<N> resolve(Key key);
}
