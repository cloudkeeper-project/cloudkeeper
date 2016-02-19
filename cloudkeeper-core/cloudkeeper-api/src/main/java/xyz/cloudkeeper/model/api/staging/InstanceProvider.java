package xyz.cloudkeeper.model.api.staging;

/**
 * Instance provider.
 *
 * <p>This interface is given to other providers as context. For example, a staging-area provider may need access to an
 * executor service with that the newly constructed staging area would schedule asynchronous tasks.
 */
public interface InstanceProvider {
    /**
     * Returns an instance of the given class/interface.
     *
     * @param requestedClass class/interface of which an instance is requested
     * @param <T> type of class/interface
     * @return instance of the given class/interface, guaranteed to be non-null
     * @throws InstanceProvisionException If the instance cannot be provided.
     */
    <T> T getInstance(Class<T> requestedClass) throws InstanceProvisionException;
}
