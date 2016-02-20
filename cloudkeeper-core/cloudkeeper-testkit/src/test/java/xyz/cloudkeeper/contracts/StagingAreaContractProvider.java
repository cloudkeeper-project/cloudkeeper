package xyz.cloudkeeper.contracts;

import org.testng.Assert;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.util.concurrent.CompletableFuture;

/**
 * Provider for {@link StagingArea} instances in contract tests.
 */
@FunctionalInterface
public interface StagingAreaContractProvider {
    /**
     * Perform pre-contract actions.
     *
     * <p>This method is called from within an {@link org.testng.annotations.BeforeClass} annotated method. It is
     * therefore possible to throw a {@link org.testng.SkipException} in this method.
     */
    default void preContract() { }

    /**
     * Returns a fresh staging area for the given absolute execution trace.
     *
     * <p>Implementations are expected to clean up system resources in a {@link org.testng.annotations.AfterSuite}
     * annotated method of the test.
     *
     * @param identifier Identifier that may be used by the provider the help debugging. The identifier is orthogonal
     *     to the CloudKeeper domain model and API.
     * @param runtimeContext runtime context including the repository and the Java class loader
     * @param executionTrace call stack
     * @return the staging area
     */
    StagingArea getStagingArea(String identifier, RuntimeContext runtimeContext,
        RuntimeAnnotatedExecutionTrace executionTrace);

    /**
     * Awaits the result of the given {@link CompletableFuture}.
     *
     * @param future {@link CompletableFuture} returned by a {@link StagingArea} instance previously returned by
     *     {@link #getStagingArea(String, RuntimeContext, RuntimeAnnotatedExecutionTrace)}
     * @param <T> type of the future
     * @return result of the given future
     */
    default <T> T await(CompletableFuture<T> future) throws Exception {
        Assert.assertTrue(future.isDone(), "Future has not yet been completed.");
        return future.get();
    }
}
