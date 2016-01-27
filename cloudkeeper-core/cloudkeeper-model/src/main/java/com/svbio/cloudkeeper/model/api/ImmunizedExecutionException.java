package com.svbio.cloudkeeper.model.api;

/**
 * Execution exception that is "immunized" against {@link ClassNotFoundException} upon deserialization in other JVMs.
 *
 * <p>Instances of this class are safe to be serialized and sent to remote JVMs. It is "stripped" of all instances of
 * classes that may not be available in the target context. Specifically, it only references (even transitively) only
 * instances of classes that are either part of the JDK or part of this package.
 *
 * <p>Yet, the returned execution exception is indistinguishable in printed stack traces.
 *
 * @see ExecutionException#toImmunizedException()
 */
final class ImmunizedExecutionException extends ExecutionException {
    private static final long serialVersionUID = 5937542440858167762L;

    private final String string;

    /**
     * Constructs an "immunized" copy of the given {@link Throwable}.
     *
     * <p>Note that the given {@link Throwable} will <em>not</em> be the cause of the constructed immunized exception.
     * Instead, this constructor is an "immunizing" copy constructor.
     *
     * @param original original {@link Throwable} to create an "immunized" copy of
     */
    private ImmunizedExecutionException(Throwable original) {
        super(original.getMessage(), proxyForNullable(original.getCause()));
        setStackTrace(original.getStackTrace());
        for (Throwable suppressed: original.getSuppressed()) {
            addSuppressed(new ImmunizedExecutionException(suppressed));
        }
        string = original.toString();
    }

    /**
     * Returns a new "immunized" copy of the given {@link ExecutionException}.
     *
     * @param original original {@link ExecutionException} to create an "immunized" copy of
     * @return the new "immunized" execution exception
     */
    static ImmunizedExecutionException proxyFor(ExecutionException original) {
        return original instanceof ImmunizedExecutionException
            ? (ImmunizedExecutionException) original
            : new ImmunizedExecutionException(original);
    }

    private static ImmunizedExecutionException proxyForNullable(Throwable original) {
        return original == null
            ? null
            : new ImmunizedExecutionException(original);
    }

    @Override
    public String toString() {
        return string;
    }
}
