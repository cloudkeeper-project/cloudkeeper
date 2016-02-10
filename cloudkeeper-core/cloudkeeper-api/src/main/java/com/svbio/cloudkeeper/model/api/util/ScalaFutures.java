package com.svbio.cloudkeeper.model.api.util;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.Recover;
import akka.japi.Function2;
import akka.japi.JavaPartialFunction;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction0;
import scala.util.Failure;
import scala.util.Success;
import scala.util.Try;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public final class ScalaFutures {
    private ScalaFutures() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Implementation of functional interface that maps the argument to itself (identity mapping).
     */
    private static final class IdentityMapper<T> extends Mapper<T, T> {
        private static final IdentityMapper<?> INSTANCE = new IdentityMapper<>();

        @SuppressWarnings("unchecked")
        static <T> IdentityMapper<T> instance() {
            return (IdentityMapper<T>) INSTANCE;
        }

        @Override
        public T apply(T parameter) {
            return parameter;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Mapper<T, T> identityMapper() {
        return (Mapper<T, T>) IdentityMapper.INSTANCE;
    }

    /**
     * Creates a new future by applying a function to the successful results of two futures.
     *
     * @param future1 first future of type {@code T1}
     * @param future2 second future of type {@code T2}
     * @param function function to apply to the successful results of {@code future1} and {@code future2}
     * @param executionContext execution context to run the new future in
     * @param <T> type of first future
     * @param <U> type of second future
     * @param <R> result type of combined future
     * @return Future of type {@code R}. If the first future is completed with an exception, then the new future will
     *     also contain this exception. Otherwise, if the second future is completed with an exception, then the new
     *     future will also contain this exception.
     */
    public static <T, U, R> Future<R> map2(
        Future<T> future1,
        final Future<U> future2,
        final Function2<T, U, R> function,
        final ExecutionContext executionContext
    ) {
        // Since Java does not know Scala's for-yield construct, we need to manually do some currying here: the first
        // mapper binds the result of future1, the second binds the result of future2.
        return future1.flatMap(
            new Mapper<T, Future<R>>() {
                @Override
                public Future<R> apply(final T value1) {
                    return future2.map(
                        new Mapper<U, R>() {
                            @Override
                            public R checkedApply(U value2) throws Exception {
                                return function.apply(value1, value2);
                            }
                        },
                        executionContext
                    );
                }
            },
            executionContext
        );
    }

    private static final class WrappedException extends RuntimeException {
        private static final long serialVersionUID = -5551087075125052070L;

        private WrappedException(Throwable cause) {
            super(cause);
        }
    }

    private static <T> Try<T> newTry(ThrowingSupplier<T, Exception> supplier) {
        Try<T> result = Try.apply(new AbstractFunction0<T>() {
            @Override
            public T apply() {
                try {
                    return supplier.get();
                } catch (Exception exception) {
                    throw new WrappedException(exception);
                }
            }
        });
        if (result.isFailure()) {
            Throwable throwable = ((Failure<T>) result).exception();
            if (throwable instanceof WrappedException) {
                result = new Failure<>(throwable.getCause());
            }
        }
        return result;
    }

    private static <T> Future<T> ofTry(Try<T> result) {
        return result.isFailure()
            ? Futures.failed(((Failure<T>) result).exception())
            : Futures.successful(result.get());
    }

    /**
     * Returns a future that will be completed with the result (or exception) of applying the given function the the
     * resource provided by the given future.
     *
     * <p>This method must only be called if <em>it is guaranteed that {@code function}</em> will never throw a runtime
     * exception. Therefore, this method is private.
     */
    public static <R extends AutoCloseable, T> Future<T> internalMapWithResource(Future<R> resource,
            Function<R, Future<Try<T>>> function, ExecutionContext executionContext) {
        return resource.flatMap(new Mapper<R, Future<T>>() {
            @Override
            public Future<T> checkedApply(R resource) {
                return function.apply(resource)
                    .flatMap(new Mapper<Try<T>, Future<T>>() {
                        @Override
                        public Future<T> checkedApply(Try<T> tryResult) throws IOException {
                            Try<Boolean> tryClose = newTry(
                                () -> {
                                    resource.close();
                                    return Boolean.TRUE;
                                }
                            );
                            if (tryResult.isFailure()) {
                                Throwable failure = ((Failure<T>) tryResult).exception();
                                if (tryClose.isFailure()) {
                                    failure.addSuppressed(((Failure<Boolean>) tryClose).exception());
                                }
                                return Futures.failed(failure);
                            } else if (tryClose.isFailure()) {
                                return Futures.failed(((Failure<Boolean>) tryClose).exception());
                            } else {
                                return Futures.successful(tryResult.get());
                            }
                        }
                    }, executionContext);
            }
        }, executionContext);
    }

    /**
     * Returns a new future that will be completed with the result of the future obtained from applying the given
     * function to the resource provided by the given future.
     *
     * <p>This method ensures that the runtime context passed to {@code function} is properly closed, even if an
     * exception occurs at any stage. This method may thus regarded as an asynchronous try-with-resources implementation
     * (with just one resource: the runtime context).
     *
     * @param resourceFuture future that will be completed with the resource
     * @param function function returning a new future that will provide the result of the returned future
     * @param executionContext execution context used for executing asynchronous tasks
     * @param <R> the type of the returned future
     * @return the future
     */
    public static <R extends AutoCloseable, T> Future<T> flatMapWithResource(Future<R> resourceFuture,
            ThrowingFunction<R, Future<T>, Exception> function, ExecutionContext executionContext) {
        return internalMapWithResource(
            resourceFuture,
            resource -> {
                Try<Future<T>> futureTry = newTry(() -> function.apply(resource))
                    .recover(new Recover<Future<T>>() {
                        @Override
                        public Future<T> recover(Throwable throwable) {
                            return Futures.failed(throwable);
                        }
                    });
                return tryFuture(futureTry.get(), executionContext);
            },
            executionContext
        );
    }

    /**
     * Returns a new future that will be completed with the result of the given function when applied to the resource
     * provided by the given future.
     *
     * <p>This method ensures that the resource passed to {@code function} is properly closed, even if an exception
     * occurs at any stage. This method may thus regarded as an asynchronous try-with-resources implementation (with
     * just one resource: {@code R} instance that {@code resourceFuture} will is completed with).
     *
     * @param resourceFuture future that will be completed with the resource
     * @param function function computing the result for the new future
     * @param executionContext execution context used for executing asynchronous tasks
     * @param <R> the type of the returned future
     * @return the future
     */
    public static <R extends AutoCloseable, T> Future<T> mapWithResource(Future<R> resourceFuture,
            ThrowingFunction<R, T, Exception> function, ExecutionContext executionContext) {
        return internalMapWithResource(
            resourceFuture,
            resource -> Futures.successful(newTry(() -> function.apply(resource))),
            executionContext
        );
    }

    /**
     * Returns a new future that is <em>synchronously</em> completed with the value obtained by calling the given
     * {@link ThrowingSupplier}.
     *
     * <p>This method is a synchronous equivalent of {@link Futures#future(Callable, ExecutionContext)}. In particular,
     * errors are handled identically. If the given supplier throws an exception or a non-fatal error (see Scala
     * documentation), the returned future is completed with that {@link Throwable}.
     *
     * @param supplier a function returning the value to be used to complete the returned future
     * @param <T> the function's return type
     * @return the new future
     */
    public static <T> Future<T> supplySync(ThrowingSupplier<T, Exception> supplier) {
        return Try
            .apply(new AbstractFunction0<Future<T>>() {
                @Override
                public Future<T> apply() {
                    try {
                        return Futures.successful(supplier.get());
                    } catch (Exception exception) {
                        return Futures.failed(exception);
                    }
                }
            })
            .recover(new JavaPartialFunction<Throwable, Future<T>>() {
                @Override
                @Nullable
                public Future<T> apply(Throwable throwable, boolean isCheck) {
                    if (isCheck) {
                        // The method is called only to check whether the partial function is defined for the value of
                        // throwable. If no exception is thrown, this indicates 'yes'.
                        return null;
                    }
                    return Futures.failed(throwable);
                }
            })
            .get();
    }

    private static final class ToSuccessMapper<T> extends Mapper<T, Try<T>> {
        private static final ToSuccessMapper<?> INSTANCE = new ToSuccessMapper<>();

        @SuppressWarnings("unchecked")
        private static <T> ToSuccessMapper<T> getInstance() {
            return (ToSuccessMapper<T>) INSTANCE;
        }

        @Override
        public Try<T> apply(T result) {
            return new Success<T>(result);
        }
    }

    private static final class ToFailureRecover<T> extends Recover<Try<T>> {
        private static final ToFailureRecover<?> INSTANCE = new ToFailureRecover<>();

        @SuppressWarnings("unchecked")
        private static <T> ToFailureRecover<T> getInstance() {
            return (ToFailureRecover<T>) INSTANCE;
        }

        @Override
        public Try<T> recover(Throwable throwable) {
            return new Failure<>(throwable);
        }
    }

    /**
     * Returns a new future that is guaranteed to be completed successfully with a {@link Try} instance wrapping the
     * completion of the given future.
     *
     * <p>If the given future is completed successfully, the returned future will be completed successfully with a
     * {@link Success} instance (wrapping the value of the original future). If the given future is completed
     * exceptionally, the returned future will be completed successfully with a {@link Failure} instance (wrapping the
     * {@link Throwable} of the failure).
     *
     * @param future future the completion of which is to be wrapped in a new future of type {@link Try}
     * @param executionContext execution context that will be used to execute all newly created futures
     * @param <A> type of the value that the given future will be completed with
     * @return new future that is guaranteed to be completed successfully with a {@link Try} instance
     */
    public static <A> Future<Try<A>> tryFuture(Future<A> future, ExecutionContext executionContext) {
        return future
            .map(ToSuccessMapper.getInstance(), executionContext)
            .recover(ToFailureRecover.getInstance(), executionContext);
    }

    /**
     * Returns a future that will be completed with a list of the values the given element futures will be completed
     * with.
     *
     * <p>Unlike {@link Futures#sequence(Iterable, ExecutionContext)}, this method guarantees that the returned future
     * will only be completed <em>after</em> all given element futures have been completed.
     *
     * <p>If a failure occurs in any of the element futures, the returned future will be completed exceptionally. In
     * this case, the first exceptionally completed element future (in list order) yields the exception that the
     * returned future will be completed with. All other exceptions will be added as suppressed exceptions (again in
     * list order).
     *
     * @param elementFutures list of futures
     * @param executionContext execution context that will be used to execute all newly created futures
     * @param <A> type of the values each of the given futures will be completed with
     * @return future that will be completed with a list of the values that the given futures will be completed with
     */
    public static <A> Future<ImmutableList<A>> createListFuture(List<Future<A>> elementFutures,
            ExecutionContext executionContext) {
        // In the first step, construct a future that
        // 1. will be completed with a list of Try<A> objects, in the same order as the given futures
        // 2. will be completed only *after* all given futures are completed
        // 3. will always be completed successfully
        Future<ArrayList<Try<A>>> tryListFuture = Futures.successful(new ArrayList<>(elementFutures.size()));
        for (Future<A> elementFuture: elementFutures) {
            tryListFuture = tryListFuture.flatMap(new Mapper<ArrayList<Try<A>>, Future<ArrayList<Try<A>>>>() {
                @Override
                public Future<ArrayList<Try<A>>> apply(ArrayList<Try<A>> list) {
                    // Now create a future that is guaranteed to be completed successfully with a Try<A> instance: If
                    // elementFuture is completed with a failure, tryFuture will be completed successfully with a
                    // Failure instance.
                    Future<Try<A>> tryFuture = tryFuture(elementFuture, executionContext);
                    // Now return a future that is guaranteed to be completed only after all element futures seen so
                    // far, and after tryFuture.
                    return tryFuture
                        .map(new Mapper<Try<A>, ArrayList<Try<A>>>() {
                            @Override
                            public ArrayList<Try<A>> apply(Try<A> parameter) {
                                list.add(parameter);
                                return list;
                            }
                        }, executionContext);
                }
            }, executionContext);
        }

        // In the second step, construct a future that transforms tryListFuture back into a future that will be
        // completed with a ImmutableList<A>.
        return tryListFuture.flatMap(new Mapper<ArrayList<Try<A>>, Future<ImmutableList<A>>>() {
            @Override
            public Future<ImmutableList<A>> apply(ArrayList<Try<A>> list) {
                // The following is just a left-fold.
                ArrayList<A> builder = new ArrayList<>(list.size());
                Throwable throwable = null;
                for (Try<A> elementTry : list) {
                    if (elementTry.isFailure()) {
                        Throwable currentFailure = ((Failure<A>) elementTry).exception();
                        if (throwable == null) {
                            throwable = currentFailure;
                        } else {
                            throwable.addSuppressed(currentFailure);
                        }
                    } else {
                        builder.add(elementTry.get());
                    }
                }
                return throwable != null
                    ? Futures.failed(throwable)
                    : Futures.successful(ImmutableList.copyOf(builder));
            }
        }, executionContext);
    }
}
