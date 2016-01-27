package com.svbio.cloudkeeper.model.runtime.execution;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.execution.BareOverrideTarget;

import javax.annotation.Nullable;

public interface RuntimeOverrideTarget extends BareOverrideTarget, Immutable {
    /**
     * Calls the visitor method that is appropriate for the actual type of this override target.
     *
     * @param visitor override-target visitor
     * @param <T> return type of visitor
     * @param <P> parameter type
     * @return result returned by visitor
     */
    @Nullable
    <T, P> T accept(RuntimeOverrideTargetVisitor<T, P> visitor, @Nullable P parameter);
}
