package com.svbio.cloudkeeper.model.beans.type;

import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.bare.type.BareWildcardType;
import com.svbio.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Objects;

@XmlRootElement(name = "wildcard-type")
@XmlType(propOrder = {"extendsBound", "superBound"})
public final class MutableWildcardType extends MutableTypeMirror<MutableWildcardType> implements BareWildcardType {
    private static final long serialVersionUID = 4524137927613777641L;

    @Nullable private MutableTypeMirror<?> extendsBound;
    @Nullable private MutableTypeMirror<?> superBound;

    public MutableWildcardType() { }

    private MutableWildcardType(WildcardType original, CopyOption[] copyOptions) {
        Type[] originalUpperBounds = original.getUpperBounds();
        extendsBound = originalUpperBounds.length > 0
            ? fromJavaType(originalUpperBounds[0], copyOptions)
            : null;

        Type[] originalLowerBounds = original.getLowerBounds();
        superBound = originalLowerBounds.length > 0
            ? fromJavaType(originalLowerBounds[0], copyOptions)
            : null;
    }

    public static MutableWildcardType fromWildcardType(WildcardType original, CopyOption... copyOptions) {
        return new MutableWildcardType(original, copyOptions);
    }

    private MutableWildcardType(BareWildcardType original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        extendsBound = copyOfTypeMirror(original.getExtendsBound(), copyOptions);
        superBound = copyOfTypeMirror(original.getSuperBound(), copyOptions);
    }

    @Nullable
    public static MutableWildcardType copyOfWildcardType(@Nullable BareWildcardType original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableWildcardType(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableWildcardType other = (MutableWildcardType) otherObject;
        return Objects.equals(extendsBound, other.extendsBound)
            && Objects.equals(superBound, other.superBound);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(extendsBound, superBound);
    }

    @Override
    protected MutableWildcardType self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitWildcardType(this, parameter);
    }

    @XmlJavaTypeAdapter(JAXBAdapters.TypeMirrorWrapperAdapter.class)
    @XmlElement(name = "extends-bound")
    @Override
    @Nullable
    public MutableTypeMirror<?> getExtendsBound() {
        return extendsBound;
    }

    public MutableWildcardType setExtendsBound(@Nullable MutableTypeMirror<?> extendsBound) {
        this.extendsBound = extendsBound;
        return this;
    }

    @XmlJavaTypeAdapter(JAXBAdapters.TypeMirrorWrapperAdapter.class)
    @XmlElement(name = "super-bound")
    @Override
    @Nullable
    public MutableTypeMirror<?> getSuperBound() {
        return superBound;
    }

    public MutableWildcardType setSuperBound(@Nullable MutableTypeMirror<?> superBound) {
        this.superBound = superBound;
        return this;
    }

    @Override
    public String toString() {
        return BareWildcardType.Default.toString(this);
    }
}
