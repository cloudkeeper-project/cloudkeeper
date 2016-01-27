package com.svbio.cloudkeeper.model.api;

import com.svbio.cloudkeeper.model.bare.element.module.BareModule;

/**
 * CloudKeeper system.
 *
 * <p>Implementations of this interface are thread-safe.
 */
public interface CloudKeeperEnvironment {
    /**
     * Returns a new workflow-execution builder for the given module.
     *
     * @param module module for that the builder is to be created
     * @return the new workflow-execution builder
     * @throws NullPointerException if the argument is null
     */
    WorkflowExecutionBuilder newWorkflowExecutionBuilder(BareModule module);
}
