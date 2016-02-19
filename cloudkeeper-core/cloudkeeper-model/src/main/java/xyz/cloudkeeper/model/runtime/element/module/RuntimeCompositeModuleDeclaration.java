package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;

import javax.annotation.Nonnull;

/**
 * Verified and linked runtime representation of a composite-module declaration.
 */
public interface RuntimeCompositeModuleDeclaration extends RuntimeModuleDeclaration, BareCompositeModuleDeclaration {
    @Override
    @Nonnull
    RuntimeCompositeModule getTemplate();
}
