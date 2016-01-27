package com.svbio.cloudkeeper.model.api.util;

/**
 * Represents a supplier of results. The supplier may throw a checked exception.
 *
 * <p>There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 *
 * @param <T> the type of results supplied by this supplier
 * @param <E> the type of the checked exception thrown by the function
 *
 * @see java.util.function.Supplier
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E;
}
