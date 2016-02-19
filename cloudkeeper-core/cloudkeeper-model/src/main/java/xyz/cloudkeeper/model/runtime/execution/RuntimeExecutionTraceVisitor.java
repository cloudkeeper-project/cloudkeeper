package xyz.cloudkeeper.model.runtime.execution;

import javax.annotation.Nullable;

/**
 * Visitor of execution traces, in the style of the visitor design pattern.
 */
public interface RuntimeExecutionTraceVisitor<T, P> {
    /**
     * Visits an execution trace that represents a module.
     *
     * @see RuntimeExecutionTrace.Type#MODULE
     */
    @Nullable
    T visitModule(RuntimeExecutionTrace executionTrace, @Nullable P parameter);

    /**
     * Visits an execution trace that last element is a content element.
     *
     * @see RuntimeExecutionTrace.Type#CONTENT
     */
    @Nullable
    T visitContent(RuntimeExecutionTrace executionTrace, @Nullable P parameter);

    /**
     * Visits an execution trace that represents an iteration.
     *
     * @see RuntimeExecutionTrace.Type#ITERATION
     */
    @Nullable
    T visitIteration(RuntimeExecutionTrace executionTrace, @Nullable P parameter);

    /**
     * Visits an execution trace that represents an in-port.
     *
     * @see RuntimeExecutionTrace.Type#IN_PORT
     */
    @Nullable
    T visitInPort(RuntimeExecutionTrace executionTrace, @Nullable P parameter);

    /**
     * Visits an execution trace that represents an out-port.
     *
     * @see RuntimeExecutionTrace.Type#OUT_PORT
     */
    @Nullable
    T visitOutPort(RuntimeExecutionTrace executionTrace, @Nullable P parameter);

    /**
     * Visits an execution trace that represents an array index.
     *
     * @see RuntimeExecutionTrace.Type#ARRAY_INDEX
     */
    @Nullable
    T visitArrayIndex(RuntimeExecutionTrace executionTrace, @Nullable P parameter);
}
