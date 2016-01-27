package com.svbio.cloudkeeper.model.runtime.element.module;

import javax.annotation.Nullable;

/**
 * Visitor of linked and verified module declarations, in the style of the visitor design pattern.
 */
public interface RuntimeModuleDeclarationVisitor<T, P> {
    /**
     * Visits a composite module declaration.
     */
    @Nullable
    T visit(RuntimeCompositeModuleDeclaration declaration, @Nullable P parameter);

    /**
     * Visits a simple module declaration.
     */
    @Nullable
    T visit(RuntimeSimpleModuleDeclaration declaration, @Nullable P parameter);
}
