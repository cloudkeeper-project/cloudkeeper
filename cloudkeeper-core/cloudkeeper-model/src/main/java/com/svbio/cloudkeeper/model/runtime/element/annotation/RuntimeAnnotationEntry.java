package com.svbio.cloudkeeper.model.runtime.element.annotation;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RuntimeAnnotationEntry extends BareAnnotationEntry, Immutable {
    /**
     * Returns true if the given object represents an annotation that is logically equivalent to this one, as specified
     * for {@link java.lang.annotation.Annotation#equals(Object)}.
     */
    boolean equals(@Nullable Object otherObject);

    /**
     * Returns the hash code as specified for {@link java.lang.annotation.Annotation#hashCode()}.
     *
     * <p>The hash code of an annotation entry is (127 times {@code getKey().getSimpleName().toString().hashCode()} XOR
     * {@code getValue().hashCode()}).
     */
    @Override
    int hashCode();

    @Override
    @Nonnull
    RuntimeAnnotationTypeElement getKey();

    @Override
    @Nonnull
    RuntimeAnnotationValue getValue();
}
