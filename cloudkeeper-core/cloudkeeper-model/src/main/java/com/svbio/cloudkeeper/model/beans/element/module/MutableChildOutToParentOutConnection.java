package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType(propOrder = { "fromModule", "fromPort", "toPort" })
public final class MutableChildOutToParentOutConnection
        extends MutableConnection<MutableChildOutToParentOutConnection>
        implements BareChildOutToParentOutConnection {
    private static final long serialVersionUID = -6706734949700672806L;

    @Nullable private MutableSimpleNameable fromModule;

    public MutableChildOutToParentOutConnection() { }

    private MutableChildOutToParentOutConnection(BareChildOutToParentOutConnection original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        fromModule = MutableSimpleNameable.copyOf(original.getFromModule(), copyOptions);
    }

    @Nullable
    public static MutableChildOutToParentOutConnection copyOfChildOutToParentOutConnection(
            @Nullable BareChildOutToParentOutConnection original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableChildOutToParentOutConnection(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(fromModule, ((MutableChildOutToParentOutConnection) otherObject).fromModule);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(fromModule);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitChildOutToParentOutConnection(this, parameter);
    }

    @Override
    protected MutableChildOutToParentOutConnection self() {
        return this;
    }

    @Override
    public String toString() {
        return BareChildOutToParentOutConnection.Default.toString(this);
    }

    @XmlAttribute(name = "from-module")
    @Override
    @Nullable
    public MutableSimpleNameable getFromModule() {
        return fromModule;
    }

    public MutableChildOutToParentOutConnection setFromModule(@Nullable MutableSimpleNameable fromModule) {
        this.fromModule = fromModule;
        return this;
    }

    public MutableChildOutToParentOutConnection setFromModule(String fromModuleName) {
        return setFromModule(
            new MutableSimpleNameable().setSimpleName(fromModuleName)
        );
    }
}
