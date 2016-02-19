package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareIOPort;
import xyz.cloudkeeper.model.bare.element.module.BarePort;
import xyz.cloudkeeper.model.bare.element.module.BarePortVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "io-port")
public final class MutableIOPort extends MutablePort<MutableIOPort> implements BareIOPort {
    private static final long serialVersionUID = -2219670603742619979L;

    public MutableIOPort() { }

    private MutableIOPort(BarePort original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    @Nullable
    public static MutableIOPort copyOfIOPort(@Nullable BarePort original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableIOPort(original, copyOptions);
    }

    @Override
    protected MutableIOPort self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitIOPort(this, parameter);
    }

    @Override
    public String toString() {
        return BareIOPort.Default.toString(this);
    }
}
