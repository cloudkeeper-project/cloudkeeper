package xyz.cloudkeeper.model.runtime.element.serialization;

import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializedString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RuntimeSerializedString extends RuntimeSerializationNode, BareSerializedString {
    @Override
    @Nullable
    default <T, P> T accept(RuntimeSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitString(this, parameter);
    }

    @Override
    @Nullable
    default <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitText(this, parameter);
    }

    @Override
    @Nonnull
    String getString();
}
