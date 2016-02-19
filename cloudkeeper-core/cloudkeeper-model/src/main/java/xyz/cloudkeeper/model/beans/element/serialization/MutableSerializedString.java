package xyz.cloudkeeper.model.beans.element.serialization;

import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializedString;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import java.util.Objects;

public final class MutableSerializedString
        extends MutableSerializationNode<MutableSerializedString>
        implements BareSerializedString {
    private static final long serialVersionUID = 4252434629264387377L;

    @Nullable private String string;

    public MutableSerializedString() { }

    private MutableSerializedString(BareSerializedString original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        string = original.getString();
    }

    @Nullable
    public static MutableSerializedString copyOfSerializedString(@Nullable BareSerializedString original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableSerializedString(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return Objects.equals(string, ((MutableSerializedString) otherObject).string);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(string);
    }

    @Override
    protected MutableSerializedString self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitText(this, parameter);
    }

    @Override
    @Nullable
    public String getString() {
        return string;
    }

    public MutableSerializedString setString(@Nullable String string) {
        this.string = string;
        return this;
    }
}
