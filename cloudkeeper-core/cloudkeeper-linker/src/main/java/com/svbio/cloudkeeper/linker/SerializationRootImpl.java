package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class SerializationRootImpl extends SerializationNodeImpl implements RuntimeSerializationRoot {
    @Nullable private final NameReference serializationReference;
    private final Map<Key, SerializationNodeImpl> entriesMap;
    @Nullable private volatile RuntimeSerializationDeclaration serializationDeclaration;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    SerializationRootImpl(Key key, SerializationDeclarationImpl serializationDeclaration,
            Map<Key, SerializationNodeImpl> entries) {
        super(key);
        Objects.requireNonNull(key);
        Objects.requireNonNull(serializationDeclaration);
        Objects.requireNonNull(entries);
        serializationReference = null;
        this.serializationDeclaration = serializationDeclaration;
        entriesMap = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    SerializationRootImpl(BareSerializationRoot original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        serializationReference
            = new NameReference(original.getDeclaration(), context.newContextForProperty("declaration"));

        entriesMap = unmodifiableMapOf(original.getEntries(), "entries", SerializationNodeImpl::copyOf,
            SerializationNodeImpl::getKey);
        Preconditions.requireCondition(!entriesMap.containsKey(NoKey.instance()) || entriesMap.size() == 1,
            context.newContextForListProperty("entries"), "Cannot contain both empty key and non-empty keys.");
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    public ImmutableList<SerializationNodeImpl> getEntries() {
        return ImmutableList.copyOf(entriesMap.values());
    }

    @Override
    @Nullable
    public SerializationNodeImpl getEntry(Key key) {
        return entriesMap.get(key);
    }

    @Override
    public RuntimeSerializationDeclaration getDeclaration() {
        require(State.FINISHED);
        @Nullable RuntimeSerializationDeclaration localSerializationDeclaration = serializationDeclaration;
        assert localSerializationDeclaration != null : "must be non-null when finished";
        return localSerializationDeclaration;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.addAll(entriesMap.values());
    }

    @Override
    final void finishFreezable(FinishContext context) throws LinkerException {
        assert serializationReference != null : "must be non-null if created unfinished";
        serializationDeclaration = context.getDeclaration(
            BareSerializationDeclaration.NAME, SerializationDeclarationImpl.class, serializationReference);
    }
}
