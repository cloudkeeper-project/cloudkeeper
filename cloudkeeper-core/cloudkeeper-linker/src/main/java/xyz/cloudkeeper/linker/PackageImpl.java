package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.BarePackage;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.runtime.element.RuntimePackage;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

final class PackageImpl extends AnnotatedConstructImpl implements RuntimePackage, IElementImpl {
    private final Name qualifiedName;
    private final Map<SimpleName, PluginDeclarationImpl> plugins;

    @Nullable private volatile URI bundleIdentifier;

    PackageImpl(BarePackage original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        CopyContext qualifiedNameContext = context.newContextForProperty("qualifiedName");
        qualifiedName = Preconditions.requireNonNull(original.getQualifiedName(), qualifiedNameContext);
        Preconditions.requireCondition(qualifiedName.isPackageName(), qualifiedNameContext,
            "Invalid package name '%s'. Package names must only consist of identifiers that start with a lowercase "
            + "letter.", qualifiedName
        );

        plugins = unmodifiableMapOf(original.getDeclarations(), "declarations", PluginDeclarationImpl::copyOf,
            PluginDeclarationImpl::getSimpleName);
    }

    @Override
    public String toString() {
        return BarePackage.Default.toString(this);
    }

    @Override
    @Nonnull
    public Name getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public ImmutableList<PluginDeclarationImpl> getDeclarations() {
        return ImmutableList.copyOf(plugins.values());
    }

    @Override
    public IElementImpl getEnclosingElement() {
        return null;
    }

    @Override
    public ImmutableList<PluginDeclarationImpl> getEnclosedElements() {
        return getDeclarations();
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        PluginDeclarationImpl declaration = plugins.get(simpleName);
        if (clazz.isInstance(declaration)) {
            @SuppressWarnings("unchecked")
            T typedElement = (T) declaration;
            return typedElement;
        } else {
            return null;
        }

    }

    @Override
    public IElementImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    public URI getBundleIdentifier() {
        require(State.FINISHED);
        @Nullable URI localBundleIdentifier = bundleIdentifier;
        assert localBundleIdentifier != null : "must be non-null when finished";
        return localBundleIdentifier;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.addAll(plugins.values());
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) {
        bundleIdentifier = context.getRequiredEnclosingFreezable(BundleImpl.class).getBundleIdentifier();
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
