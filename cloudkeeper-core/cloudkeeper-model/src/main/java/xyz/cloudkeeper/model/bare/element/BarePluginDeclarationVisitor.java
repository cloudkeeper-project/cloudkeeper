package xyz.cloudkeeper.model.bare.element;

import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;

import javax.annotation.Nullable;

/**
 * Visitor of plugin declarations, in the style of the visitor design pattern.
 */
public interface BarePluginDeclarationVisitor<T, P> {
    /**
     * Visits a module declaration.
     */
    @Nullable
    T visit(BareModuleDeclaration declaration, @Nullable P parameter);

    /**
     * Visits a type declaration.
     */
    @Nullable
    T visit(BareTypeDeclaration declaration, @Nullable P parameter);

    /**
     * Visits an annotation-type declaration.
     */
    @Nullable
    T visit(BareAnnotationTypeDeclaration declaration, @Nullable P parameter);

    /**
     * Visits a serialization declaration.
     */
    @Nullable
    T visit(BareSerializationDeclaration declaration, @Nullable P parameter);
}
