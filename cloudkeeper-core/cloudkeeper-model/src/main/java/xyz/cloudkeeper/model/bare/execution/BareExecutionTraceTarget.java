package xyz.cloudkeeper.model.bare.execution;

import javax.annotation.Nullable;

public interface BareExecutionTraceTarget extends BareOverrideTarget {
    /**
     * Returns the execution trace.
     */
    @Nullable
    BareExecutionTrace getExecutionTrace();
}
