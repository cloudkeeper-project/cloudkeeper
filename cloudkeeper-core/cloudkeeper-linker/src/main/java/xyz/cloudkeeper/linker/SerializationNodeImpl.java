package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNode;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializedString;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationNode;

import javax.annotation.Nullable;
import java.util.Objects;

abstract class SerializationNodeImpl extends LocatableImpl implements RuntimeSerializationNode {
    private final Key key;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    SerializationNodeImpl(Key key) {
        super(State.PRECOMPUTED, null);
        this.key = Objects.requireNonNull(key);
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    SerializationNodeImpl(BareSerializationNode original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        key = original.getKey();
    }

    private enum CopyVisitor implements
            BareSerializationNodeVisitor<Try<? extends SerializationNodeImpl>, CopyContext> {
        INSTANCE;

        @Override
        public Try<SerializationRootImpl> visitRoot(BareSerializationRoot original,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new SerializationRootImpl(original, parentContext));
        }

        @Override
        public Try<ByteSequenceImpl> visitByteSequence(BareByteSequence original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ByteSequenceImpl(original, parentContext));
        }

        @Override
        public Try<SerializedStringImpl> visitText(BareSerializedString original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new SerializedStringImpl(original, parentContext));
        }
    }

    static SerializationNodeImpl copyOf(BareSerializationNode original, CopyContext parentContext)
            throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        @Nullable Try<? extends SerializationNodeImpl> copyTry = original.accept(CopyVisitor.INSTANCE, parentContext);
        assert copyTry != null;
        return copyTry.get();
    }

    @Override
    public final Key getKey() {
        return key;
    }

    @Override
    final void preProcessFreezable(FinishContext context) { }

    @Override
    final void verifyFreezable(VerifyContext context) { }
}
