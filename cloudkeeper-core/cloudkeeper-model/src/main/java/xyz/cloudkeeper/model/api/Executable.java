package xyz.cloudkeeper.model.api;

/**
 * Functional interface for processing the user-defined code that defines a simple module.
 *
 * <p>Implementations are required to be thread-safe and are encouraged to be immutable.
 */
@FunctionalInterface
public interface Executable {
    /**
     * Executes the simple module represented by this executable.
     *
     * <p>Within this method, inputs are expected to be retrieved using
     * {@link ModuleConnector#getInput(xyz.cloudkeeper.model.immutable.element.SimpleName)}. Before the method
     * returns, output for <em>all</em> out-ports need to have been provided using
     * {@link ModuleConnector#setOutput(xyz.cloudkeeper.model.immutable.element.SimpleName, Object)}.
     *
     * <p>Implementations of this method are expected to be very light-weight wrappers around the user-defined code.
     * This way, the time it takes this method to return is a relatively accurate measure of time spent in user code.
     *
     * @param moduleConnector module connector providing interface for retrieving inputs and returning outputs
     * @throws UserException if an exception occurs in user-provided code (the user-defined exception will be available
     *     as cause, also if it is a CloudKeeper exception such as
     *     {@link ConnectorException})
     * @throws ExecutionException if an exception occurs outside of user-provided code
     */
    void run(ModuleConnector moduleConnector) throws ExecutionException;
}
