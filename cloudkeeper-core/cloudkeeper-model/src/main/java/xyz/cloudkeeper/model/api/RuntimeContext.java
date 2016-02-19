package xyz.cloudkeeper.model.api;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.io.IOException;
import java.util.List;

/**
 * CloudKeeper runtime context.
 *
 * <p>The CloudKeeper runtime state consists of:
 * <ul><li>
 *     a {@link RuntimeRepository} instance for access to CloudKeeper plug-in declarations
 * </li><li>
 *     a {@link ClassLoader} instance for loading Java classes
 * </li></ul>
 *
 * <p>Instances of this interface are guaranteed to be thread-safe, but not necessarily immutable.
 */
public interface RuntimeContext extends AutoCloseable {
    @Override
    void close() throws IOException;

    /**
     * Returns the repository that provides access to CloudKeeper plug-in declarations.
     *
     * @return the repository
     */
    RuntimeRepository getRepository();

    /**
     * Returns the class loader that provides access to Java classes.
     *
     * <p>This class loader should be used by user-defined code such as {@link Marshaler} implementations.
     *
     * @return the class loader
     */
    ClassLoader getClassLoader();

    /**
     * Returns an absolute annotated execution trace.
     *
     * <p>This method links the given module using the provided annotation overrides and the repository contained in
     * this runtime context. It creates an annotated execution trace from the given (bare) execution trace, which will
     * be of type module and represent the given module.
     *
     * @param absoluteTrace absolute execution trace that will correspond to the given module
     * @param bareModule bare module
     * @param overrides list of annotation overrides
     * @return absolute annotated execution trace
     * @throws LinkerException if linking fails because of inconsistent or incomplete input
     */
    RuntimeAnnotatedExecutionTrace newAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace, BareModule bareModule,
        List<? extends BareOverride> overrides) throws LinkerException;
}
