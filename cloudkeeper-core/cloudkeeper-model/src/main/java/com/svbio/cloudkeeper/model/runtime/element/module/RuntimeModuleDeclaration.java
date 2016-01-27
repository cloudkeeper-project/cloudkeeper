package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;

import javax.annotation.Nullable;

public interface RuntimeModuleDeclaration
        extends RuntimePluginDeclaration, BareModuleDeclaration, RuntimePortContainer {
    @Nullable
    <T, P> T accept(RuntimeModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter);
}
