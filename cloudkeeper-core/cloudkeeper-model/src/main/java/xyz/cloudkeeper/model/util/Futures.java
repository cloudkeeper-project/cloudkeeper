package xyz.cloudkeeper.model.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * This class consists exclusively of static methods that operate on futures.
 */
public final class Futures {
    private Futures() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns a new future that will be completed normally with the list of results of all given completion stages.
     *
     * <p>If any of the given completion stages completes exceptionally, then the returned future is completed
     * exceptionally with the first encountered exception, without necessarily awaiting the results of all other
     * completion stages. That is, the returned future may be completed even though some of the given completion stages
     * may not yet be completed.
     *
     * <p>The order of the given completion stages is preserved in the returned list.
     *
     * @param completionStages completion stages supplying the elements of the list that the returned future will be
     *     completed with
     * @param executor the executor to use for asynchronous execution
     * @param <T> the element type of the list that the new future will be completed with
     * @return the new future
     */
    public static <T> CompletableFuture<List<T>> shortCircuitCollect(
            Iterable<? extends CompletionStage<? extends T>> completionStages, Executor executor) {
        Objects.requireNonNull(completionStages);
        Objects.requireNonNull(executor);

        CompletableFuture<List<T>> listFuture = CompletableFuture.completedFuture((List<T>) new ArrayList<T>());
        for (CompletionStage<? extends T> completionStage: completionStages) {
            listFuture = listFuture.thenComposeAsync(
                list -> completionStage.thenApply(
                    value -> {
                        list.add(value);
                        return list;
                    }
                ),
                executor
            );
        }
        return listFuture;
    }
}
