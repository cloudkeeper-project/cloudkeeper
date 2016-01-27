package com.svbio.cloudkeeper.model.runtime.element.serialization;

import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RuntimeSerializationRoot extends RuntimeSerializationNode, BareSerializationRoot {
    @Override
    @Nullable
    default <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitRoot(this, parameter);
    }

    @Nullable
    @Override
    default <T, P> T accept(RuntimeSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitRoot(this, parameter);
    }

    @Override
    @Nonnull
    RuntimeSerializationDeclaration getDeclaration();

    /**
     * {@inheritDoc}
     *
     * <p>It is guaranteed that the returned list is non-empty and all returned nodes have non-null tokens. Moreover, it
     * is guaranteed that if the list contains an entry with pseudo-key
     * {@link com.svbio.cloudkeeper.model.immutable.element.NoKey}, then it does not contains any other entries.
     */
    @Override
    ImmutableList<? extends RuntimeSerializationNode> getEntries();

    /**
     * Returns the serialization node for the given key.
     *
     * @param key key of the serialization node, may be the empty key
     * @return the serialization node, or {@code null} if it does not exist
     */
    @Nullable
    RuntimeSerializationNode getEntry(Key key);
}
