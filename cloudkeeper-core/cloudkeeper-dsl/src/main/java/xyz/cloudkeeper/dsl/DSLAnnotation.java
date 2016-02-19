package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.dsl.exception.DSLException;
import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.ModelEquivalent;
import xyz.cloudkeeper.model.bare.element.BareQualifiedNameable;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationEntry;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.beans.element.MutableSimpleNameable;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import xyz.cloudkeeper.model.immutable.Location;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DSLAnnotation implements BareAnnotation, Immutable {
    private final MutableQualifiedNamable declaration;
    private final List<MutableAnnotationEntry> elements = new ArrayList<>();

    DSLAnnotation(final Annotation dslAnnotation, Class<? extends Annotation> modelAnnotationType) {
        declaration = new MutableQualifiedNamable().setQualifiedName(modelAnnotationType.getName());

        for (Method method: dslAnnotation.annotationType().getDeclaredMethods()) {
            MutableAnnotationEntry entry = new MutableAnnotationEntry()
                .setKey(
                    new MutableSimpleNameable()
                        .setSimpleName(method.getName())
                )
                .setValue(getValue(dslAnnotation, method));
            elements.add(entry);
        }
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public BareQualifiedNameable getDeclaration() {
        // Defensive copy
        return MutableQualifiedNamable.copyOf(declaration);
    }

    @Override
    public List<? extends BareAnnotationEntry> getEntries() {
        // Defensive copy
        List<MutableAnnotationEntry> copiedEntries = new ArrayList<>(elements.size());
        for (MutableAnnotationEntry entry: elements) {
            copiedEntries.add(MutableAnnotationEntry.copyOf(entry));
        }
        return copiedEntries;
    }

    static List<DSLAnnotation> unmodifiableAnnotationList(AnnotatedElement annotatedElement) {
        Annotation[] annotations = annotatedElement.getAnnotations();
        List<DSLAnnotation> newAnnotationsList = new ArrayList<>();
        for (Annotation annotation: annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            @Nullable ModelEquivalent modelEquivalent = annotationType.getAnnotation(ModelEquivalent.class);

            if (annotationType.getAnnotation(AnnotationTypePlugin.class) != null) {
                newAnnotationsList.add(new DSLAnnotation(annotation, annotationType));
            } else if (modelEquivalent != null) {
                newAnnotationsList.add(new DSLAnnotation(annotation, modelEquivalent.value()));
            }
        }
        return Collections.unmodifiableList(newAnnotationsList);
    }

    static Serializable getValue(Annotation annotation, Method method) {
        Serializable methodResult;
        try {
            methodResult = (Serializable) method.invoke(annotation);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new DSLException(
                String.format("Cannot convert annotation %s to a CloudKeeper annotation.", annotation),
                exception,
                null
            );
        }

        if (methodResult instanceof Class<?>) {
            methodResult = ((Class<?>) methodResult).getName();
        } else if (methodResult instanceof Class<?>[]) {
            Class<?>[] classArray = (Class<?>[]) methodResult;
            String[] stringArray = new String[classArray.length];
            for (int i = 0; i < classArray.length; ++i) {
                stringArray[i] = classArray[i].getName();
            }
            methodResult = stringArray;
        }
        return methodResult;
    }
}
