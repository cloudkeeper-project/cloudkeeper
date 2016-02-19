package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.runtime.element.RuntimeAnnotatedConstruct;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;

interface IAnnotatedConstructImpl extends RuntimeAnnotatedConstruct {
    @Override
    @Nullable
    IAnnotatedConstructImpl getSuperAnnotatedConstruct();

    @Override
    @Nullable
    AnnotationImpl getDeclaredAnnotation(Name annotationName);

    @Override
    ImmutableList<AnnotationImpl> getDeclaredAnnotations();
}
