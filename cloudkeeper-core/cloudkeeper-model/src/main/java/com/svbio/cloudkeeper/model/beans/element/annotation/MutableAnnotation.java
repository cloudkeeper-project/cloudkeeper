package com.svbio.cloudkeeper.model.beans.element.annotation;

import com.svbio.cloudkeeper.model.ModelEquivalent;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationEntry;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;
import com.svbio.cloudkeeper.model.immutable.AnnotationValue;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable implementation of the {@link BareAnnotation} interface.
 */
@XmlType(propOrder = { "declaration", "entries" })
public final class MutableAnnotation extends MutableLocatable<MutableAnnotation> implements BareAnnotation {
    private static final long serialVersionUID = -4912725620743409255L;

    @Nullable private MutableQualifiedNamable declaration;
    private final ArrayList<MutableAnnotationEntry> entries = new ArrayList<>();

    public MutableAnnotation() { }

    /**
     * Copy constructor from a native Java annotation.
     *
     * <p>This method will only include entries that have non-default values.
     *
     * @param original native Java annotation
     */
    private MutableAnnotation(Annotation original, CopyOption[] copyOptions) {
        // Note: getClass() cannot be used here because getClass() never returns an interface.
        Class<? extends Annotation> annotationType = original.annotationType();

        @Nullable ModelEquivalent modelEquivalent = annotationType.getAnnotation(ModelEquivalent.class);
        Class<? extends Annotation> modelAnnotationType = modelEquivalent == null
            ? annotationType
            : modelEquivalent.value();

        declaration = new MutableQualifiedNamable().setQualifiedName(nameForClass(modelAnnotationType, copyOptions));

        Method[] declaredMethods = annotationType.getDeclaredMethods();
        for (Method method: declaredMethods) {
            Method modelMethod;
            if (modelAnnotationType == annotationType) {
                modelMethod = method;
            } else {
                try {
                    modelMethod = modelAnnotationType.getMethod(method.getName());
                } catch (NoSuchMethodException exception) {
                    throw new IllegalArgumentException(String.format(
                        "Method %s does not have an equivalent in %s.", method, modelAnnotationType
                    ), exception);
                }
            }

            @Nullable Object nativeDefaultValue = modelMethod.getDefaultValue();
            @Nullable AnnotationValue defaultValue = nativeDefaultValue == null
                ? null
                : AnnotationValue.of(nativeDefaultValue);
            AnnotationValue value = getValue(original, method);

            if (!value.equals(defaultValue)) {
                MutableAnnotationEntry entry = new MutableAnnotationEntry()
                    .setKey(new MutableSimpleNameable().setSimpleName(method.getName()))
                    .setValue(value);
                entries.add(entry);
            }
        }
    }

    public static MutableAnnotation fromAnnotation(Annotation original, CopyOption... copyOptions) {
        return new MutableAnnotation(original, copyOptions);
    }

    /**
     * Returns the value for an entry in an annotation.
     *
     * @param annotation annotation containing the entry
     * @param method method, which specifies the key for the entry
     * @return annotation value; guaranteed to be non-null
     * @throws NullPointerException if the method unexpectedly returns null
     * @throws IllegalArgumentException if the method cannot be invoked or if the return type is not a primitive type,
     *     a String, or an array type of one of the former.
     */
    static AnnotationValue getValue(Annotation annotation, Method method) {
        @Nullable Object methodResult;
        AnnotationValue value;
        try {
            methodResult = method.invoke(annotation);
        } catch (IllegalAccessException|InvocationTargetException exception) {
            throw new IllegalArgumentException(
                String.format("Cannot convert annotation %s to a CloudKeeper annotation.", annotation),
                exception
            );
        }
        if (methodResult == null) {
            throw new NullPointerException(String.format("Annotation method %s unexpectedly returned null.", method));
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

        value = AnnotationValue.of(methodResult);
        return value;
    }

    private MutableAnnotation(BareAnnotation original, CopyOption[] copyOptions) {
        declaration = MutableQualifiedNamable.copyOf(original.getDeclaration());

        for (BareAnnotationEntry element: original.getEntries()) {
            entries.add(MutableAnnotationEntry.copyOf(element, copyOptions));
        }
    }

    @Nullable
    public static MutableAnnotation copyOf(@Nullable BareAnnotation original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableAnnotation(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableAnnotation other = (MutableAnnotation) otherObject;
        return Objects.equals(declaration, other.declaration)
            && Objects.equals(entries, other.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaration, entries);
    }

    @Override
    protected MutableAnnotation self() {
        return this;
    }

    @XmlAttribute(name = "ref")
    @Override
    @Nullable
    public MutableQualifiedNamable getDeclaration() {
        return declaration;
    }

    public MutableAnnotation setDeclaration(@Nullable MutableQualifiedNamable declaration) {
        this.declaration = declaration;
        return this;
    }

    public MutableAnnotation setDeclaration(String declarationName) {
        return setDeclaration(
            new MutableQualifiedNamable().setQualifiedName(declarationName)
        );
    }

    @XmlElement(name = "entries")
    @XmlJavaTypeAdapter(JAXBAdapters.EntriesWrapperAdapter.class)
    @Override
    public List<MutableAnnotationEntry> getEntries() {
        return entries;
    }

    public MutableAnnotation setEntries(List<MutableAnnotationEntry> entries) {
        Objects.requireNonNull(entries);
        List<MutableAnnotationEntry> backup = new ArrayList<>(entries);
        this.entries.clear();
        this.entries.addAll(backup);
        return this;
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }
}
