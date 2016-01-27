package com.svbio.cloudkeeper.model.runtime.element.annotation;

import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;
import com.svbio.cloudkeeper.model.util.ImmutableList;

public interface RuntimeAnnotationTypeDeclaration extends BareAnnotationTypeDeclaration, RuntimePluginDeclaration {
    @Override
    ImmutableList<? extends RuntimeAnnotationTypeElement> getElements();
}
