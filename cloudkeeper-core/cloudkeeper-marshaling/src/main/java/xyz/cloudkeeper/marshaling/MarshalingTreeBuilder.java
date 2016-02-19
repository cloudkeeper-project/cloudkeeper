package xyz.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ByteSequenceNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNodeImpl.ByteSequenceNodeImpl;
import xyz.cloudkeeper.marshaling.MarshalingTreeNodeImpl.MarshaledAsObjectNodeImpl;
import xyz.cloudkeeper.marshaling.MarshalingTreeNodeImpl.MarshaledAsReplacementObjectNodeImpl;
import xyz.cloudkeeper.marshaling.MarshalingTreeNodeImpl.RawObjectNodeImpl;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.immutable.element.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder of a marshaling tree.
 *
 * <p>This class can be used to build a marshaling tree in memory consisting of nodes of type
 * {@link MarshalingTreeNode}. All nodes of a tree built using this class implement {@link Object#equals(Object)} and
 * {@link Object#hashCode()}. See also {@link MarshalingTreeNode.ByteSequenceNode#of(ByteSequence)}.
 *
 * <p>Instance of this class are immutable.
 */
public final class MarshalingTreeBuilder implements TreeBuilder<MarshalingTreeNode> {
    private final ShouldMarshalPredicate shouldMarshalPredicate;
    private final List<Key> path;

    private MarshalingTreeBuilder(ShouldMarshalPredicate shouldMarshalPredicate, List<Key> path) {
        this.shouldMarshalPredicate = shouldMarshalPredicate;
        this.path = path;
    }

    /**
     * Creates a new marshaling-tree builder that wraps the given should-marshal predicate.
     *
     * @param shouldMarshalPredicate predicate that determines the result of
     *     {@link TreeBuilder#shouldMarshal(Marshaler, Object)}
     * @return the new tree builder
     */
    public static MarshalingTreeBuilder create(ShouldMarshalPredicate shouldMarshalPredicate) {
        Objects.requireNonNull(shouldMarshalPredicate);
        return new MarshalingTreeBuilder(shouldMarshalPredicate, Collections.emptyList());
    }

    /**
     * Marshals an object using the given tree builder.
     *
     * <p>This method performs the equivalent of the following steps:
     * <ul><li>
     *     create the marshaling-tree builder using {@link #create(ShouldMarshalPredicate)},
     * </li><li>
     *     marshal the object using {@link TreeBuilderMarshalTarget#marshal(Object, Collection, TreeBuilder)} and
     *     return the result.
     * </li></ul>
     *
     * @param object object to marshal
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link xyz.cloudkeeper.model.api.MarshalContext#writeObject(Object, Key)} recursively
     * @param shouldMarshalPredicate predicate that determines the result of
     *     {@link TreeBuilder#shouldMarshal(Marshaler, Object)}
     * @return the built marshaling tree
     * @throws xyz.cloudkeeper.model.api.MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static ObjectNode marshal(Object object, Collection<? extends Marshaler<?>> marshalers,
            ShouldMarshalPredicate shouldMarshalPredicate) throws IOException {
        MarshalingTreeBuilder treeBuilder = create(shouldMarshalPredicate);
        return (ObjectNode) TreeBuilderMarshalTarget.marshal(object, marshalers, treeBuilder);
    }

    /**
     * Writes the given marshaling tree to the given marshal .
     *
     * <p>Any contained {@link MarshalingTreeNode.RawObjectNode} will be marshaled in the same way as by
     * {@link #marshal(Object, Collection, ShouldMarshalPredicate)}.
     *
     * <p>This method performs the equivalent of the following steps:
     * <ul><li>
     *     create a marshaling-tree builder using {@link #create(ShouldMarshalPredicate)},
     * </li><li>
     *     process the marshaling tree using
     *     {@link TreeBuilderMarshalTarget#processMarshalingTree(ObjectNode, Collection, TreeBuilder)} and return the
     *     result.
     * </li></ul>
     *
     * @param tree marshaling tree that needs to be transformed and marshaled
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link xyz.cloudkeeper.model.api.MarshalContext#writeObject(Object, Key)} recursively
     * @param shouldMarshalPredicate predicate that determines the result of
     *     {@link TreeBuilder#shouldMarshal(Marshaler, Object)}
     * @throws xyz.cloudkeeper.model.api.MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static ObjectNode processMarshalingTree(ObjectNode tree, Collection<? extends Marshaler<?>> marshalers,
            ShouldMarshalPredicate shouldMarshalPredicate) throws IOException {
        MarshalingTreeBuilder treeBuilder = create(shouldMarshalPredicate);
        return (ObjectNode) TreeBuilderMarshalTarget.processMarshalingTree(tree, marshalers, treeBuilder);
    }


    @Override
    public boolean shouldMarshal(Marshaler<?> marshaler, Object object) {
        return shouldMarshalPredicate.test(Collections.unmodifiableList(path), marshaler, object);
    }

    @Override
    public RawObjectNode createObjectNode(Marshaler<?> marshaler, Object object) {
        return new RawObjectNodeImpl(marshaler, object);
    }

    @Override
    public ByteSequenceNode createByteSequenceNode(ByteSequence byteSequence) {
        return new ByteSequenceNodeImpl(byteSequence);
    }

    @Override
    public MarshaledReplacementObjectNode createRedirectNode(Marshaler<?> marshaler, MarshalingTreeNode child) {
        return new MarshaledAsReplacementObjectNodeImpl(marshaler, child);
    }

    @Override
    public MarshaledObjectNode createParentNode(Marshaler<?> marshaler, Map<Key, MarshalingTreeNode> children) {
        return new MarshaledAsObjectNodeImpl(marshaler, children);
    }

    @Override
    public MarshalingTreeBuilder resolve(Key key) {
        List<Key> childPath = new ArrayList<>(path.size() + 1);
        childPath.addAll(path);
        childPath.add(key);
        return new MarshalingTreeBuilder(shouldMarshalPredicate, childPath);
    }
}
