package xyz.cloudkeeper.model.api;

import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.nio.file.Path;

/**
 * CloudKeeper module connector.
 *
 * <p>This interface provides low-level methods for retrieving module inputs and setting module outputs. Implementations
 * of this interface are thread-safe.
 */
public interface ModuleConnector {
    /**
     * Returns the execution trace representing the current module invocation.
     *
     * <p>The execution trace also contains the runtime representation of the current module, which can be retrieved
     * using {@link RuntimeAnnotatedExecutionTrace#getModule()}.
     *
     * @return the execution trace representing the current module invocation
     */
    RuntimeAnnotatedExecutionTrace getExecutionTrace();

    /**
     * Returns the working directory for this module execution that may be used as temporary workspace.
     *
     * <p>User-defined code must not rely on the working directory of the JVM (system property {@code user.dir}), but
     * should use the directory returned by this method instead. This also precludes relying on methods that may use
     * {@code user.dir} indirectly, such as {@link java.nio.file.Paths#get(String, String...)} when called with a
     * relative path.
     *
     * <p>The returned directory is guaranteed to be cleaned up once the user-define code is out of scope. Multiple
     * invocations always return the same directory.
     *
     * @return the working directory for this module execution
     */
    Path getWorkingDirectory();

    /**
     * Returns the value of the in-port with the given name.
     *
     * @param inPortName name of in-port
     * @return value of the given in-port
     * @throws NullPointerException if the argument is null
     * @throws ConnectorException If the value cannot be retrieved; for instance, because the identifier does not refer
     *     to an in-port, or because the value could not be read from the staging area.
     */
    Object getInput(SimpleName inPortName);

    /**
     * Sets the value of the out-port with the given name.
     *
     * @param outPortName name of out-port
     * @param value value
     * @throws NullPointerException if any argument is null
     * @throws ConnectorException If the value cannot be written; for instance, because the identifier does not refer
     *     to an out-port, or because the value could not be written to the staging area.
     */
    void setOutput(SimpleName outPortName, Object value);
}
