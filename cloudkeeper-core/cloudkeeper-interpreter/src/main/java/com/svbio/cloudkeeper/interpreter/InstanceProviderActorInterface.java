package com.svbio.cloudkeeper.interpreter;

/**
 * Message interface of the actor created by {@link InstanceProviderActorCreator}.
 */
final class InstanceProviderActorInterface {
    private InstanceProviderActorInterface() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns an {@link com.svbio.cloudkeeper.model.api.staging.InstanceProvider} object.
     *
     * <p>Some providers, for instance {@link com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider}, may need
     * additional context in order to provide the requested instances. For example, a staging-area provider may need
     * access to an executor service with that the newly constructed staging area would schedule asynchronous tasks.
     *
     * <p>The CloudKeeper top-level interpreter actor therefore assumes that there is a <em>local</em> actor (within the
     * same JVM) that responds to messages of this class by replying with a
     * {@link com.svbio.cloudkeeper.model.api.staging.InstanceProvider} instance.
     *
     * <p>This message is only sent locally within the same JVM.
     */
    enum GetInstanceProviderMessage {
        INSTANCE;

        @Override
        public String toString() {
            return String.format("message %s", getClass().getSimpleName());
        }
    }
}
