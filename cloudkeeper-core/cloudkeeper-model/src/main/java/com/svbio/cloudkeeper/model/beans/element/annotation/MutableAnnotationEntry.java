package com.svbio.cloudkeeper.model.beans.element.annotation;

import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationEntry;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationValue;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;
import com.svbio.cloudkeeper.model.immutable.AnnotationValue;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Objects;

/**
 * Mutable implementation of the {@link BareAnnotationEntry} interface.
 */
@XmlType(propOrder = { "key", "value" })
public final class MutableAnnotationEntry
        extends MutableLocatable<MutableAnnotationEntry>
        implements BareAnnotationEntry {
    private static final long serialVersionUID = -1904359687069574335L;

    @Nullable private MutableSimpleNameable key;
    @Nullable private AnnotationValue value;

    public MutableAnnotationEntry() { }

    private MutableAnnotationEntry(BareAnnotationEntry original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        key = MutableSimpleNameable.copyOf(original.getKey(), copyOptions);
        @Nullable BareAnnotationValue originalValue = original.getValue();
        value = originalValue == null
            ? null
            : AnnotationValue.copyOf(originalValue);
    }

    @Nullable
    public static MutableAnnotationEntry copyOf(@Nullable BareAnnotationEntry original, CopyOption... copyOptions) {
        return original != null
            ? new MutableAnnotationEntry(original, copyOptions)
            : null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableAnnotationEntry other = (MutableAnnotationEntry) otherObject;
        return Objects.equals(key, other.key)
            && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    protected MutableAnnotationEntry self() {
        return this;
    }

    @XmlAttribute(name = "ref")
    @Override
    @Nullable
    public MutableSimpleNameable getKey() {
        return key;
    }

    public MutableAnnotationEntry setKey(@Nullable MutableSimpleNameable key) {
        this.key = key;
        return this;
    }

    public MutableAnnotationEntry setKey(String key) {
        return setKey(
            new MutableSimpleNameable().setSimpleName(key)
        );
    }

    @XmlElement
    @Override
    @Nullable
    public AnnotationValue getValue() {
        return value;
    }

    public MutableAnnotationEntry setValue(@Nullable AnnotationValue value) {
        this.value = value;
        return this;
    }

    public MutableAnnotationEntry setValue(Serializable value) {
        return setValue(AnnotationValue.of(value));
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }
}
