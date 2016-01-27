package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationRoot;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

@XmlRootElement(name = "input-module")
public final class MutableInputModule extends MutableModule<MutableInputModule> implements BareInputModule {
    private static final long serialVersionUID = -1023539886105399311L;

    /**
     * The type of the out-port of this input module.
     *
     * @serial
     */
    @Nullable private MutableTypeMirror<?> outPortType;

    /**
     * The raw data provided by this input module.
     *
     * @serial
     */
    @Nullable private MutableSerializationRoot raw;

    /**
     * Contains original value of this input module.
     *
     * Note that if the value does not implement {@link Serializable}, then this field will be {@code null}
     * after Java deserialization with {@link ObjectInputStream}.
     */
    @Nullable private transient Object value;

    public MutableInputModule() { }

    private MutableInputModule(BareInputModule original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        outPortType = MutableTypeMirror.copyOfTypeMirror(original.getOutPortType(), copyOptions);
        raw = MutableSerializationRoot.copyOfSerializationRoot(original.getRaw(), copyOptions);
        value = original.getValue();
    }

    @Nullable
    public static MutableInputModule copyOfInputModule(@Nullable BareInputModule original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableInputModule(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableInputModule other = (MutableInputModule) otherObject;
        return Objects.equals(outPortType, other.outPortType)
            && Objects.equals(raw, other.raw)
            && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(outPortType, raw, value);
    }

    @Override
    public String toString() {
        return BareInputModule.Default.toString(this);
    }

    /**
     * Serialize this instance.
     *
     * <p>This method is needed because the value may or may not implement {@link Serializable}. In cases it
     * does, the value is written, otherwise it is omitted.
     *
     * @param out output stream
     * @serialData Default fields followed by the content of {@link #value} (if it is a {@link Serializable}
     *     instance) or {@code null} otherwise
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (value instanceof Serializable) {
            Serializable serializable = (Serializable) value;
            out.writeObject(serializable);
        } else {
            out.writeObject(null);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        value = stream.readObject();
    }

    @Override
    protected MutableInputModule self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitInputModule(this, parameter);
    }

    @XmlElementRef
    @Override
    @Nullable
    public MutableTypeMirror<?> getOutPortType() {
        return outPortType;
    }

    public MutableInputModule setOutPortType(@Nullable MutableTypeMirror<?> outPortType) {
        this.outPortType = outPortType;
        return this;
    }

    @XmlElement
    @Override
    @Nullable
    public MutableSerializationRoot getRaw() {
        return raw;
    }

    public MutableInputModule setRaw(@Nullable MutableSerializationRoot raw) {
        this.raw = raw;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * Note that this value is not guaranteed to be serializable. That is, if this instance was created on a different
     * machine, then the returned value will be null if the object's class did not implement {@link Serializable}.
     */
    @XmlTransient
    @Override
    @Nullable
    public Object getValue() {
        return value;
    }

    public MutableInputModule setValue(@Nullable Object value) {
        this.value = value;
        return this;
    }
}
