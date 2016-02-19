package xyz.cloudkeeper.model.runtime.execution;

import xyz.cloudkeeper.model.bare.execution.BareExecutionTraceTarget;

import javax.annotation.Nonnull;

public interface RuntimeExecutionTraceTarget extends RuntimeOverrideTarget, BareExecutionTraceTarget {
    @Override
    @Nonnull
    RuntimeExecutionTrace getExecutionTrace();
}
