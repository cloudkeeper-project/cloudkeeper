package com.svbio.cloudkeeper.model.runtime.element.annotation;

import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeTypeMirror;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RuntimeAnnotationTypeElement extends BareAnnotationTypeElement, RuntimeElement {
    @Override
    @Nonnull
    SimpleName getSimpleName();

    @Override
    @Nonnull
    RuntimeTypeMirror getReturnType();

    /**
     * {@inheritDoc}
     *
     * <p>This method returns null if this annotation type element does not have a default value.
     *
     * @return default value, or {@code null} if this annotation type element does not have a default value
     */
    @Override
    @Nullable
    RuntimeAnnotationValue getDefaultValue();
}
