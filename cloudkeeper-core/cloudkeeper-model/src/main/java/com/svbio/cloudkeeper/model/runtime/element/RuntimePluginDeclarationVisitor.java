package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration;

import javax.annotation.Nullable;

public interface RuntimePluginDeclarationVisitor<T, P> {
    /**
     * Visits a module declaration.
     */
    @Nullable
    T visit(RuntimeModuleDeclaration declaration, @Nullable P parameter);

    /**
     * Visits a type declaration.
     */
    @Nullable
    T visit(RuntimeTypeDeclaration declaration, @Nullable P parameter);

    /**
     * Visits an annotation-type declaration.
     */
    @Nullable
    T visit(RuntimeAnnotationTypeDeclaration declaration, @Nullable P parameter);

    /**
     * Visits a serialization declaration.
     */
    @Nullable
    T visit(RuntimeSerializationDeclaration declaration, @Nullable P parameter);
}
