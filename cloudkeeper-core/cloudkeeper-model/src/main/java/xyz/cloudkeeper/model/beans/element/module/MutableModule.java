package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareCompositeModule;
import xyz.cloudkeeper.model.bare.element.module.BareInputModule;
import xyz.cloudkeeper.model.bare.element.module.BareLoopModule;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import xyz.cloudkeeper.model.bare.element.module.BareProxyModule;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import java.util.Objects;

import static xyz.cloudkeeper.model.immutable.element.SimpleName.identifier;

@XmlSeeAlso({
    MutableCompositeModule.class, MutableInputModule.class, MutableLoopModule.class, MutableProxyModule.class
})
@XmlType(propOrder = { "simpleName", "declaredAnnotations" })
public abstract class MutableModule<D extends MutableModule<D>>
        extends MutableAnnotatedConstruct<D>
        implements BareModule {
    private static final long serialVersionUID = 1457768754074577080L;

    @Nullable private SimpleName simpleName;

    MutableModule() { }

    MutableModule(BareModule original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        simpleName = original.getSimpleName();
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        // Note that super.equals() performs the verification that otherObject is of the same class
        return super.equals(otherObject)
            && Objects.equals(simpleName, ((MutableModule<?>) otherObject).simpleName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(simpleName);
    }

    @Override
    public abstract String toString();

    private enum CopyVistor implements BareModuleVisitor<MutableModule<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableModule<?> visitInputModule(BareInputModule inputModule, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableInputModule.copyOfInputModule(inputModule, copyOptions);
        }

        @Override
        public MutableModule<?> visitCompositeModule(BareCompositeModule compositeModule,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableCompositeModule.copyOfCompositeModule(compositeModule, copyOptions);
        }

        @Override
        public MutableModule<?> visitLoopModule(BareLoopModule loopModule, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableLoopModule.copyOfLoopModule(loopModule, copyOptions);
        }

        @Override
        public MutableModule<?> visitLinkedModule(BareProxyModule proxyModule, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableProxyModule.copyOfProxyModule(proxyModule, copyOptions);
        }
    }

    @Nullable
    public static MutableModule<?> copyOfModule(@Nullable BareModule original, CopyOption... copyOptions) {
        return original == null
            ? null
            : original.accept(CopyVistor.INSTANCE, copyOptions);
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

    public final D setSimpleName(String simpleName) {
        return setSimpleName(identifier(simpleName));
    }
}
