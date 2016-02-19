package xyz.cloudkeeper.model.beans.element;

import xyz.cloudkeeper.model.bare.element.BarePluginDeclaration;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.beans.element.module.MutableModuleDeclaration;
import xyz.cloudkeeper.model.beans.element.serialization.MutableSerializationDeclaration;
import xyz.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

@XmlSeeAlso({
    MutableModuleDeclaration.class, MutableTypeDeclaration.class, MutableAnnotationTypeDeclaration.class,
    MutableSerializationDeclaration.class
})
@XmlType(propOrder = { "simpleName", "declaredAnnotations" })
public abstract class MutablePluginDeclaration<D extends MutablePluginDeclaration<D>>
        extends MutableAnnotatedConstruct<D>
        implements BarePluginDeclaration {
    private static final long serialVersionUID = 8956812467838414153L;

    @Nullable private SimpleName simpleName;

    protected MutablePluginDeclaration() { }

    protected MutablePluginDeclaration(BarePluginDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = original.getSimpleName();
    }

    protected MutablePluginDeclaration(Class<?> original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = nameForClass(original, copyOptions).toSimpleName();
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        // Note that super.equals() performs the verification that otherObject is of the same class
        return super.equals(otherObject)
            && Objects.equals(simpleName, ((MutablePluginDeclaration<?>) otherObject).simpleName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(simpleName);
    }

    @Override
    public abstract String toString();

    private enum CopyVisitor implements BarePluginDeclarationVisitor<MutablePluginDeclaration<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableModuleDeclaration<?> visit(BareModuleDeclaration declaration,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableModuleDeclaration.copyOfModuleDeclaration(declaration, copyOptions);
        }

        @Override
        public MutablePluginDeclaration<?> visit(BareTypeDeclaration declaration, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableTypeDeclaration.copyOfTypeDeclaration(declaration, copyOptions);
        }

        @Override
        public MutablePluginDeclaration<?> visit(BareAnnotationTypeDeclaration declaration,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableAnnotationTypeDeclaration.copyOfAnnotationTypeDeclaration(declaration, copyOptions);
        }

        @Override
        public MutablePluginDeclaration<?> visit(BareSerializationDeclaration declaration,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableSerializationDeclaration.copyOfSerializationDeclaration(declaration, copyOptions);
        }
    }

    @Nullable
    public static MutablePluginDeclaration<?> copyOfPluginDeclaration(@Nullable BarePluginDeclaration original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : original.accept(CopyVisitor.INSTANCE, copyOptions);
    }

    @XmlElement(name = "simple-name")
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
        return setSimpleName(SimpleName.identifier(name));
    }
}
