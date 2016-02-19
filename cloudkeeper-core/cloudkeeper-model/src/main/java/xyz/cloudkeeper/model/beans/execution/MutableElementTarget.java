package xyz.cloudkeeper.model.beans.execution;

import xyz.cloudkeeper.model.bare.execution.BareElementTarget;
import xyz.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlValue;
import java.util.Objects;

public final class MutableElementTarget
        extends MutableOverrideTarget<MutableElementTarget>
        implements BareElementTarget {
    private static final long serialVersionUID = 2619651886533515638L;

    @Nullable private MutableQualifiedNamable element;

    public MutableElementTarget() { }

    private MutableElementTarget(BareElementTarget original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        element = MutableQualifiedNamable.copyOf(original.getElement(), copyOptions);
    }

    @Nullable
    public static MutableElementTarget copyOfElementTarget(@Nullable BareElementTarget original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableElementTarget(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return Objects.equals(element, ((MutableElementTarget) otherObject).element);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(element);
    }

    @Override
    protected MutableElementTarget self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitElementTarget(this, parameter);
    }

    @XmlValue
    @Override
    @Nullable
    public MutableQualifiedNamable getElement() {
        return element;
    }

    public MutableElementTarget setElement(@Nullable MutableQualifiedNamable element) {
        this.element = element;
        return this;
    }

    public MutableElementTarget setElement(String elementReference) {
        return setElement(
            new MutableQualifiedNamable().setQualifiedName(elementReference)
        );
    }
}
