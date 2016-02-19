package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclaration;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

abstract class PluginDeclarationImpl extends AnnotatedConstructImpl implements RuntimePluginDeclaration, IElementImpl {
    private final SimpleName simpleName;
    @Nullable private volatile Name qualifiedName;
    @Nullable private volatile PackageImpl enclosingPackage;

    PluginDeclarationImpl(BarePluginDeclaration original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        simpleName = Preconditions.requireNonNull(
            original.getSimpleName(), getCopyContext().newContextForProperty("simpleName"));
    }

    private enum CopyVisitor
            implements BarePluginDeclarationVisitor<Try<? extends PluginDeclarationImpl>, CopyContext> {
        INSTANCE;

        @Override
        public Try<ModuleDeclarationImpl> visit(BareModuleDeclaration original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> ModuleDeclarationImpl.copyOf(original, parentContext));
        }

        @Override
        public Try<TypeDeclarationImpl> visit(BareTypeDeclaration original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new TypeDeclarationImpl(original, parentContext));
        }

        @Override
        public Try<AnnotationTypeDeclarationImpl> visit(BareAnnotationTypeDeclaration original,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new AnnotationTypeDeclarationImpl(original, parentContext));
        }

        @Override
        public Try<SerializationDeclarationImpl> visit(BareSerializationDeclaration declaration,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new SerializationDeclarationImpl(declaration, parentContext));
        }
    }

    public static PluginDeclarationImpl copyOf(BarePluginDeclaration original, CopyContext parentContext)
            throws LinkerException {
        @Nullable Try<? extends PluginDeclarationImpl> copyTry = original.accept(CopyVisitor.INSTANCE, parentContext);
        assert copyTry != null;
        return copyTry.get();
    }

    @Override
    public abstract String toString();

    @Override
    @Nullable
    public abstract PluginDeclarationImpl getSuperAnnotatedConstruct();

    @Override
    @Nonnull
    public final SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public final Name getQualifiedName() {
        require(State.LINKED);
        @Nullable Name localQualifiedName = qualifiedName;
        assert localQualifiedName != null : "must be non-null when in state " + State.LINKED;
        return localQualifiedName;
    }

    @Override
    public PackageImpl getPackage() {
        require(State.LINKED);
        @Nullable PackageImpl localEnclosingPackage = enclosingPackage;
        assert localEnclosingPackage != null : "must be non-null when in state " + State.LINKED;
        return localEnclosingPackage;
    }

    @Override
    final void preProcessFreezable(FinishContext context) {
        IElementImpl enclosingElement = context.getRequiredEnclosingFreezable(IElementImpl.class);
        qualifiedName = enclosingElement.getQualifiedName().join(simpleName);
        enclosingPackage = context.getRequiredEnclosingFreezable(PackageImpl.class);
    }
}
