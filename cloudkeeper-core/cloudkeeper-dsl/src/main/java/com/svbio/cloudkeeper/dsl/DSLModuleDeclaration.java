package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import java.util.List;

abstract class DSLModuleDeclaration implements BareModuleDeclaration, Immutable {
    private final SimpleName simpleName;
    private final List<DSLAnnotation> annotations;

    DSLModuleDeclaration(Class<?> clazz) {
        simpleName = Shared.simpleNameOfClass(clazz);
        annotations = DSLAnnotation.unmodifiableAnnotationList(clazz);
    }

    @Override
    public final <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public abstract String toString();

    @Override
    public SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public List<DSLAnnotation> getDeclaredAnnotations() {
        return annotations;
    }
}
