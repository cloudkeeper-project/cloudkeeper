package xyz.cloudkeeper.model.runtime.element.annotation;

import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;
import xyz.cloudkeeper.model.util.ImmutableList;

public interface RuntimeAnnotationTypeDeclaration extends BareAnnotationTypeDeclaration, RuntimePluginDeclaration {
    @Override
    ImmutableList<? extends RuntimeAnnotationTypeElement> getElements();
}
