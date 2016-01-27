package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializedString;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializedString;

import java.util.Collection;

final class SerializedStringImpl extends SerializationNodeImpl implements RuntimeSerializedString {
    private final String string;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    SerializedStringImpl(Key key, String string) {
        super(key);
        this.string = string;
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    SerializedStringImpl(BareSerializedString original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        string = Preconditions.requireNonNull(original.getString(), getCopyContext().newContextForProperty("string"));
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    public String getString() {
        return string;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void finishFreezable(FinishContext context) { }
}
