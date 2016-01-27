package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeAnnotatedConstruct;
import com.svbio.cloudkeeper.model.util.ImmutableList;

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
