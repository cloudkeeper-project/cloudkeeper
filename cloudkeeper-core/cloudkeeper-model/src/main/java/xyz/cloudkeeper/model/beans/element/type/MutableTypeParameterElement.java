package xyz.cloudkeeper.model.beans.element.type;

import xyz.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@XmlType(propOrder = { "simpleName", "declaredAnnotations", "bounds" })
public final class MutableTypeParameterElement
        extends MutableAnnotatedConstruct<MutableTypeParameterElement>
        implements BareTypeParameterElement {
    private static final long serialVersionUID = -5626977711092638124L;

    @Nullable private SimpleName simpleName;
    private final ArrayList<MutableTypeMirror<?>> bounds = new ArrayList<>();

    public MutableTypeParameterElement() { }

    private MutableTypeParameterElement(TypeVariable<?> original, CopyOption[] copyOptions) {
        simpleName = SimpleName.identifier(original.getName());

        List<Type> originalBounds = Arrays.asList(original.getBounds());
        if (originalBounds.size() == 1 && Object.class.equals(originalBounds.get(0))) {
            originalBounds = Collections.emptyList();
        }
        for (Type bound: originalBounds) {
            bounds.add(MutableTypeMirror.fromJavaType(bound, copyOptions));
        }
    }

    public MutableTypeParameterElement(BareTypeParameterElement original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = original.getSimpleName();

        for (BareTypeMirror bound: original.getBounds()) {
            bounds.add(MutableTypeMirror.copyOfTypeMirror(bound, copyOptions));
        }
    }

    public static MutableTypeParameterElement fromTypeVariable(TypeVariable<?> original, CopyOption... copyOptions) {
        return new MutableTypeParameterElement(original, copyOptions);
    }

    @Nullable
    public static MutableTypeParameterElement copyOf(@Nullable BareTypeParameterElement original,
            CopyOption... copyOptions) {
        return original != null
            ? new MutableTypeParameterElement(original, copyOptions)
            : null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableTypeParameterElement other = (MutableTypeParameterElement) otherObject;
        return Objects.equals(simpleName, other.simpleName)
            && Objects.equals(bounds, other.bounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simpleName, bounds);
    }

    @Override
    public String toString() {
        return Default.toHumanReadableString(this);
    }

    @Override
    protected MutableTypeParameterElement self() {
        return this;
    }

    @XmlElement(name = "simple-name")
    @Override
    @Nullable
    public SimpleName getSimpleName() {
        return simpleName;
    }

    public MutableTypeParameterElement setSimpleName(@Nullable SimpleName simpleName) {
        this.simpleName = simpleName;
        return this;
    }

    public MutableTypeParameterElement setSimpleName(String simpleName) {
        return setSimpleName(SimpleName.identifier(simpleName));
    }

    @XmlElementWrapper(name = "bounds")
    @XmlElementRef
    @Override
    public List<MutableTypeMirror<?>> getBounds() {
        return bounds;
    }

    public MutableTypeParameterElement setBounds(List<MutableTypeMirror<?>> bounds) {
        Objects.requireNonNull(bounds);
        List<MutableTypeMirror<?>> backup = new ArrayList<>(bounds);
        this.bounds.clear();
        this.bounds.addAll(backup);
        return this;
    }
}
