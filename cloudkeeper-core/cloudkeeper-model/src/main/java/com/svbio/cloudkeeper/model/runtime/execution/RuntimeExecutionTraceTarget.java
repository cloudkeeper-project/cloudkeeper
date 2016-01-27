package com.svbio.cloudkeeper.model.runtime.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTraceTarget;

import javax.annotation.Nonnull;

public interface RuntimeExecutionTraceTarget extends RuntimeOverrideTarget, BareExecutionTraceTarget {
    @Override
    @Nonnull
    RuntimeExecutionTrace getExecutionTrace();
}
