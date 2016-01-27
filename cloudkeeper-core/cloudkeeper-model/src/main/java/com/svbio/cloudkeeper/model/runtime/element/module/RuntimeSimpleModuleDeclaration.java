package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.api.Executable;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;

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
