package com.svbio.cloudkeeper.model.runtime.element.annotation;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

public interface RuntimeAnnotation extends BareAnnotation, Immutable {
    /**
     * Returns the CloudKeeper annotation type declaration for this annotation.
     */
    @Override
    @Nonnull
    RuntimeAnnotationTypeDeclaration getDeclaration();

    /**
     * {@inheritDoc}
     *
     * Elements that are implicitly assuming their default values are not included in the returned list.
     */
    @Override
    ImmutableList<? extends RuntimeAnnotationEntry> getEntries();

    /**
     * Returns the annotation value for the element with the given name.
     */
    @Nullable
    RuntimeAnnotationValue getValue(SimpleName name);

    /**
     * Returns a Java annotation corresponding to this CloudKeeper annotation instance.
     *
     * <p>Implementations may cache the result, which maybe reused if {@code annotationClass} compares equal to the
     * class of the cached annotation instance (that is, if {@code annotationClass} has the same name as the cached
     * annotation instance, but stems from a different class loader, this method does not return the cached result).
     *
     * @param annotationClass Java annotation-type declaration
     * @param <T> type of the Java annotation-type declaration
     * @return Java annotation with the values of this instance
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if the name of the Java annotation class does not match
     *     {@link #getDeclaration()} or if {@link Class#isAnnotation()} is false
     */
    <T extends Annotation> T getJavaAnnotation(Class<T> annotationClass);
}
