package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;

import java.util.List;

abstract class DSLMixinPluginDeclaration implements BarePluginDeclaration, Immutable {
    private final List<DSLAnnotation> annotations;

    DSLMixinPluginDeclaration(DSLPluginDescriptor descriptor) {
        annotations = DSLAnnotation.unmodifiableAnnotationList(descriptor.getClassWithAnnotation());
    }

    @Override
    public abstract String toString();

    @Override
    public final List<DSLAnnotation> getDeclaredAnnotations() {
        return annotations;
    }
}
