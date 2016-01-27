package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.BareAnnotatedConstruct;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotation;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;

public interface RuntimeAnnotatedConstruct extends BareAnnotatedConstruct, Immutable {
    /**
     * Returns the annotated construct that this annotated construct inherits annotations from, or {@code null} if there
     * is none.
     *
     * In Java, annotation inheritance corresponds to type inheritance. In CloudKeeper, different annotation inheritance
     * is possible. For instance, a module inherits annotations from the corresponding declaration.
     */
    @Nullable
    RuntimeAnnotatedConstruct getSuperAnnotatedConstruct();

    /**
     * Returns this construct's annotation of the specified type if such an annotation is <em>directly present</em>,
     * else null.
     *
     * @param annotationName qualified name of the annotation type
     * @return this construct's annotation of the specified type if such an annotation is <em>directly present</em>,
     *     else null
     */
    @Nullable
    RuntimeAnnotation getDeclaredAnnotation(Name annotationName);

    @Override
    ImmutableList<? extends RuntimeAnnotation> getDeclaredAnnotations();
}
