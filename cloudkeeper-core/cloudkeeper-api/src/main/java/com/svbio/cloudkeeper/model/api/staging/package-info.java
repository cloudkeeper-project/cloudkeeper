/**
 * Defines interfaces and exceptions for CloudKeeper staging areas.
 *
 * <p>A staging area is a mapping from execution traces to objects. It serves as central place that contains module
 * inputs and outputs, as well as intermediate results. It may be seen as a key-value store where the keys are
 * execution traces ({@link com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace} instances) and the values
 * are arbitrary objects.
 *
 * @see com.svbio.cloudkeeper.model.api.staging.StagingArea
 */
@NonNullByDefault
package com.svbio.cloudkeeper.model.api.staging;

import com.svbio.cloudkeeper.model.util.NonNullByDefault;
