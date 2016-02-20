package xyz.cloudkeeper.interpreter;

import akka.dispatch.OnComplete;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

final class ScalaFutures {
    private ScalaFutures() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns a new {@link CompletableFuture} equivalent to the given Scala {@link Future}.
     *
     * <p>Note that since Scala {@link Future} instances are unmodifiable, calling
     * {@link CompletableFuture#cancel(boolean)} will have no effect on the original {@link Future} instance.
     *
     * @param scalaFuture the Scala {@link Future}
     * @param executionContext execution context from which the returned {@link CompletableFuture} will be completed
     * @param <T> type of the object that the futures will be completed with in case of success
     * @return the new future
     */
    static <T> CompletableFuture<T> completableFutureOf(
            Future<T> scalaFuture, ExecutionContext executionContext) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        scalaFuture.onComplete(new OnComplete<T>() {
            @Override
            public void onComplete(@Nullable Throwable failure, @Nullable T success) {
                if (failure != null) {
                    completableFuture.completeExceptionally(failure);
                } else {
                    completableFuture.complete(success);
                }
            }
        }, executionContext);
        return completableFuture;
    }
}
