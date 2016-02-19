package xyz.cloudkeeper.model.runtime.element.serialization;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;

import javax.annotation.Nullable;

public interface RuntimeByteSequence extends RuntimeSerializationNode, BareByteSequence {
    @Override
    @Nullable
    default <T, P> T accept(RuntimeSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitByteSequence(this, parameter);
    }

    @Override
    @Nullable
    default <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitByteSequence(this, parameter);
    }

    /**
     * Returns a {@link ByteSequence} representing this persistence-tree node.
     *
     * <p>This method is more efficient than calling {@link #getArray()} because it does not involve any copying.
     *
     * @return {@link ByteSequence} representing this persistence-tree node
     */
    ByteSequence toByteSequence();
}
