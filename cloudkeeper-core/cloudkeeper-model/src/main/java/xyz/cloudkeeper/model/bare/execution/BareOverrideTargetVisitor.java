package xyz.cloudkeeper.model.bare.execution;

import javax.annotation.Nullable;

/**
 * Visitor of annotation override targets, in the style of the visitor design pattern.
 */
public interface BareOverrideTargetVisitor<T, P> {
    /**
     * Visits a target that is an element reference.
     */
    @Nullable
    T visitElementTarget(BareElementTarget elementReferenceTarget, @Nullable P parameter);

    /**
     * Visits a target that is a regular expression for matching element references.
     */
    @Nullable
    T visitElementPatternTarget(BareElementPatternTarget elementReferencePatternTarget, @Nullable P parameter);

    /**
     * Visits a target that is an execution trace.
     */
    @Nullable
    T visitExecutionTraceTarget(BareExecutionTraceTarget executionTraceTarget, @Nullable P parameter);

    /**
     * Visits a target that is a regular expression for matching execution traces.
     */
    @Nullable
    T visitExecutionTracePatternTarget(BareExecutionTracePatternTarget executionTracePatternTarget,
        @Nullable P parameter);
}
