package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;

/**
 * Type that represents a the completion of a computation, which may either be a successfully computed value or an
 * exception.
 *
 * @param <T> type of successfully computed value
 */
abstract class Try<T> {
    @FunctionalInterface
    public interface Tryable<T> {
        T apply() throws LinkerException;
    }

    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Try<T> failure(LinkerException exception) {
        return new Failure<>(exception);
    }

    public static <T> Try<T> run(Tryable<T> tryable) {
        try {
            return success(tryable.apply());
        } catch (LinkerException exception) {
            return failure(exception);
        }
    }

    /**
     * Returns the value from this {@link Success} or throws the exception if this is a {@link Failure}.
     *
     * @throws LinkerException if this object is a {@link Failure}
     */
    abstract T get() throws LinkerException;

    /**
     * Success.
     *
     * @param <T> type of successfully computed value
     */
    private static final class Success<T> extends Try<T> {
        private final T value;

        Success(T value) {
            this.value = value;
        }

        @Override
        T get() {
            return value;
        }
    }

    /**
     * Failure.
     *
     * @param <T> type of successfully computed value
     */
    private static final class Failure<T> extends Try<T> {
        private final LinkerException exception;

        Failure(LinkerException exception) {
            this.exception = exception;
        }

        @Override
        T get() throws LinkerException {
            throw exception;
        }
    }
}
