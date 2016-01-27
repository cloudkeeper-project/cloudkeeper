package com.svbio.cloudkeeper.model.api.executor;

import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import scala.concurrent.Future;

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
     *     {@link com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace.Type#MODULE} and must represent a
     *     simple module)
     * @return future that will be completed with an {@link ExtendedModuleConnector} instance on success and an
     *     {@link com.svbio.cloudkeeper.model.api.staging.StagingException} on failure.
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if the argument does not satisfy the constraints described above
     */
    Future<ExtendedModuleConnector> provideModuleConnector(StagingArea stagingArea);
}
