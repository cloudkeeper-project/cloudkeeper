package com.svbio.cloudkeeper.model.bare.execution;

import com.svbio.cloudkeeper.model.bare.BareLocatable;

import javax.annotation.Nullable;

/**
 * Reference to one or more execution traces or elements.
 */
public interface BareOverrideTarget extends BareLocatable {
    /**
     * Calls the visitor method that is appropriate for the actual type of this override target.
     *
     * @param visitor override-target visitor
     * @param <T> return type of visitor
     * @param <P> parameter type
     * @return result returned by visitor
     */
    @Nullable
    <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter);
}
