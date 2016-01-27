package com.svbio.cloudkeeper.model.api;

import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

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
     * Registers the given callback that will be executed when the linker has returned an annotated execution trace
     * (containing the entire runtime representation of the workflow).
     *
     * <p>If the annotated execution trace is already available, the given callback will be executed immediately.
     *
     * @param onHasRootExecutionTrace callback handler
     * @throws NullPointerException if the argument is null
     */
    void whenHasRootExecutionTrace(OnActionComplete<RuntimeAnnotatedExecutionTrace> onHasRootExecutionTrace);

    /**
     * Registers the given callback that will be executed when an execution ID has been assigned.
     *
     * <p>If the execution ID has already been assigned, the given callback will be executed immediately.
     *
     * @param onHasExecutionId callback handler
     * @throws NullPointerException if the argument is null
     */
    void whenHasExecutionId(OnActionComplete<Long> onHasExecutionId);

    /**
     * Returns whether the execution is still running.
     *
     * <p>Even while the workflow execution is still running, individual outputs may already be available through
     * {@link #whenHasOutput(String, OnActionComplete)}.
     *
     * @return whether the execution is still running
     */
    boolean isRunning();

    /**
     * Registers the given callback that will be executed when the output for the given out-port has become available.
     *
     * <p>If the workflow execution has already finished, the given callback will be executed immediately.
     *
     * <p>Note that this method returns immediately and is therefore not expected to throw any exceptions (unless any of
     * the arguments is null). If the given {@code outPortName} is invalid, an appropriate runtime exception will be
     * passed to the completion handler. If the workflow execution fails for any reason, the callback will be passed
     * a {@link WorkflowExecutionException}.
     *
     * @param outPortName name of the out-port
     * @param onHasOutput callback handler
     * @throws NullPointerException if any of the arguments is null
     */
    void whenHasOutput(String outPortName, OnActionComplete<Object> onHasOutput);

    /**
     * Registers the given callback that will be executed when the workflow execution has finished.
     *
     * <p>If the workflow execution has already finished, the given callback will be executed immediately. The value
     * passed to {@link OnActionComplete#complete(Throwable, Object)} in case of success is the the difference, measured
     * in milliseconds, between the finish time and midnight, January 1, 1970 UTC.
     *
     * @param onHasFinishTimeMillis callback handler
     * @throws NullPointerException if the argument is null
     */
    void whenHasFinishTimeMillis(OnActionComplete<Long> onHasFinishTimeMillis);

    /**
     * Registers the given callback that will be executed when the workflow execution has finished.
     *
     * <p>If the workflow execution has already finished, the given callback will be executed immediately. The value
     * passed to {@link OnActionComplete#complete(Throwable, Object)} in case of success is null.
     *
     * @param onExecutionFinished callback handler
     * @throws NullPointerException if the argument is null
     */
    void whenExecutionFinished(OnActionComplete<Void> onExecutionFinished);

    /**
     * Callback for when an action is completed with either failure or success.
     *
     * @param <T> type of the value that the action will be completed with in case of success
     */
    interface OnActionComplete<T> {
        /**
         * This method will be invoked once the action that this callback is registered on becomes completed with a
         * failure or a success.
         *
         * <p>In the case of success {@code exception} will be {@code null}, otherwise it will contain the reason of the
         * failure.
         *
         * @param throwable reason for the failure of the action
         * @param value value that the action was successfully completed with
         */
        void complete(Throwable throwable, T value);
    }
}
