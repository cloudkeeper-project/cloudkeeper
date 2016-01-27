package com.svbio.cloudkeeper.model.api.staging;

import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.io.Serializable;

/**
 * Initial staging-area provider.
 *
 * <p>Instances of this interface provide a serializable "recipe" from which the staging area can be reconstructed,
 * including in a separate JVM. In order to reconstruct a {@link StagingArea}, additional context beyond the "embedded"
 * parameters may be necessary. This context is expected to be accessible through the {@link InstanceProvider} that is
 * passed to
 * {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, InstanceProvider)}.
 *
 * <p>The necessity of context during deserialization is also the raison d'être for this interface. {@link StagingArea}
 * implementations could typically not be {@link Serializable} in a reasonable manner because Java serialization by
 * itself (specifically, method {@code void readObject(java.io.ObjectInputStream in)}) does not provide the required
 * additional context.
 */
@FunctionalInterface
public interface StagingAreaProvider extends Serializable {
    /**
     * Returns a new {@link StagingArea} instance.
     *
     * @param runtimeContext runtime context, which consists of the CloudKeeper plug-in repository and the Java class
     *     loader
     * @param executionTrace execution trace, containing the current call stack
     * @param instanceProvider instance provider to be used as context when creating a new staging area
     * @return the staging area
     * @throws InstanceProvisionException if a {@link StagingArea} instance cannot be provided
     */
    StagingArea provideStaging(RuntimeContext runtimeContext, RuntimeAnnotatedExecutionTrace executionTrace,
        InstanceProvider instanceProvider) throws InstanceProvisionException;
}
