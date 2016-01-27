package com.svbio.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.immutable.element.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Marshaling-tree node (see {@link com.svbio.cloudkeeper.model.api.MarshalContext} for a definition).
 *
 * <p>Each instance of this interface is also an instance of one and only one of:
 * <ul><li>
 *     {@link ByteSequenceNode}
 * </li><li>
 *     {@link ObjectNode}
 * </li></ul>
 *
 * <p>Since nodes may contain byte sequences and since the {@link ByteSequence} interface does not not define a
 * particular contract for {@link Object#equals(Object)} or {@link Object#hashCode()}, this interface does not define
 * one, either.
 */
public interface MarshalingTreeNode {
    /**
     * Applies a visitor to this marshaling-tree node.
     *
     * @param visitor the visitor operating on this node
     * @param parameter additional parameter to the visitor
     * @param <T> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @return a visitor-specified result
     */
    @Nullable
    <T, P> T accept(MarshalingTreeNodeVisitor<T, P> visitor, @Nullable P parameter);

    /**
     * Byte-sequence node in the marshaling tree.
     */
    interface ByteSequenceNode extends MarshalingTreeNode {
        /**
         * Returns a byte-sequence node that is shallow immutable.
         *
         * <p>The returned instance implements {@link Object#equals(Object)} and {@link Object#hashCode()} in the
         * canonical way, after converting the byte sequence into a {@code byte} array using
         * {@link ByteSequence#toByteArray()}. If an {@link java.io.IOException} occurs, it will be wrapped in a
         * {@link java.io.UncheckedIOException}.
         *
         * @param byteSequence byte sequence
         * @return new byte-sequence node
         */
        static ByteSequenceNode of(ByteSequence byteSequence) {
            return new MarshalingTreeNodeImpl.ByteSequenceNodeImpl(byteSequence);
        }

        @Nullable
        @Override
        default <T, P> T accept(MarshalingTreeNodeVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitByteSequenceNode(this, parameter);
        }

        /**
         * Returns a read-only byte buffer containg the byte sequence.
         */
        ByteSequence getByteSequence();
    }

    /**
     * Object node in the marshaling tree.
     *
     * <p>Each instance of this interface is also an instance of one and only one of:
     * <ul><li>
     *     {@link MarshaledReplacementObjectNode}
     * </li><li>
     *     {@link MarshaledObjectNode}
     * </li><li>
     *     {@link RawObjectNode}
     * </li></ul>
     */
    interface ObjectNode extends MarshalingTreeNode {
        /**
         * Returns the marshaler that was used, or would have been used, to marshal the object represented by this node.
         *
         * @return the marshaler, or {@code null} if not available
         */
        @Nullable
        Marshaler<?> getMarshaler();
    }

    /**
     * Marshaled-replacement-object node in the marshaling tree.
     */
    interface MarshaledReplacementObjectNode extends ObjectNode {
        /**
         * Returns a mutable-replacement-object node that is shallow immutable.
         *
         * <p>The returned instance implements {@link Object#equals(Object)} and {@link Object#hashCode()} in the
         * canonical way.
         *
         * @param marshaler marshaler for this object node
         * @param child child node (which implicitly has the empty key as label)
         * @return new mutable-replacement-object node
         */
        static MarshaledReplacementObjectNode of(Marshaler<?> marshaler, MarshalingTreeNode child) {
            return new MarshalingTreeNodeImpl.MarshaledAsReplacementObjectNodeImpl(marshaler, child);
        }

        @Nullable
        @Override
        default <T, P> T accept(MarshalingTreeNodeVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitMarshaledReplacementNode(this, parameter);
        }

        /**
         * Returns the marshaler that was used to marshal the object represented by this node.
         *
         * @return the marshaler, or {@code null} if not available
         */
        @Nonnull
        @Override
        Marshaler<?> getMarshaler();

        MarshalingTreeNode getChild();
    }

    /**
     * Marshaled-object node in the marshaling tree.
     */
    interface MarshaledObjectNode extends ObjectNode {
        /**
         * Returns a mutable-object node that is shallow immutable.
         *
         * <p>The returned instance implements {@link Object#equals(Object)} and {@link Object#hashCode()} in the
         * canonical way.
         *
         * @param marshaler marshaler for this object node
         * @param children children nodes (must not have empty keys)
         * @return new marshaled-object node
         */
        static MarshaledObjectNode of(Marshaler<?> marshaler, Map<Key, MarshalingTreeNode> children) {
            return new MarshalingTreeNodeImpl.MarshaledAsObjectNodeImpl(marshaler, children);
        }

        @Nullable
        @Override
        default <T, P> T accept(MarshalingTreeNodeVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitMarshaledObjectNode(this, parameter);
        }

        /**
         * Returns the marshaler that was used to marshal the object represented by this node.
         *
         * @return the marshaler, or {@code null} if not available
         */
        @Nonnull
        @Override
        Marshaler<?> getMarshaler();

        Map<Key, MarshalingTreeNode> getChildren();
    }

    /**
     * Object node in the marshaling tree.
     */
    interface RawObjectNode extends ObjectNode {
        /**
         * Returns a mutable-object node that is shallow immutable.
         *
         * <p>The returned instance implements {@link Object#equals(Object)} and {@link Object#hashCode()} in the
         * canonical way.
         *
         * @param marshaler marshaler for this object node
         * @param object raw object
         * @return new raw-object node
         */
        static RawObjectNode of(Marshaler<?> marshaler, Object object) {
            return new MarshalingTreeNodeImpl.RawObjectNodeImpl(marshaler, object);
        }

        @Nullable
        @Override
        default <T, P> T accept(MarshalingTreeNodeVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitRawObjectNode(this, parameter);
        }

        Object getObject();
    }
}
