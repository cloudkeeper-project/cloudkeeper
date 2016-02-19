package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import xyz.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;

import javax.annotation.Nullable;

public interface RuntimeModuleDeclaration
        extends RuntimePluginDeclaration, BareModuleDeclaration, RuntimePortContainer {
    @Nullable
    <T, P> T accept(RuntimeModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter);
}
