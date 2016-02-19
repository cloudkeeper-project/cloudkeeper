package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.bare.element.module.BarePortVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "out-port")
public final class MutableOutPort extends MutablePort<MutableOutPort> implements BareOutPort {
    private static final long serialVersionUID = 6650987360128015357L;

    public MutableOutPort() { }

    private MutableOutPort(BareOutPort original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    @Nullable
    public static MutableOutPort copyOfOutPort(@Nullable BareOutPort original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableOutPort(original, copyOptions);
    }

    @Override
    public String toString() {
        return BareOutPort.Default.toString(this);
    }

    @Override
    protected MutableOutPort self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitOutPort(this, parameter);
    }
}
