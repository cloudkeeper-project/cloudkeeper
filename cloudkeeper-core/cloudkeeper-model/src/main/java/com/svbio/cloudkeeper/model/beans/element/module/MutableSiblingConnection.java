package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareSiblingConnection;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType(propOrder = { "fromModule", "fromPort", "toModule", "toPort" })
public final class MutableSiblingConnection
        extends MutableConnection<MutableSiblingConnection>
        implements BareSiblingConnection {
    private static final long serialVersionUID = 277094996279777689L;

    @Nullable private MutableSimpleNameable fromModule;
    @Nullable private MutableSimpleNameable toModule;

    public MutableSiblingConnection() { }

    private MutableSiblingConnection(BareSiblingConnection original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        fromModule = MutableSimpleNameable.copyOf(original.getFromModule(), copyOptions);
        toModule = MutableSimpleNameable.copyOf(original.getToModule(), copyOptions);
    }

    @Nullable
    public static MutableSiblingConnection copyOfSiblingConnection(@Nullable BareSiblingConnection original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableSiblingConnection(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableSiblingConnection other = (MutableSiblingConnection) otherObject;
        return Objects.equals(fromModule, other.fromModule)
            && Objects.equals(toModule, other.toModule);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(fromModule, toModule);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitSiblingConnection(this, parameter);
    }

    @Override
    protected MutableSiblingConnection self() {
        return this;
    }

    @Override
    public String toString() {
        return BareSiblingConnection.Default.toString(this);
    }

    @XmlAttribute(name = "from-module")
    @Override
    @Nullable
    public MutableSimpleNameable getFromModule() {
        return fromModule;
    }

    public MutableSiblingConnection setFromModule(@Nullable MutableSimpleNameable fromModule) {
        this.fromModule = fromModule;
        return this;
    }

    public MutableSiblingConnection setFromModule(String fromModuleName) {
        return setFromModule(
            new MutableSimpleNameable().setSimpleName(fromModuleName)
        );
    }

    @XmlAttribute(name = "to-module")
    @Override
    @Nullable
    public MutableSimpleNameable getToModule() {
        return toModule;
    }

    public MutableSiblingConnection setToModule(@Nullable MutableSimpleNameable toModule) {
        this.toModule = toModule;
        return this;
    }

    public MutableSiblingConnection setToModule(String toModule) {
        return setToModule(
            new MutableSimpleNameable().setSimpleName(toModule)
        );
    }
}
