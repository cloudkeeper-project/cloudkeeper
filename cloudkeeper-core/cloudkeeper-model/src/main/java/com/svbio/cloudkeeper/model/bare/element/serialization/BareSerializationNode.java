package com.svbio.cloudkeeper.model.bare.element.serialization;

import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.immutable.element.Key;

import javax.annotation.Nullable;

/**
 * Node in a persistence tree.
 *
 * <p>A <em>persistence tree</em> is a tree representation of a value. Unlike arbitrary user-defined Java objects that
 * are normally used to represent values, a persistence tree provides a serialized representation of values (that
 * consists of potentially multiple byte sequences).
 */
public interface BareSerializationNode extends BareLocatable {
    /**
     * Returns the key of this node.
     *
     * <p>The token of a serialization node may be a simple name, an index, or the empty key.
     *
     * @return key of this node, guaranteed not null
     */
    Key getKey();

    /**
     * Calls the visitor method that is appropriate for the actual type of this module.
     *
     * @param visitor module visitor
     * @param <T> return type of visitor
     * @param <P> parameter type
     * @return result returned by visitor
     */
    @Nullable
    <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter);
}
