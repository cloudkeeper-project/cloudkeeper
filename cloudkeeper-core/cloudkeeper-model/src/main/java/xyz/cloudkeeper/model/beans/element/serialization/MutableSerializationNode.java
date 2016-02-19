package xyz.cloudkeeper.model.beans.element.serialization;

import xyz.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNode;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializedString;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.MutableLocatable;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.element.NoKey;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Objects;

@XmlSeeAlso({ MutableSerializationRoot.class, MutableSerializedString.class, MutableByteSequence.class })
@XmlJavaTypeAdapter(JAXBAdapters.SerializationNodeAdapter.class)
public abstract class MutableSerializationNode<D extends MutableSerializationNode<D>>
        extends MutableLocatable<D>
        implements BareSerializationNode {
    private static final long serialVersionUID = 1557887864514833134L;

    private Key key;

    MutableSerializationNode() {
        key = NoKey.instance();
    }

    MutableSerializationNode(BareSerializationNode original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        key = Objects.requireNonNull(original.getKey());
    }

    private enum CopyVisitor implements BareSerializationNodeVisitor<MutableSerializationNode<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableSerializationRoot visitRoot(BareSerializationRoot original, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableSerializationRoot.copyOfSerializationRoot(original, copyOptions);
        }

        @Override
        public MutableByteSequence visitByteSequence(BareByteSequence original, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableByteSequence.copyOfByteSequence(original, copyOptions);
        }

        @Override
        public MutableSerializedString visitText(BareSerializedString original, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableSerializedString.copyOfSerializedString(original, copyOptions);
        }
    }

    @Nullable
    public static MutableSerializationNode<?> copyOfSerializationNode(@Nullable BareSerializationNode original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : original.accept(CopyVisitor.INSTANCE, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        return super.equals(otherObject) && key.equals(((MutableSerializationNode<?>) otherObject).key);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + key.hashCode();
    }

    @Override
    public final Key getKey() {
        return key;
    }

    public final D setKey(Key key) {
        Objects.requireNonNull(key);
        this.key = key;
        return self();
    }
}
