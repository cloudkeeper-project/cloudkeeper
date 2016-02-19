package xyz.cloudkeeper.model.beans.element.serialization;

import xyz.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import java.util.Arrays;

public final class MutableByteSequence
        extends MutableSerializationNode<MutableByteSequence>
        implements BareByteSequence {
    private static final long serialVersionUID = -895239246709708730L;

    @Nullable private byte[] array;

    public MutableByteSequence() { }

    private MutableByteSequence(BareByteSequence original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        @Nullable byte[] originalArray = original.getArray();
        array = originalArray == null
            ? null
            : Arrays.copyOf(originalArray, originalArray.length);
    }

    @Nullable
    public static MutableByteSequence copyOfByteSequence(@Nullable BareByteSequence original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableByteSequence(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return Arrays.equals(array, ((MutableByteSequence) otherObject).array);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Arrays.hashCode(array);
    }

    @Override
    protected MutableByteSequence self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitByteSequence(this, parameter);
    }

    @Override
    @Nullable
    public byte[] getArray() {
        return array;
    }

    public MutableByteSequence setArray(@Nullable byte[] array) {
        this.array = array;
        return this;
    }
}
