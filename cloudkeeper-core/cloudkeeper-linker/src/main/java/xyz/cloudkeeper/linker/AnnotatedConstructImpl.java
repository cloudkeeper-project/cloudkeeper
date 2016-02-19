package xyz.cloudkeeper.linker;

import net.florianschoppmann.java.reflect.ReflectionTypes;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.BareAnnotatedConstruct;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class AnnotatedConstructImpl extends LocatableImpl implements IAnnotatedConstructImpl {
    private final ImmutableList<AnnotationImpl> declaredAnnotations;
    private final Map<Name, AnnotationImpl> declaredAnnotationsMap;

    /**
     * Constructor that allows to set the initial {@link State}.
     */
    AnnotatedConstructImpl(State initialState, CopyContext parentContext) {
        super(initialState, parentContext);
        declaredAnnotationsMap = Collections.emptyMap();
        declaredAnnotations = ImmutableList.of();
    }

    AnnotatedConstructImpl(@Nullable BareAnnotatedConstruct original, CopyContext parentContext)
            throws LinkerException {
        super(original, parentContext);
        assert original != null;
        declaredAnnotationsMap = unmodifiableMapOf(original.getDeclaredAnnotations(), "declaredAnnotations",
            AnnotationImpl::new, AnnotationImpl::getKey);
        declaredAnnotations = ImmutableList.copyOf(declaredAnnotationsMap.values());
    }

    @Override
    @Nullable
    public abstract IAnnotatedConstructImpl getSuperAnnotatedConstruct();

    @Override
    public final AnnotationImpl getDeclaredAnnotation(Name annotationName) {
        return declaredAnnotationsMap.get(annotationName);
    }

    @Override
    public final ImmutableList<AnnotationImpl> getDeclaredAnnotations() {
        return declaredAnnotations;
    }

    static UnsupportedOperationException unsupportedException() {
        return new UnsupportedOperationException(String.format(
            "Annotations not currently supported by %s.", ReflectionTypes.class
        ));
    }

    /**
     * Returns this construct's annotation of the specified type if such an annotation is <em>present</em>, else null.
     *
     * <p>This method implements {@link javax.lang.model.AnnotatedConstruct#getAnnotation(Class)}.
     *
     * @throws UnsupportedOperationException whenever this method is called
     */
    @SuppressWarnings("unused")
    public final <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw unsupportedException();
    }

    /**
     * Returns the annotations that are directly present on this construct.
     *
     * This method is needed to implement {@code javax.lang.model.AnnotatedConstruct} on Java 1.8 and newer.
     *
     * @throws UnsupportedOperationException whenever this method is called
     */
    @SuppressWarnings("unused")
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
        throw unsupportedException();
    }

    /**
     * Returns annotations that are associated with this construct.
     *
     * This method is needed to implement {@code javax.lang.model.AnnotatedConstruct} on Java 1.8 and newer.
     *
     * @throws UnsupportedOperationException whenever this method is called
     */
    @SuppressWarnings("unused")
    public final <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw unsupportedException();
    }

    @Override
    final void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.addAll(declaredAnnotations);
        collectEnclosedByAnnotatedConstruct(freezables);
    }

    abstract void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables);
}
