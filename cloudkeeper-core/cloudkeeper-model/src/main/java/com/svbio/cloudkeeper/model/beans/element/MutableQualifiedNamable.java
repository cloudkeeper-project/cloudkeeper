package com.svbio.cloudkeeper.model.beans.element;

import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Objects;

@XmlJavaTypeAdapter(JAXBAdapters.MutableQualifiedNamableAdapter.class)
public final class MutableQualifiedNamable
        extends MutableLocatable<MutableQualifiedNamable>
        implements BareQualifiedNameable {
    private static final long serialVersionUID = 2066520784889935187L;

    @Nullable private Name qualifiedName;

    public MutableQualifiedNamable() { }

    private MutableQualifiedNamable(BareQualifiedNameable original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        qualifiedName = original.getQualifiedName();
    }

    /**
     * Returns a copy of the given instance, or {@code null} if the given instance is {@code null}.
     */
    @Nullable
    public static MutableQualifiedNamable copyOf(@Nullable BareQualifiedNameable original, CopyOption... copyOptions) {
        return original != null
            ? new MutableQualifiedNamable(original, copyOptions)
            : null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return Objects.equals(qualifiedName, ((MutableQualifiedNamable) otherObject).qualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(qualifiedName);
    }

    @Override
    public final String toString() {
        return Default.toString(this);
    }

    @Override
    protected MutableQualifiedNamable self() {
        return this;
    }

    @Override
    @Nullable
    public Name getQualifiedName() {
        return qualifiedName;
    }

    public MutableQualifiedNamable setQualifiedName(@Nullable Name qualifiedName) {
        this.qualifiedName = qualifiedName;
        return this;
    }

    public MutableQualifiedNamable setQualifiedName(String qualifiedName) {
        return setQualifiedName(Name.qualifiedName(qualifiedName));
    }
}
