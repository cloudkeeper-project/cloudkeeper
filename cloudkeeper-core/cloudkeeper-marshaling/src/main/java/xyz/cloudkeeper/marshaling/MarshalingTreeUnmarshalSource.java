package xyz.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ByteSequenceNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.MarshalingException;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.element.NoKey;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * This class exposes a marshaling tree as unmarshal source.
 *
 * <p>Instance of this class are shallow immutable.
 */
public abstract class MarshalingTreeUnmarshalSource implements UnmarshalSource {
    private MarshalingTreeUnmarshalSource() { }

    /**
     * Creates a new unmarshal source from the given marshaling tree.
     *
     * @param tree marshaling tree
     * @return the new unmarshal source
     */
    public static MarshalingTreeUnmarshalSource create(ObjectNode tree) {
        Objects.requireNonNull(tree);
        @Nullable MarshalingTreeUnmarshalSource unmarshalSource = tree.accept(CreateInstanceVisitor.INSTANCE, null);
        assert unmarshalSource != null;
        return unmarshalSource;
    }

    /**
     * Unmarshals an object from the given source.
     *
     * <p>This method is equivalent to creating an unmarshal source using {@link #create(ObjectNode)} and then calling
     * {@link DelegatingUnmarshalContext#unmarshal(UnmarshalSource, ClassLoader)}.
     *
     * @param tree marshaling tree
     * @param classLoader class loader to be returned by
     *     {@link xyz.cloudkeeper.model.api.UnmarshalContext#getClassLoader()}
     * @return the object
     * @throws MarshalingException if a deserialization error occurs
     * @throws IOException if an I/O error occurs
     */
    public static Object unmarshal(ObjectNode tree, ClassLoader classLoader) throws IOException {
        Objects.requireNonNull(tree);
        Objects.requireNonNull(classLoader);
        UnmarshalSource unmarshalSource = create(tree);
        return DelegatingUnmarshalContext.unmarshal(unmarshalSource, classLoader);
    }

    @Nullable
    @Override
    public Object getObject() {
        return null;
    }

    @Nullable
    abstract MarshalingTreeNode getChild(Key key);

    @Nullable
    @Override
    public final ByteSequence getByteSequence(Key key) throws MarshalingException {
        @Nullable MarshalingTreeNode child = getChild(key);
        if (child instanceof ByteSequenceNode) {
            return ((ByteSequenceNode) child).getByteSequence();
        } else if (child != null) {
            throw new MarshalingException(String.format(
                "Expected byte-sequence node for key '%s', but got object node.", key
            ));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public final UnmarshalSource resolve(Key key) throws MarshalingException {
        @Nullable MarshalingTreeNode child = getChild(key);
        if (child instanceof ObjectNode) {
            @Nullable UnmarshalSource childUnmarshalSource = child.accept(CreateInstanceVisitor.INSTANCE, null);
            assert childUnmarshalSource != null;
            return childUnmarshalSource;
        } else if (child != null) {
            throw new MarshalingException(String.format(
                "Expected object node for key '%s', but got byte-sequence node.", key
            ));
        } else {
            return null;
        }
    }

    private static final class ObjectNodeUnmarshalSource extends MarshalingTreeUnmarshalSource {
        private final RawObjectNode node;

        private ObjectNodeUnmarshalSource(RawObjectNode node) {
            this.node = node;
        }

        private static UnsupportedOperationException fail() {
            return new UnsupportedOperationException(
                "Impossible operation for unmarshal source that represents raw-object node."
            );
        }

        @Override
        public Marshaler<?> getMarshaler() {
            throw fail();
        }

        @Nullable
        @Override
        public Object getObject() {
            return node.getObject();
        }

        @Override
        MarshalingTreeNode getChild(Key key) {
            throw fail();
        }
    }

    private static final class MarshaledObjectUnmarshalSource extends MarshalingTreeUnmarshalSource {
        private final MarshaledObjectNode node;

        private MarshaledObjectUnmarshalSource(MarshaledObjectNode node) {
            this.node = node;
        }

        @Override
        public Marshaler<?> getMarshaler() {
            return node.getMarshaler();
        }

        @Nullable
        @Override
        MarshalingTreeNode getChild(Key key) {
            return node.getChildren().get(key);
        }
    }

    private static final class MarshaledReplacementObjectUnmarshalSource extends MarshalingTreeUnmarshalSource {
        private final MarshaledReplacementObjectNode node;

        private MarshaledReplacementObjectUnmarshalSource(MarshaledReplacementObjectNode node) {
            this.node = node;
        }

        @Override
        public Marshaler<?> getMarshaler() {
            return node.getMarshaler();
        }

        @Nullable
        @Override
        MarshalingTreeNode getChild(Key key) {
            return key instanceof NoKey
                ? node.getChild()
                : null;
        }
    }

    private enum CreateInstanceVisitor implements MarshalingTreeNodeVisitor<MarshalingTreeUnmarshalSource, Void> {
        INSTANCE;

        @Override
        public ObjectNodeUnmarshalSource visitRawObjectNode(RawObjectNode node, @Nullable Void ignored) {
            return new ObjectNodeUnmarshalSource(node);
        }

        @Override
        public MarshaledObjectUnmarshalSource visitMarshaledObjectNode(MarshaledObjectNode node,
                @Nullable Void ignored) {
            return new MarshaledObjectUnmarshalSource(node);
        }

        @Nullable
        @Override
        public MarshalingTreeUnmarshalSource visitMarshaledReplacementNode(MarshaledReplacementObjectNode node,
                @Nullable Void ignored) {
            return new MarshaledReplacementObjectUnmarshalSource(node);
        }

        @Nullable
        @Override
        public MarshalingTreeUnmarshalSource visitByteSequenceNode(ByteSequenceNode node, @Nullable Void ignored) {
            // Cannot happen.
            throw new IllegalStateException("Cannot create UnmarshalSource for byte-sequence node.");
        }
    }
}
