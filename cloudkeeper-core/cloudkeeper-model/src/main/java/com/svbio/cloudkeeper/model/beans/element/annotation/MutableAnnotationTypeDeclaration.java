package com.svbio.cloudkeeper.model.beans.element.annotation;

import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable implementation of the {@link BareAnnotationTypeDeclaration} interface.
 */
@XmlRootElement(name = "annotation-declaration")
public final class MutableAnnotationTypeDeclaration
        extends MutablePluginDeclaration<MutableAnnotationTypeDeclaration>
        implements BareAnnotationTypeDeclaration {
    private static final long serialVersionUID = 5650163165247619608L;

    private final ArrayList<MutableAnnotationTypeElement> elements = new ArrayList<>();

    public MutableAnnotationTypeDeclaration() { }

    /**
     * Copy constructor from a native annotation type.
     *
     * <p>This constructor also copies Java annotations. See
     * {@link com.svbio.cloudkeeper.model.beans.element.MutableAnnotatedConstruct#MutableAnnotatedConstruct(java.lang.reflect.AnnotatedElement, CopyOption[])}
     * for details.
     *
     * @param original native Java annotation type
     */
    private MutableAnnotationTypeDeclaration(Class<? extends Annotation> original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        for (Method method: original.getDeclaredMethods()) {
            elements.add(MutableAnnotationTypeElement.fromMethod(method, copyOptions));
        }
    }

    public static MutableAnnotationTypeDeclaration fromClass(Class<? extends Annotation> original,
            CopyOption... copyOptions) {
        return new MutableAnnotationTypeDeclaration(original, copyOptions);
    }

    private MutableAnnotationTypeDeclaration(BareAnnotationTypeDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        for (BareAnnotationTypeElement element: original.getElements()) {
            elements.add(MutableAnnotationTypeElement.copyOf(element, copyOptions));
        }
    }

    @Nullable
    public static MutableAnnotationTypeDeclaration copyOfAnnotationTypeDeclaration(
            @Nullable BareAnnotationTypeDeclaration original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableAnnotationTypeDeclaration(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return Objects.equals(elements, ((MutableAnnotationTypeDeclaration) otherObject).elements);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(elements);
    }

    @Override
    public String toString() {
        return BareAnnotationTypeDeclaration.Default.toString(this);
    }

    @Override
    protected MutableAnnotationTypeDeclaration self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @XmlElementWrapper
    @XmlElement(name = "element")
    @Override
    public List<MutableAnnotationTypeElement> getElements() {
        return elements;
    }

    public MutableAnnotationTypeDeclaration setElements(List<MutableAnnotationTypeElement> elements) {
        Objects.requireNonNull(elements);
        List<MutableAnnotationTypeElement> backup = new ArrayList<>(elements);
        this.elements.clear();
        this.elements.addAll(backup);
        return this;
    }
}
