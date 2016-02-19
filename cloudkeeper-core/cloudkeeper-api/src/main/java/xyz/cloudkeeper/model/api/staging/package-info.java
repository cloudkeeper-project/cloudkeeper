/**
 * Defines interfaces and exceptions for CloudKeeper staging areas.
 *
 * <p>A staging area is a mapping from execution traces to objects. It serves as central place that contains module
 * inputs and outputs, as well as intermediate results. It may be seen as a key-value store where the keys are
 * execution traces ({@link xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace} instances) and the
 * values are arbitrary objects.
 *
 * @see xyz.cloudkeeper.model.api.staging.StagingArea
 */
@NonNullByDefault
package xyz.cloudkeeper.model.api.staging;

import xyz.cloudkeeper.model.util.NonNullByDefault;
