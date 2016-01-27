package com.svbio.cloudkeeper.model.runtime.execution;

import javax.annotation.Nullable;

/**
 * Visitor of annotation override targets, in the style of the visitor design pattern.
 */
public interface RuntimeOverrideTargetVisitor<T, P> {
    /**
     * Visits a target that is an element reference.
     */
    @Nullable
    T visitElementTarget(RuntimeElementTarget elementReferenceTarget, @Nullable P parameter);

    /**
     * Visits a target that is a regular expression for matching element references.
     */
    @Nullable
    T visitElementPatternTarget(RuntimeElementPatternTarget elementReferencePatternTarget, @Nullable P parameter);

    /**
     * Visits a target that is an execution trace.
     */
    @Nullable
    T visitExecutionTraceTarget(RuntimeExecutionTraceTarget executionTraceTarget, @Nullable P parameter);

    /**
     * Visits a target that is a regular expression for matching execution traces.
     */
    @Nullable
    T visitExecutionTracePatternTarget(RuntimeExecutionTracePatternTarget executionTracePatternTarget,
        @Nullable P parameter);
}
