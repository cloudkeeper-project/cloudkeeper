package xyz.cloudkeeper.model.runtime.element.serialization;

import javax.annotation.Nullable;

/**
 * Visitor of persistence-tree nodes, in the style of the visitor design pattern.
 */
public interface RuntimeSerializationNodeVisitor<T, P> {
    /**
     * Visits a root node.
     */
    @Nullable
    T visitRoot(RuntimeSerializationRoot root, @Nullable P parameter);

    /**
     * Visits a byte-sequence node.
     */
    @Nullable
    T visitByteSequence(RuntimeByteSequence byteSequence, @Nullable P parameter);

    /**
     * Visits a string node.
     */
    @Nullable
    T visitString(RuntimeSerializedString serializedString, @Nullable P parameter);
}
