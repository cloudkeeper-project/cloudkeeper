package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;

import javax.annotation.Nonnull;

/**
 * Linked module.
 *
 * <p>{@link RuntimeProxyModule} are similar to function calls in a regular programming language. They contain a
 * reference to a module declaration.
 */
public interface RuntimeProxyModule extends BareProxyModule, RuntimeModule {
    /**
     * Returns the declaration.
     *
     * @return declaration (guaranteed to be non-null)
     */
    @Override
    @Nonnull
    RuntimeModuleDeclaration getDeclaration();
}
