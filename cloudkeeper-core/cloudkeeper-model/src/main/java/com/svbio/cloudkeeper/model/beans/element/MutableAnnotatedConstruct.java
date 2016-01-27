package com.svbio.cloudkeeper.model.beans.element;

import com.svbio.cloudkeeper.model.bare.element.BareAnnotatedConstruct;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import com.svbio.cloudkeeper.model.beans.AnnotationCopyOption;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotation;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@XmlTransient
public abstract class MutableAnnotatedConstruct<D extends MutableAnnotatedConstruct<D>>
        extends MutableLocatable<D>
        implements BareAnnotatedConstruct {
    private static final long serialVersionUID = -7616227186095133000L;

    private final ArrayList<MutableAnnotation> declaredAnnotations = new ArrayList<>();

    protected MutableAnnotatedConstruct() { }

    protected MutableAnnotatedConstruct(BareAnnotatedConstruct original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        for (BareAnnotation annotation: original.getDeclaredAnnotations()) {
            declaredAnnotations.add(MutableAnnotation.copyOf(annotation, copyOptions));
        }
    }

    /**
     * Copy constructor from a native plug-in type.
     *
     * <p>This constructor copies all annotations that are <em>directly</em> present on the given class, provided that
     * no {@link AnnotationCopyOption} specifies to omit a particular {@link Annotation} instance.
     *
     * <p>Annotations that are only inherited (but not directly present) are <em>not</em> copied. Instead, the
     * assumption is, that inherited assumptions should also be inherited when transformed into the CloudKeeper domain
     * model.
     *
     * @param original native Java annotated element
     */
    protected MutableAnnotatedConstruct(AnnotatedElement original, CopyOption[] copyOptions) {
        List<Annotation> javaAnnotations = Arrays.asList(original.getDeclaredAnnotations());
        for (Annotation annotation: javaAnnotations) {
            if (isCloudKeeperAnnotation(annotation, original, copyOptions)) {
                declaredAnnotations.add(MutableAnnotation.fromAnnotation(annotation, copyOptions));
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        return super.equals(otherObject)
            && Objects.equals(declaredAnnotations, ((MutableAnnotatedConstruct<?>) otherObject).declaredAnnotations);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(declaredAnnotations);
    }

    @XmlElementWrapper(name = "annotations")
    @XmlElement(name = "annotation")
    @Override
    public final List<MutableAnnotation> getDeclaredAnnotations() {
        return declaredAnnotations;
    }

    public final D setDeclaredAnnotations(List<MutableAnnotation> declaredAnnotations) {
        Objects.requireNonNull(declaredAnnotations);
        List<MutableAnnotation> backup = new ArrayList<>(declaredAnnotations);
        this.declaredAnnotations.clear();
        this.declaredAnnotations.addAll(backup);
        return self();
    }

    /**
     * Returns whether the given {@link Annotation} object should be present in the CloudKeeper model.
     *
     * @param annotation annotation instance
     * @param annotatedElement annotation element where {@code annotation} is directly present
     * @param copyOptions copy options
     * @return whether the CloudKeeper plug-in declaration should be treated as a nested declaration
     *
     * @see AnnotationCopyOption#isCloudKeeperAnnotation(Annotation, AnnotatedElement)
     */
    protected static boolean isCloudKeeperAnnotation(Annotation annotation, AnnotatedElement annotatedElement,
            CopyOption[] copyOptions) {
        for (CopyOption copyOption: copyOptions) {
            if (copyOption instanceof AnnotationCopyOption) {
                return ((AnnotationCopyOption) copyOption).isCloudKeeperAnnotation(annotation, annotatedElement);
            }
        }
        return false;
    }
}
