package com.svbio.cloudkeeper.model.api;

import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.util.concurrent.CompletableFuture;

/**
 * CloudKeeper workflow execution.
 *
 * <p>This interface provides methods for determining the status of a workflow execution, cancelling the execution, and
 * registering callbacks for execution events.
 */
public interface WorkflowExecution {
    /**
     * Returns the time of when the execution was started in milliseconds.
     *
     * <p>The start time is defined as the time when {@link WorkflowExecutionBuilder#start()} is called. The value
     * returned by this method is to be interpreted in the same way as {@link System#currentTimeMillis()}.
     *
     * @return the difference, measured in milliseconds, between the start time and midnight, January 1, 1970 UTC.
     */
    long getStartTimeMillis();

    /**
     * Attempts to cancel execution of this workflow.
     *
     * <p>This attempt will fail if the task has already completed, has already been cancelled, or could not be
     * cancelled for some other reason.
     *
     * After this method returns, subsequent calls to {@link #isRunning()} will always return {@code false}.
     *
     * @return {@code false} if the execution could not be cancelled, typically because it has already completed
     *     normally; {@code true}  otherwise
     */
    boolean cancel();

    /**
     * Returns a new future that will normally be completed with the trace corresponding to the root module.
     *
     * @return the new future
     */
    CompletableFuture<RuntimeAnnotatedExecutionTrace> getTrace();

    /**
     * Returns a new future that will normally be completed with the execution ID.
     *
     * @return the new future
     */
    CompletableFuture<Long> getExecutionId();

    /**
     * Returns whether the execution is still running.
     *
     * <p>Even while the workflow execution is still running, individual outputs may already be available through
     * {@link #getOutput(String)}.
     *
     * @return {@code true} if the execution is still running, {@code false} otherwise
     */
    boolean isRunning();

    /**
     * Returns a new future that will normally be completed with the output value for the given out-port.
     *
     * <p>This method returns immediately and will not throw any exceptions (unless the arguments is null). If the given
     * {@code outPortName} is invalid, the returned future will be completed exceptionally with an
     * {@link IllegalArgumentException}.
     *
     * @param outPortName name of the out-port
     * @return the new future
     */
    CompletableFuture<Object> getOutput(String outPortName);

    /**
     * Returns a new future that will normally be completed with the difference, measured in milliseconds, between the
     * finish time and midnight, January 1, 1970 UTC.
     *
     * <p>The value is to be interpreted in the same way as {@link System#currentTimeMillis()}. This future will be
     * completed normally even if an expected exception occurs during the workflow execution. Use
     * {@link #toCompletableFuture()} if interested in such exceptions (or to determine whether the workflow execution
     * was successful).
     *
     * @return the new future
     */
    CompletableFuture<Long> getFinishTimeMillis();

    /**
     * Returns a new future that represents this workflow execution and that will normally be completed with
     * {@code null}.
     *
     * <p>At the time the returned futures is completed, it is guaranteed that all futures returned by other methods in
     * this interface have been completed, too.
     *
     * @return the new future
     */
    CompletableFuture<Void> toCompletableFuture();
}
