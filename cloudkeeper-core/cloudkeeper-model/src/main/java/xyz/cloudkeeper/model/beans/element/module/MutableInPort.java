package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.bare.element.module.BarePortVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "in-port")
public final class MutableInPort extends MutablePort<MutableInPort> implements BareInPort {
    private static final long serialVersionUID = 9192499359344519261L;

    public MutableInPort() { }

    private MutableInPort(BareInPort original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    @Nullable
    public static MutableInPort copyOfInPort(@Nullable BareInPort original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableInPort(original, copyOptions);
    }

    @Override
    protected MutableInPort self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitInPort(this, parameter);
    }

    @Override
    public String toString() {
        return BareInPort.Default.toString(this);
    }
}
