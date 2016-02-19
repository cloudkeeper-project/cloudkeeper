package xyz.cloudkeeper.model.api.executor;

import scala.concurrent.Future;
import xyz.cloudkeeper.model.api.staging.StagingArea;

/**
 * Provider (factory) of {@link ExtendedModuleConnector} instances corresponding to a staging area and its represented
 * simple module.
 */
public interface ModuleConnectorProvider {
    /**
     * Returns a future that will be completed with a {@link ExtendedModuleConnector} instance.
     *
     * @param stagingArea staging area (the absolute execution trace returned by
     *     {@link StagingArea#getAnnotatedExecutionTrace()} must be of type
     *     {@link xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace.Type#MODULE} and must represent a
     *     simple module)
     * @return future that will be completed with an {@link ExtendedModuleConnector} instance on success and an
     *     {@link xyz.cloudkeeper.model.api.staging.StagingException} on failure.
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if the argument does not satisfy the constraints described above
     */
    Future<ExtendedModuleConnector> provideModuleConnector(StagingArea stagingArea);
}
