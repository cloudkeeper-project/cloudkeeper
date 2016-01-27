package com.svbio.cloudkeeper.model.api;

import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * This interface is used to create workflow executions.
 *
 * <p>Each workflow-execution builder represents a (fixed) module. Implementations of this interface are expected to set
 * this module during construction.
 *
 * <p>Instances of this interface are not guaranteed to be thread-safe. A builder should not be used from multiple
 * threads, unless other provisions are in place that guarantee memory consistency.
 */
public interface WorkflowExecutionBuilder {
    /**
     * Sets this builder's bundle identifiers.
     *
     * <p>This method will not retain any bundle identifiers previously set for this builder.
     *
     * @param bundleIdentifiers bundle identifiers
     * @return this builder
     */
    WorkflowExecutionBuilder setBundleIdentifiers(List<URI> bundleIdentifiers);

    /**
     * Sets this builder's annotation overrides.
     *
     * <p>This method will not retain any overrides previously set for this builder.
     *
     * @param overrides annotation overrides
     * @return this builder
     */
    WorkflowExecutionBuilder setOverrides(List<? extends BareOverride> overrides);

    /**
     * Sets this builder's input values.
     *
     * <p>This method will not retain any input values previously set for this builder.
     *
     * @param inputValues inputs values
     * @return this builder
     */
    WorkflowExecutionBuilder setInputs(Map<SimpleName, Object> inputValues);

    /**
     * Starts a new workflow execution of the module represented by this builder and using all current attributes of
     * this builder.
     *
     * <p>It is possible to use the same builder for creating more than one workflow execution. Modifying the builder
     * will not have any effect on any workflow execution previously started with this method.
     *
     * @return a new {@link WorkflowExecution} object for managing the workflow execution
     */
    WorkflowExecution start();
}
