package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import xyz.cloudkeeper.model.bare.element.module.BareParentInToChildInConnection;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableSimpleNameable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlType(propOrder = { "fromPort", "toModule", "toPort" })
public final class MutableParentInToChildInConnection
        extends MutableConnection<MutableParentInToChildInConnection>
        implements BareParentInToChildInConnection {
    private static final long serialVersionUID = 2876552994767372103L;

    @Nullable private MutableSimpleNameable toModule;

    public MutableParentInToChildInConnection() { }

    private MutableParentInToChildInConnection(BareParentInToChildInConnection original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        toModule = MutableSimpleNameable.copyOf(original.getToModule(), copyOptions);
    }

    @Nullable
    public static MutableParentInToChildInConnection copyOfParentInToChildInConnection(
            @Nullable BareParentInToChildInConnection original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableParentInToChildInConnection(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }
        return Objects.equals(toModule, ((MutableParentInToChildInConnection) otherObject).toModule);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(toModule);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitParentInToChildInConnection(this, parameter);
    }

    @Override
    protected MutableParentInToChildInConnection self() {
        return this;
    }

    @Override
    public String toString() {
        return BareParentInToChildInConnection.Default.toString(this);
    }

    @XmlAttribute(name = "to-module")
    @Override
    @Nullable
    public MutableSimpleNameable getToModule() {
        return toModule;
    }

    public MutableParentInToChildInConnection setToModule(@Nullable MutableSimpleNameable toModule) {
        this.toModule = toModule;
        return this;
    }

    public MutableParentInToChildInConnection setToModule(String toModule) {
        return setToModule(
            new MutableSimpleNameable().setSimpleName(toModule)
        );
    }
}
