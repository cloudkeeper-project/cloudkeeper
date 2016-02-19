package xyz.cloudkeeper.model.beans.element.annotation;

import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationValue;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.immutable.AnnotationValue;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Mutable Java Bean-style implementation of the
 * {@link BareAnnotationTypeElement} interface.
 */
@XmlType(propOrder = { "simpleName", "declaredAnnotations", "returnType", "defaultValue" })
public final class MutableAnnotationTypeElement
        extends MutableAnnotatedConstruct<MutableAnnotationTypeElement>
        implements BareAnnotationTypeElement {
    private static final long serialVersionUID = -5787467436181603708L;

    @Nullable private SimpleName simpleName;
    @Nullable private MutableTypeMirror<?> returnType;
    @Nullable private AnnotationValue defaultValue;

    public MutableAnnotationTypeElement() { }

    private MutableAnnotationTypeElement(BareAnnotationTypeElement original, CopyOption[] copyOptions) {
        simpleName = original.getSimpleName();
        returnType = MutableTypeMirror.copyOfTypeMirror(original.getReturnType(), copyOptions);
        @Nullable BareAnnotationValue originalDefaultValue = original.getDefaultValue();
        defaultValue =  originalDefaultValue == null
            ? null
            : AnnotationValue.copyOf(originalDefaultValue);
    }

    @Nullable
    public static MutableAnnotationTypeElement copyOf(@Nullable BareAnnotationTypeElement original,
            CopyOption... copyOptions) {
        return original != null
            ? new MutableAnnotationTypeElement(original, copyOptions)
            : null;
    }

    /**
     * Copy constructor from native Java methods of annotation types.
     *
     * <p>This constructor also copies Java annotations. See
     * {@link MutableAnnotatedConstruct#MutableAnnotatedConstruct(java.lang.reflect.AnnotatedElement, CopyOption[])}
     * for details.
     *
     * @param original native Java annotation type
     */
    private MutableAnnotationTypeElement(Method original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = SimpleName.identifier(original.getName());
        returnType = MutableTypeMirror.fromJavaType(original.getReturnType(), copyOptions);
        @Nullable Object originalNativeDefaultValue = original.getDefaultValue();
        defaultValue = originalNativeDefaultValue == null
            ? null
            : AnnotationValue.of(originalNativeDefaultValue);
    }

    public static MutableAnnotationTypeElement fromMethod(Method original, CopyOption[] copyOptions) {
        return new MutableAnnotationTypeElement(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableAnnotationTypeElement other = (MutableAnnotationTypeElement) otherObject;
        return Objects.equals(simpleName, other.simpleName)
            && Objects.equals(returnType, other.returnType)
            && Objects.equals(defaultValue, other.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simpleName, returnType, defaultValue);
    }

    @Override
    protected MutableAnnotationTypeElement self() {
        return this;
    }

    @Override
    public String toString() {
        return Default.toHumanReadableString(this);
    }

    @XmlElement(name = "simple-name")
    @Override
    @Nullable
    public SimpleName getSimpleName() {
        return simpleName;
    }

    public MutableAnnotationTypeElement setSimpleName(@Nullable SimpleName simpleName) {
        this.simpleName = simpleName;
        return this;
    }

    public MutableAnnotationTypeElement setSimpleName(String simpleName) {
        return setSimpleName(SimpleName.identifier(simpleName));
    }

    @XmlElementRef
    @Override
    @Nullable
    public MutableTypeMirror<?> getReturnType() {
        return returnType;
    }

    public MutableAnnotationTypeElement setReturnType(@Nullable MutableTypeMirror<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    @XmlElement(name = "default-value")
    @Override
    @Nullable
    public AnnotationValue getDefaultValue() {
        return defaultValue;
    }

    public MutableAnnotationTypeElement setDefaultValue(@Nullable AnnotationValue defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public MutableAnnotationTypeElement setDefaultValue(Serializable defaultValue) {
        return setDefaultValue(AnnotationValue.of(defaultValue));
    }
}
