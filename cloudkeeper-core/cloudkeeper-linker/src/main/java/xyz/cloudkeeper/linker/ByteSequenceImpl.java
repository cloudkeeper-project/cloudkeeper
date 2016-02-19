package xyz.cloudkeeper.linker;

import cloudkeeper.types.ByteSequence;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeByteSequence;
import xyz.cloudkeeper.model.util.ByteSequences;

import java.util.Arrays;
import java.util.Collection;

final class ByteSequenceImpl extends SerializationNodeImpl implements RuntimeByteSequence {
    private final byte[] array;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    ByteSequenceImpl(Key key, byte[] bytes) {
        super(key);
        array = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    ByteSequenceImpl(BareByteSequence original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        byte[] originalArray
            = Preconditions.requireNonNull(original.getArray(), getCopyContext().newContextForProperty("array"));
        array = Arrays.copyOf(originalArray, originalArray.length);
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    public byte[] getArray() {
        return array.clone();
    }

    @Override
    public ByteSequence toByteSequence() {
        return ByteSequences.arrayBacked(array);
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void finishFreezable(FinishContext context) { }
}
