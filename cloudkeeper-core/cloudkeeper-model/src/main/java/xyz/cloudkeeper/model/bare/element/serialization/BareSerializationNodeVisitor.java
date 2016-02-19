package xyz.cloudkeeper.model.bare.element.serialization;

import javax.annotation.Nullable;

/**
 * Visitor of serialization nodes, in the style of the visitor design pattern.
 */
public interface BareSerializationNodeVisitor<T, P> {
    /**
     * Visits a root node.
     */
    @Nullable
    T visitRoot(BareSerializationRoot root, @Nullable P parameter);

    /**
     * Visits a byte-sequence node.
     */
    @Nullable
    T visitByteSequence(BareByteSequence byteSequence, @Nullable P parameter);

    /**
     * Visits a string node.
     */
    @Nullable
    T visitText(BareSerializedString text, @Nullable P parameter);
}
