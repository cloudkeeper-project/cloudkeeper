package xyz.cloudkeeper.model.api;

/**
 * Signals that an exception occurred while executing a simple module.
 */
public class ExecutionException extends WorkflowExecutionException {
    private static final long serialVersionUID = -196288201587578402L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public ExecutionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns an execution exception that is "immunized" against {@link ClassNotFoundException} upon deserialization in
     * other JVMs.
     *
     * <p>The returned execution exception is safe to be serialized and sent to remote JVMs. It is "stripped" of all
     * instances of classes that may not be available in the target context. Specifically, it only references (even
     * transitively) only instances of classes that are either part of the JDK or part of this package.
     *
     * <p>Yet, the returned execution exception is indistinguishable in printed stack traces.
     *
     * @return the "immunized" execution exception
     */
    public ExecutionException toImmunizedException() {
        return ImmunizedExecutionException.proxyFor(this);
    }
}
