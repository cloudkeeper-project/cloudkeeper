package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclaration;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Abstract module declaration, containing static information about a module, such as the name (identifier), in- and
 * out-port types and other aspects.
 *
 * Only simple and composite modules can be declared. Therefore, the only two subclasses are
 * {@link SimpleModuleDeclarationImpl} and {@link CompositeModuleDeclarationImpl}.
 */
abstract class ModuleDeclarationImpl
        extends PluginDeclarationImpl
        implements RuntimeModuleDeclaration, IPortContainerImpl {
    ModuleDeclarationImpl(BareModuleDeclaration original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
    }

    private enum CopyVisitor
            implements BareModuleDeclarationVisitor<Try<? extends ModuleDeclarationImpl>, CopyContext> {
        INSTANCE;

        @Override
        @Nullable
        public Try<CompositeModuleDeclarationImpl> visit(BareCompositeModuleDeclaration declaration,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new CompositeModuleDeclarationImpl(declaration, parentContext));
        }

        @Override
        @Nullable
        public Try<SimpleModuleDeclarationImpl> visit(BareSimpleModuleDeclaration declaration,
                @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new SimpleModuleDeclarationImpl(declaration, parentContext));
        }
    }

    static ModuleDeclarationImpl copyOf(BareModuleDeclaration original, CopyContext parentContext)
            throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        @Nullable Try<? extends ModuleDeclarationImpl> copyTry = original.accept(CopyVisitor.INSTANCE, parentContext);
        assert copyTry != null;
        return copyTry.get();
    }

    @Override
    @Nullable
    public final <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    @Nullable
    public final <T, P> T accept(RuntimePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public final IElementImpl getEnclosingElement() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Since there is currently no support for inheritance of module declarations, annotations can likewise not be
     * inherited.
     */
    @Override
    @Nullable
    public ModuleDeclarationImpl getSuperAnnotatedConstruct() {
        return null;
    }

    /**
     * Returns the list of ports, similar to {@link #getPorts()} but with weaker return type.
     *
     * <p>This method may be called (and its return value be used) safely even before the module declaration is fully
     * constructed; that is, before the state is {@link State#PRECOMPUTED}. Once this instance is frozen, this method
     * will return the same reference as {@code #getPorts()}.
     *
     * <p>Note that this method can be called also if the state is {@link State#PRECOMPUTED}: For instance, when a new
     * proxy module is linked against a fully linked and verified repository.
     *
     * <p>Since this method is a package-private method, no precaution is taken to prevent exposing internal mutable
     * data structures. Callers are expected not to modify the returned list.
     *
     * @see ProxyModuleImpl#preProcessFreezable(FinishContext)
     */
    abstract List<? extends BarePort> getBarePorts();

    @Override
    final void verifyFreezable(VerifyContext context) { }
}
