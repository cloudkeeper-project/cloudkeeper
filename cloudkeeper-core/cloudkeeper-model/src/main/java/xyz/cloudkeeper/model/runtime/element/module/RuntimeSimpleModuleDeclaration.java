package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.api.Executable;
import xyz.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;

/**
 * Simple-module declaration.
 */
public interface RuntimeSimpleModuleDeclaration extends RuntimeModuleDeclaration, BareSimpleModuleDeclaration {
    /**
     * Returns the executable instance for this simple-module declaration.
     *
     * @throws IllegalStateException if the executable instance is not available
     */
    Executable toExecutable();
}
