package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareParentInToChildInConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareShortCircuitConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareSiblingConnection;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Objects;

@XmlTransient
@XmlSeeAlso({
    MutableParentInToChildInConnection.class, MutableSiblingConnection.class,
    MutableChildOutToParentOutConnection.class, MutableShortCircuitConnection.class
})
public abstract class MutableConnection<D extends MutableConnection<D>>
        extends MutableLocatable<D>
        implements BareConnection {
    private static final long serialVersionUID = 1255909758995275255L;

    @Nullable private MutableSimpleNameable fromPort;
    @Nullable private MutableSimpleNameable toPort;

    MutableConnection() { }

    MutableConnection(BareConnection original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        fromPort = MutableSimpleNameable.copyOf(original.getFromPort(), copyOptions);
        toPort = MutableSimpleNameable.copyOf(original.getToPort(), copyOptions);
    }

    private enum CopyVisitor implements BareConnectionVisitor<MutableConnection<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableConnection<?> visitSiblingConnection(BareSiblingConnection connection,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableSiblingConnection.copyOfSiblingConnection(connection, copyOptions);
        }

        @Override
        public MutableConnection<?> visitParentInToChildInConnection(BareParentInToChildInConnection connection,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableParentInToChildInConnection.copyOfParentInToChildInConnection(connection, copyOptions);
        }

        @Override
        public MutableConnection<?> visitChildOutToParentOutConnection(BareChildOutToParentOutConnection connection,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableChildOutToParentOutConnection.copyOfChildOutToParentOutConnection(connection, copyOptions);
        }

        @Override
        public MutableConnection<?> visitShortCircuitConnection(BareShortCircuitConnection connection,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableShortCircuitConnection.copyOfShortCircuitConnection(connection, copyOptions);
        }
    }

    @Nullable
    public static MutableConnection<?> copyOfConnection(@Nullable BareConnection original, CopyOption... copyOptions) {
        return original != null
            ? original.accept(CopyVisitor.INSTANCE, copyOptions)
            : null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableConnection<?> other = (MutableConnection<?>) otherObject;
        return Objects.equals(fromPort, other.fromPort)
            && Objects.equals(toPort, other.toPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPort, toPort);
    }

    @XmlAttribute(name = "from-port")
    @Override
    @Nullable
    public final MutableSimpleNameable getFromPort() {
        return fromPort;
    }

    public final D setFromPort(@Nullable MutableSimpleNameable fromPort) {
        this.fromPort = fromPort;
        return self();
    }

    public final D setFromPort(String fromPort) {
        return setFromPort(
            new MutableSimpleNameable().setSimpleName(fromPort)
        );
    }

    @XmlAttribute(name = "to-port")
    @Override
    @Nullable
    public final MutableSimpleNameable getToPort() {
        return toPort;
    }

    public final D setToPort(@Nullable MutableSimpleNameable toPort) {
        this.toPort = toPort;
        return self();
    }

    public final D setToPort(String toPort) {
        return setToPort(
            new MutableSimpleNameable().setSimpleName(toPort)
        );
    }
}
