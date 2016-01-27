package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleVisitor;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class ProxyModuleImpl extends ModuleImpl implements RuntimeProxyModule {
    private final NameReference declarationReference;

    @Nullable private volatile ModuleDeclarationImpl moduleDeclaration;
    @Nullable private volatile Map<SimpleName, PortImpl> declaredPortsMap;
    @Nullable private volatile ImmutableList<PortImpl> declaredPorts;
    @Nullable private volatile ImmutableList<IInPortImpl> inPorts;
    @Nullable private volatile ImmutableList<IOutPortImpl> outPorts;

    ProxyModuleImpl(BareProxyModule original, CopyContext parentContext, int index) throws LinkerException {
        super(original, parentContext, index);
        declarationReference
            = new NameReference(original.getDeclaration(), getCopyContext().newContextForProperty("declaration"));
    }

    @Override
    public String toString() {
        return BareProxyModule.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitLinkedModule(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimeModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public ModuleDeclarationImpl getSuperAnnotatedConstruct() {
        require(State.FINISHED);
        return moduleDeclaration;
    }

    /**
     * {@inheritDoc}
     *
     * A proxy module does not enclose any elements.
     */
    @Override
    public ImmutableList<? extends IElementImpl> getEnclosedElements() {
        require(State.LINKED);
        @Nullable ImmutableList<PortImpl> localDeclaredPorts = declaredPorts;
        assert localDeclaredPorts != null : "must be non-null when linked";
        return localDeclaredPorts;
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        require(State.LINKED);
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(simpleName);

        @Nullable Map<SimpleName, PortImpl> localDeclaredPortsMap = declaredPortsMap;
        assert localDeclaredPortsMap != null : "must be non-null when linked";
        @Nullable PortImpl port = localDeclaredPortsMap.get(simpleName);
        if (port != null && clazz.isInstance(port)) {
            @SuppressWarnings("unchecked")
            T typedPort = (T) port;
            return typedPort;
        } else {
            return null;
        }
    }

    @Override
    public ModuleDeclarationImpl getDeclaration() {
        require(State.FINISHED);
        @Nullable ModuleDeclarationImpl localModuleDeclaration = moduleDeclaration;
        assert localModuleDeclaration != null : "must be non-null when finished";
        return localModuleDeclaration;
    }

    /**
     * {@inheritDoc}
     *
     * Note that a proxy module can only reference simple or composite modules (both of which do not have implicit
     * ports).
     */
    @Override
    public ImmutableList<PortImpl> getPorts() {
        require(State.LINKED);
        @Nullable ImmutableList<PortImpl> localDeclaredPorts = declaredPorts;
        assert localDeclaredPorts != null : "must be non-null when linked";
        return localDeclaredPorts;
    }

    @Override
    public ImmutableList<IInPortImpl> getInPorts() {
        require(State.LINKED);
        @Nullable ImmutableList<IInPortImpl> localInPorts = inPorts;
        assert localInPorts != null : "must be non-null when linked";
        return localInPorts;
    }

    @Override
    public ImmutableList<IOutPortImpl> getOutPorts() {
        require(State.LINKED);
        @Nullable ImmutableList<IOutPortImpl> localOutPorts = outPorts;
        assert localOutPorts != null : "must be non-null when linked";
        return localOutPorts;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        require(State.LINKED);
        @Nullable ImmutableList<PortImpl> localDeclaredPort = declaredPorts;
        assert localDeclaredPort != null : "must be non-null when linked";
        freezables.addAll(localDeclaredPort);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Before linking, we do not have any information about our ports. This method adds concrete port instances.
     * Note that the referenced module declarations (either composite module or simple module) can not contain implicit
     * ports.
     */
    @Override
    void preProcessFreezable(FinishContext context) throws LinkerException {
        ModuleDeclarationImpl localModuleDeclaration
            = context.getDeclaration(BareModuleDeclaration.NAME, ModuleDeclarationImpl.class, declarationReference);
        moduleDeclaration = localModuleDeclaration;

        CopyContext copyContext = getCopyContext();
        // Add missing ports. At link time, instances of this package may not yet be fully constructed, and we can
        // therefore not rely on ModuleDeclarationImpl#getPorts(). Instead, ModuleDeclarationImpl provides
        // getBarePorts() for this purpose.
        PortAccumulationState state = new PortAccumulationState();
        Map<SimpleName, PortImpl> newPortMap = new LinkedHashMap<>();
        collect(
            localModuleDeclaration.getBarePorts(),
            copyContext.newSystemContext("declared ports of " + declarationReference)
                .newContextForListProperty("barePorts"),
            portConstructor(state),
            Arrays.asList(
                portAccumulator(state),
                mapAccumulator(newPortMap, PortImpl::getSimpleName)
            )
        );
        declaredPortsMap = Collections.unmodifiableMap(newPortMap);
        declaredPorts = ImmutableList.copyOf(newPortMap.values());
        inPorts = ImmutableList.copyOf(state.getInPorts());
        outPorts = ImmutableList.copyOf(state.getOutPorts());
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
