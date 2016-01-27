package com.svbio.cloudkeeper.model.beans.type;

import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.bare.type.BareTypeVariable;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

@XmlRootElement(name = "type-variable")
public final class MutableTypeVariable extends MutableTypeMirror<MutableTypeVariable> implements BareTypeVariable {
    private static final long serialVersionUID = 1568772900659318345L;

    @Nullable private MutableSimpleNameable formalTypeParameter;

    public MutableTypeVariable() { }

    private MutableTypeVariable(BareTypeVariable original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        formalTypeParameter = MutableSimpleNameable.copyOf(original.getFormalTypeParameter(), copyOptions);
    }

    @Nullable
    public static MutableTypeVariable copyOfTypeVariable(@Nullable BareTypeVariable original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableTypeVariable(original, copyOptions);
    }

    private MutableTypeVariable(TypeVariable<?> typeVariable, CopyOption[] copyOptions) {
        formalTypeParameter = new MutableSimpleNameable()
            .setSimpleName(typeVariable.getName());
    }

    public static MutableTypeVariable fromTypeVariable(TypeVariable<?> typeVariable, CopyOption... copyOptions) {
        return new MutableTypeVariable(typeVariable, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(formalTypeParameter, ((MutableTypeVariable) otherObject).formalTypeParameter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(formalTypeParameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitTypeVariable(this, parameter);
    }

    @Override
    protected MutableTypeVariable self() {
        return this;
    }

    @XmlAttribute(name = "ref")
    @Override
    @Nullable
    public MutableSimpleNameable getFormalTypeParameter() {
        return formalTypeParameter;
    }

    public MutableTypeVariable setFormalTypeParameter(@Nullable MutableSimpleNameable formalTypeParameter) {
        this.formalTypeParameter = formalTypeParameter;
        return this;
    }

    public MutableTypeVariable setFormalTypeParameter(String formalTypeParameter) {
        return setFormalTypeParameter(
            new MutableSimpleNameable().setSimpleName(formalTypeParameter)
        );
    }

    @Override
    public String toString() {
        return BareTypeVariable.Default.toString(this);
    }
}
