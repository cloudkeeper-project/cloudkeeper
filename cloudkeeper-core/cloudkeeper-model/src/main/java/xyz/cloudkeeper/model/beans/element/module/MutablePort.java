package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareIOPort;
import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.bare.element.module.BarePort;
import xyz.cloudkeeper.model.bare.element.module.BarePortVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

import static xyz.cloudkeeper.model.immutable.element.SimpleName.identifier;

@XmlSeeAlso({ MutableInPort.class, MutableIOPort.class, MutableOutPort.class })
@XmlType(propOrder = { "simpleName", "declaredAnnotations", "type" })
public abstract class MutablePort<D extends MutablePort<D>>
    extends MutableAnnotatedConstruct<D>
    implements BarePort {

    private static final long serialVersionUID = 4515651881426980303L;

    @Nullable private SimpleName simpleName;
    @Nullable private MutableTypeMirror<?> type;

    MutablePort() { }

    MutablePort(BarePort original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = original.getSimpleName();
        type = MutableTypeMirror.copyOfTypeMirror(original.getType(), copyOptions);
    }

    private enum CopyVisitor implements BarePortVisitor<MutablePort<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableInPort visitInPort(BareInPort inPort, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableInPort.copyOfInPort(inPort, copyOptions);
        }

        @Override
        public MutablePort<?> visitOutPort(BareOutPort outPort, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableOutPort.copyOfOutPort(outPort, copyOptions);
        }

        @Override
        public MutablePort<?> visitIOPort(BareIOPort ioPort, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableIOPort.copyOfIOPort(ioPort, copyOptions);
        }
    }

    @Nullable
    public static MutablePort<?> copyOfPort(@Nullable BarePort port, CopyOption... copyOptions) {
        return port == null
            ? null
            : port.accept(CopyVisitor.INSTANCE, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (!super.equals(otherObject)) {
            return false;
        }

        MutablePort<?> other = (MutablePort<?>) otherObject;
        return Objects.equals(simpleName, other.simpleName)
            && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(simpleName, type);
    }

    @Override
    public abstract String toString();

    @XmlElement(name = "name")
    @Override
    @Nullable
    public final SimpleName getSimpleName() {
        return simpleName;
    }

    public final D setSimpleName(@Nullable SimpleName simpleName) {
        this.simpleName = simpleName;
        return self();
    }

    public final D setSimpleName(String name) {
        return setSimpleName(identifier(name));
    }

    @XmlElementRef
    @Override
    @Nullable
    public final MutableTypeMirror<?> getType() {
        return type;
    }

    public final D setType(@Nullable MutableTypeMirror<?> type) {
        this.type = type;
        return self();
    }
}
