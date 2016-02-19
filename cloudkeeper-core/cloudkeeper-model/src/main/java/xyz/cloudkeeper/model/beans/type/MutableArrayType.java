package xyz.cloudkeeper.model.beans.type;

import xyz.cloudkeeper.model.bare.type.BareArrayType;
import xyz.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement(name = "array-type")
public final class MutableArrayType extends MutableTypeMirror<MutableArrayType> implements BareArrayType {
    private static final long serialVersionUID = -4431724426769715798L;

    @Nullable private MutableTypeMirror<?> componentType;

    public MutableArrayType() { }

    private MutableArrayType(BareArrayType original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        componentType = MutableTypeMirror.copyOfTypeMirror(original.getComponentType(), copyOptions);
    }

    @Nullable
    public static MutableArrayType copyOfArrayType(@Nullable BareArrayType original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableArrayType(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(componentType, ((MutableArrayType) otherObject).componentType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentType);
    }

    @Override
    public String toString() {
        return BareArrayType.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitArrayType(this, parameter);
    }

    @Override
    protected MutableArrayType self() {
        return this;
    }

    @XmlElementRef
    @Override
    @Nullable
    public MutableTypeMirror<?> getComponentType() {
        return componentType;
    }

    public MutableArrayType setComponentType(@Nullable MutableTypeMirror<?> componentType) {
        this.componentType = componentType;
        return this;
    }
}
