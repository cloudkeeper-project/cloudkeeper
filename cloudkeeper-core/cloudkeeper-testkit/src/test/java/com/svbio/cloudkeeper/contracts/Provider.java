package com.svbio.cloudkeeper.contracts;

/**
 * Provider for (non-parametrized) instances in contract tests.
 */
@FunctionalInterface
public interface Provider<T> {
    /**
     * Perform pre-contract actions.
     *
     * <p>This method is called from within an {@link org.testng.annotations.BeforeClass} annotated method. It is
     * therefore possible to throw a {@link org.testng.SkipException} in this method.
     */
    default void preContract() { }

    /**
     * Returns a fresh instance.
     */
    T get();
}
