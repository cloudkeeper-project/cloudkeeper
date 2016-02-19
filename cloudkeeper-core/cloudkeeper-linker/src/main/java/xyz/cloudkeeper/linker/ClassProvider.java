package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.api.RuntimeStateProvisionException;
import xyz.cloudkeeper.model.immutable.element.Name;

import java.util.Optional;

/**
 * Provider for resolving plug-in declaration names into {@link Class} instances.
 */
@FunctionalInterface
public interface ClassProvider {
    /**
     * Returns an optional {@link Class} instance that corresponds to the plug-in declaration with the given name.
     *
     * @param name name of plug-in declaration (annotation, marshaler, or type declaration)
     * @return the optional {@link Class} instance
     * @throws ClassNotFoundException if the {@link Class} cannot be loaded
     * @throws RuntimeStateProvisionException if the {@link Class} cannot be provided for any other reason
     */
    Optional<Class<?>> provideClass(Name name) throws ClassNotFoundException, RuntimeStateProvisionException;
}
