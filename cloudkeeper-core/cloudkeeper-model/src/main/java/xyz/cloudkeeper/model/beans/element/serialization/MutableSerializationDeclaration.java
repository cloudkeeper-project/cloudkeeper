package xyz.cloudkeeper.model.beans.element.serialization;

import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutablePluginDeclaration;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "serialization-declaration")
public final class MutableSerializationDeclaration
        extends MutablePluginDeclaration<MutableSerializationDeclaration>
        implements BareSerializationDeclaration {
    private static final long serialVersionUID = -1714949848006774809L;

    public MutableSerializationDeclaration() { }

    private MutableSerializationDeclaration(Class<? extends Marshaler<?>> original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    private MutableSerializationDeclaration(BareSerializationDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    /**
     * Factory method to construct a serialization declaration from a {@link Marshaler} class object.
     *
     * <p>This constructor also copies Java annotations. See
     * {@link xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct} for details.
     *
     * @param original {@link Marshaler} class object
     */
    public static MutableSerializationDeclaration fromClass(Class<? extends Marshaler<?>> original,
            CopyOption... copyOptions) {
        return new MutableSerializationDeclaration(original, copyOptions);
    }

    @Nullable
    public static MutableSerializationDeclaration copyOfSerializationDeclaration(
            @Nullable BareSerializationDeclaration original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableSerializationDeclaration(original, copyOptions);
    }

    @Override
    public String toString() {
        return BareSerializationDeclaration.Default.toString(this);
    }

    @Override
    protected MutableSerializationDeclaration self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }
}
