package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.api.Executable;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvisionException;
import com.svbio.cloudkeeper.model.immutable.element.Name;

import java.util.Optional;

/**
 * Provider for resolving simple-module declaration names into {@link Executable} instances.
 */
@FunctionalInterface
public interface ExecutableProvider {
    /**
     * Returns an optional {@link Executable} object that corresponds to the simple-module declaration with the given
     * name.
     *
     * @param name name of simple-module declaration
     * @return the optional {@link Executable} object
     * @throws RuntimeStateProvisionException if the executable instance cannot be created for any reason
     */
    Optional<Executable> provideExecutable(Name name) throws RuntimeStateProvisionException;
}
