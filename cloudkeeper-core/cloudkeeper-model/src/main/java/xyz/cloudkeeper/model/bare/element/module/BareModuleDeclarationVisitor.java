package xyz.cloudkeeper.model.bare.element.module;

import javax.annotation.Nullable;

/**
 * Visitor of module declarations, in the style of the visitor design pattern.
 */
public interface BareModuleDeclarationVisitor<T, P> {
    /**
     * Visits a composite module declaration.
     */
    @Nullable
    T visit(BareCompositeModuleDeclaration declaration, @Nullable P parameter);

    /**
     * Visits a simple module declaration.
     */
    @Nullable
    T visit(BareSimpleModuleDeclaration declaration, @Nullable P parameter);
}
