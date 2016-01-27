package com.svbio.cloudkeeper.model.api.util;

/**
 * Represents a function that accepts one argument and produces a result. The function may throw a checked exception.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object)}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of the checked exception thrown by the function
 *
 * @see java.util.function.Function
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t) throws E;
}
