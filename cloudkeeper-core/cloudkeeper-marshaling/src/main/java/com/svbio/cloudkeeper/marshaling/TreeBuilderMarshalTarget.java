package com.svbio.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.util.ByteSequences;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tree-builder marshal target.
 *
 * <p>This class implements a {@link MarshalTarget} that facilitates transforming the (conceptual) marshaling tree into
 * a tree-like data structure.
 *
 * <p>Instances of this class have a life cycle. After calling {@link #finish()}, instances of this class are
 * effectively immutable and thread-safe (but not before).
 *
 * @param <N> type of the nodes in the built tree
 */
public final class TreeBuilderMarshalTarget<N> implements MarshalTarget {
    private final TreeBuilder<N> treeBuilder;
    @Nullable private Marshaler<?> marshaler;
    @Nullable private NodeBuilder<N> nodeBuilder;
    @Nullable private volatile N node;

    private TreeBuilderMarshalTarget(TreeBuilder<N> treeBuilder) {
        this.treeBuilder = treeBuilder;
    }

    /**
     * Creates a new tree-builder marshal target that wraps the given tree builder.
     *
     * @param treeBuilder tree builder
     * @param <N> type of the nodes in the tree
     * @return the new marshal target
     */
    public static <N> TreeBuilderMarshalTarget<N> create(TreeBuilder<N> treeBuilder) {
        Objects.requireNonNull(treeBuilder);
        return new TreeBuilderMarshalTarget<>(treeBuilder);
    }

    /**
     * Marshals an object using the given tree builder.
     *
     * <p>This method performs the equivalent of the following steps:
     * <ul><li>
     *     create the tree-builder marshal target using {@link #create(TreeBuilder)},
     * </li><li>
     *     marshal the object using {@link DelegatingMarshalContext#marshal(Object, Collection, MarshalTarget)},
     * </li><li>
     *     return the result of {@link #getTree()}.
     * </li></ul>
     *
     * @param object object to marshal
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link com.svbio.cloudkeeper.model.api.MarshalContext#writeObject(Object, Key)} recursively
     * @param treeBuilder tree builder
     * @return the built tree
     * @throws com.svbio.cloudkeeper.model.api.MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static <N> N marshal(Object object, Collection<? extends Marshaler<?>> marshalers,
            TreeBuilder<N> treeBuilder) throws IOException {
        TreeBuilderMarshalTarget<N> marshalTarget = create(treeBuilder);
        DelegatingMarshalContext.marshal(object, marshalers, marshalTarget);
        return marshalTarget.getTree();
    }

    /**
     * Writes the given marshaling tree to the given marshal .
     *
     * <p>Any contained {@link MarshalingTreeNode.RawObjectNode} will be marshaled in the same way as by
     * {@link #marshal(Object, Collection, TreeBuilder)}.
     *
     * <p>This method performs the equivalent of the following steps:
     * <ul><li>
     *     create a tree builder using {@link #create(TreeBuilder)},
     * </li><li>
     *     process the marshaling tree using
     *     {@link DelegatingMarshalContext#processMarshalingTree(ObjectNode, Collection, MarshalTarget)},
     * </li><li>
     *     return the result of {@link #getTree()}.
     * </li></ul>
     *
     * @param tree marshaling tree that needs to be transformed and marshaled
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link com.svbio.cloudkeeper.model.api.MarshalContext#writeObject(Object, Key)} recursively
     * @param treeBuilder tree builder
     * @throws com.svbio.cloudkeeper.model.api.MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static <N> N processMarshalingTree(ObjectNode tree, Collection<? extends Marshaler<?>> marshalers,
            TreeBuilder<N> treeBuilder) throws IOException {
        TreeBuilderMarshalTarget<N> marshalTarget = create(treeBuilder);
        DelegatingMarshalContext.processMarshalingTree(tree, marshalers, marshalTarget);
        return marshalTarget.getTree();
    }

    @Override
    public void start(Marshaler<?> marshaler) {
        if (this.marshaler != null) {
            throw new IllegalStateException("start() called more than once.");
        } else {
            this.marshaler = marshaler;
        }
    }

    private ParentNodeBuilder createParentNodeBuilder() {
        assert nodeBuilder == null || ParentNodeBuilder.class.isInstance(nodeBuilder);
        @Nullable ParentNodeBuilder parentNodeBuilder = (ParentNodeBuilder) nodeBuilder;
        if (parentNodeBuilder == null) {
            parentNodeBuilder = new ParentNodeBuilder();
            nodeBuilder = parentNodeBuilder;
        }
        return parentNodeBuilder;
    }

    private void createByteSequenceNodeBuilder(Key key, NodeBuilder<N> childNodeBuilder) throws IOException {
        assert nodeBuilder == null || ParentNodeBuilder.class.isInstance(nodeBuilder);
        if (key instanceof NoKey) {
            assert nodeBuilder == null;
            nodeBuilder = new SimpleRedirectNodeBuilder(childNodeBuilder);
        } else {
            assert nodeBuilder == null || ParentNodeBuilder.class.isInstance(nodeBuilder);
            @Nullable ParentNodeBuilder parentNodeBuilder = (ParentNodeBuilder) nodeBuilder;
            if (parentNodeBuilder == null) {
                parentNodeBuilder = new ParentNodeBuilder();
                nodeBuilder = parentNodeBuilder;
            }
            @Nullable NodeBuilder<N> previous = parentNodeBuilder.children.put(key, childNodeBuilder);
            assert previous == null;
        }
    }

    @Override
    public OutputStream newOutputStream(Key key) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        createByteSequenceNodeBuilder(
            key,
            new OutputStreamByteSequenceNodeBuilder<>(treeBuilder.resolve(key), outputStream)
        );
        return outputStream;
    }

    @Override
    public void putByteSequence(ByteSequence byteSequence, Key key) throws IOException {
        createByteSequenceNodeBuilder(
            key,
            new ByteSequenceNodeBuilder<>(treeBuilder.resolve(key), ByteSequences.selfContained(byteSequence))
        );
    }

    @Override
    public ObjectAction putImmutableObject(Object object) throws IOException {
        assert nodeBuilder == null;
        assert marshaler != null : "TreeBuilderMarshalTarget#start should have been called before";
        if (treeBuilder.shouldMarshal(marshaler, object)) {
            return ObjectAction.MARSHAL;
        } else {
            nodeBuilder = new ObjectNodeBuilder(object);
            return MarshalTarget.ObjectAction.NONE;
        }
    }

    @Override
    public MarshalTarget resolve(Key key) throws IOException {
        TreeBuilderMarshalTarget<N> childContext = new TreeBuilderMarshalTarget<>(treeBuilder.resolve(key));
        if (key instanceof NoKey) {
            assert nodeBuilder == null;
            nodeBuilder = new RedirectNodeBuilder(childContext);
        } else {
            ParentNodeBuilder parentNodeBuilder = createParentNodeBuilder();
            parentNodeBuilder.children.put(key, new PolymorphicNodeBuilder<>(childContext));
        }
        return childContext;
    }

    @Override
    public void finish() throws IOException {
        if (nodeBuilder != null) {
            node = nodeBuilder.getNode();
        }
    }

    /**
     * Returns the root node of the tree built by this tree builder.
     *
     * <p>A tree is typically built by passing this tree builder to
     * {@link DelegatingMarshalContext#marshal(Object, Collection, MarshalTarget)}.
     *
     * @return the root node of the tree
     * @throws IllegalStateException if no tree has been built yet
     */
    public N getTree() {
        @Nullable N localNode = node;
        if (localNode == null) {
            throw new IllegalStateException("getTree() called before tree was successfully built.");
        }
        return localNode;
    }

    @FunctionalInterface
    private interface NodeBuilder<N> {
        N getNode() throws IOException;
    }

    private final class ParentNodeBuilder implements NodeBuilder<N> {
        private final Map<Key, NodeBuilder<N>> children = new LinkedHashMap<>();

        @Override
        public N getNode() throws IOException {
            Map<Key, N> childNodes = new LinkedHashMap<>(children.size());
            for (Map.Entry<Key, NodeBuilder<N>> entry: children.entrySet()) {
                childNodes.put(entry.getKey(), entry.getValue().getNode());
            }
            assert marshaler != null : "TreeBuilderMarshalTarget#start should have been called before";
            return treeBuilder.createParentNode(marshaler, childNodes);
        }
    }

    private final class RedirectNodeBuilder implements NodeBuilder<N> {
        private final TreeBuilderMarshalTarget<N> childContext;

        private RedirectNodeBuilder(TreeBuilderMarshalTarget<N> childContext) {
            this.childContext = childContext;
        }

        @Override
        public N getNode() {
            assert marshaler != null : "TreeBuilderMarshalTarget#start should have been called before";
            assert childContext.node != null
                : "TreeBuilderMarshalTarget#finish should have been called for all child contexts";
            return treeBuilder.createRedirectNode(marshaler, childContext.getTree());
        }
    }

    private final class SimpleRedirectNodeBuilder implements NodeBuilder<N> {
        private final NodeBuilder<N> child;

        private SimpleRedirectNodeBuilder(NodeBuilder<N> child) {
            this.child = child;
        }

        @Override
        public N getNode() throws IOException {
            assert marshaler != null : "TreeBuilderMarshalTarget#start should have been called before";
            return treeBuilder.createRedirectNode(marshaler, child.getNode());
        }
    }

    private static final class PolymorphicNodeBuilder<N> implements NodeBuilder<N> {
        private final TreeBuilderMarshalTarget<N> childContext;

        private PolymorphicNodeBuilder(TreeBuilderMarshalTarget<N> childContext) {
            this.childContext = childContext;
        }

        @Override
        public N getNode() {
            assert childContext.node != null
                : "TreeBuilderMarshalTarget#finish should have been called for all child contexts";
            return childContext.getTree();
        }
    }

    private static final class OutputStreamByteSequenceNodeBuilder<N> implements NodeBuilder<N> {
        private final TreeBuilder<N> treeBuilder;
        private final ByteArrayOutputStream outputStream;

        private OutputStreamByteSequenceNodeBuilder(TreeBuilder<N> treeBuilder,
                ByteArrayOutputStream outputStream) {
            this.treeBuilder = treeBuilder;
            this.outputStream = outputStream;
        }

        @Override
        public N getNode() throws IOException {
            return treeBuilder.createByteSequenceNode(ByteSequences.arrayBacked(outputStream.toByteArray()));
        }
    }

    private static final class ByteSequenceNodeBuilder<N> implements NodeBuilder<N> {
        private final TreeBuilder<N> treeBuilder;
        private final ByteSequence byteSequence;

        private ByteSequenceNodeBuilder(TreeBuilder<N> treeBuilder, ByteSequence byteSequence) {
            this.treeBuilder = treeBuilder;
            this.byteSequence = byteSequence;
        }

        @Override
        public N getNode() throws IOException {
            return treeBuilder.createByteSequenceNode(byteSequence);
        }
    }

    private final class ObjectNodeBuilder implements NodeBuilder<N> {
        private final Object object;

        private ObjectNodeBuilder(Object object) {
            this.object = object;
        }

        @Override
        public N getNode() {
            assert marshaler != null : "TreeBuilderMarshalTarget#start should have been called before";
            return treeBuilder.createObjectNode(marshaler, object);
        }
    }
}
