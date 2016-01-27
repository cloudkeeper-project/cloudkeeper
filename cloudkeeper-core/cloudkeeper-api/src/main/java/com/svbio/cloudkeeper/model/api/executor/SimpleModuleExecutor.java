package com.svbio.cloudkeeper.model.api.executor;

import com.svbio.cloudkeeper.model.api.ExecutionException;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import scala.concurrent.Future;

import javax.annotation.Nullable;

/**
 * Executor of simple modules represented by {@link RuntimeStateProvider} instances.
 */
public interface SimpleModuleExecutor {
    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when {@link #submit(RuntimeStateProvider, Future)} was called.
     */
    SimpleName SUBMISSION_TIME_MILLIS = SimpleName.identifier("submissionTimeMillis");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when the future returned by {@link #submit(RuntimeStateProvider, Future)} was
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
     * <p>Support for cancellation is optional. Callers of this method should complete the promise underlying
     * {@code cancellationFuture} whenever a submission is cancelled, but they must not expect more than a best-effort
     * attempt to cancel the execution.
     *
     * @param runtimeStateProvider provider of the runtime state, which consists of the CloudKeeper plug-in
     *     repository, the Java class loader, the call stack, and the staging area
     * @param cancellationFuture Future that will be completed if cancellation of the execution is requested. The
     *     execution should be cancelled both when the future is completed successfully and exceptionally. In case of
     *     successful execution, the {@link String} contains a reason for cancellation. May be null.
     * @return Future representing pending completion of the task. The future will be completed with a
     *     {@link SimpleModuleExecutorResult}. If the module execution fails,
     *     {@link SimpleModuleExecutorResult#getExecutionException() contains an {@link ExecutionException}. Only if an
     *     unexpected error occurs, the future may be completed with any other {@link Throwable}.
     * @throws NullPointerException if the first argument is null
     */
    Future<SimpleModuleExecutorResult> submit(RuntimeStateProvider runtimeStateProvider,
        @Nullable Future<String> cancellationFuture);
}
