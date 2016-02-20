package xyz.cloudkeeper.model.api.executor;

import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import java.util.concurrent.CompletableFuture;

/**
 * Executor of simple modules represented by {@link RuntimeStateProvider} instances.
 */
public interface SimpleModuleExecutor {
    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when {@link #submit(RuntimeStateProvider)} was called.
     */
    SimpleName SUBMISSION_TIME_MILLIS = SimpleName.identifier("submissionTimeMillis");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when the future returned by {@link #submit(RuntimeStateProvider)} was
     * completed.
     */
    SimpleName COMPLETION_TIME_MILLIS = SimpleName.identifier("completionTimeMillis");

    /**
     * Submits the current runtime state for execution and returns a future representing that execution.
     *
     * <p>This interface leaves implementations a great degree of freedom, and no assumptions beyond the explicit
     * guarantees should be made. For instance, a perfectly valid implementation could simply forward the execution
     * request to another JVM.
     *
     * <p>Support for cancellation is optional. That is, calling {@link CompletableFuture#cancel(boolean)} on the
     * returned future is not guaranteed to perform more than a best-effort attempt to cancel the execution.
     *
     * @param runtimeStateProvider provider of the runtime state, which consists of the CloudKeeper plug-in
     *     repository, the Java class loader, the call stack, and the staging area
     * @return Future representing pending completion of the task. The future will be completed with a
     *     {@link SimpleModuleExecutorResult}. If the module execution fails,
     *     {@link SimpleModuleExecutorResult#getExecutionException()} contains a
     *     {@link xyz.cloudkeeper.model.api.ExecutionException}. Only if an
     *     unexpected error occurs, the future may be completed with any other {@link Throwable}.
     */
    CompletableFuture<SimpleModuleExecutorResult> submit(RuntimeStateProvider runtimeStateProvider);
}
