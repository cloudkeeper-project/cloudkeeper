package xyz.cloudkeeper.model.beans.element.serialization;

import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNode;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNodeVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlJavaTypeAdapter(JAXBAdapters.SerializationRootAdapter.class)
public final class MutableSerializationRoot
        extends MutableSerializationNode<MutableSerializationRoot>
        implements BareSerializationRoot {
    private static final long serialVersionUID = 9179591723614263788L;

    @Nullable private MutableQualifiedNamable declaration;
    private final ArrayList<MutableSerializationNode<?>> entries = new ArrayList<>();

    public MutableSerializationRoot() { }

    private MutableSerializationRoot(BareSerializationRoot original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        declaration = MutableQualifiedNamable.copyOf(original.getDeclaration(), copyOptions);

        for (BareSerializationNode node: original.getEntries()) {
            entries.add(copyOfSerializationNode(node, copyOptions));
        }
    }

    @Nullable
    public static MutableSerializationRoot copyOfSerializationRoot(@Nullable BareSerializationRoot original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableSerializationRoot(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableSerializationRoot other = (MutableSerializationRoot) otherObject;
        return Objects.equals(declaration, other.declaration)
            && entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(declaration, entries);
    }

    @Override
    protected MutableSerializationRoot self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareSerializationNodeVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitRoot(this, parameter);
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    @Nullable
    public MutableQualifiedNamable getDeclaration() {
        return declaration;
    }

    public MutableSerializationRoot setDeclaration(@Nullable MutableQualifiedNamable declaration) {
        this.declaration = declaration;
        return this;
    }

    public MutableSerializationRoot setDeclaration(String serializationName) {
        return setDeclaration(
            new MutableQualifiedNamable().setQualifiedName(Name.qualifiedName(serializationName))
        );
    }

    @Override
    public List<MutableSerializationNode<?>> getEntries() {
        return entries;
    }

    public MutableSerializationRoot setEntries(List<MutableSerializationNode<?>> entries) {
        Objects.requireNonNull(entries);
        if (this.entries != entries) {
            this.entries.clear();
            this.entries.addAll(entries);
        }
        return this;
    }
}
