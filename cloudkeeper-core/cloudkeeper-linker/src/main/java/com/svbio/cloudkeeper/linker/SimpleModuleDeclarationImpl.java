package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.Executable;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class SimpleModuleDeclarationImpl extends ModuleDeclarationImpl implements RuntimeSimpleModuleDeclaration {
    private final Map<SimpleName, PortImpl> declaredPortsMap;
    private final ImmutableList<PortImpl> declaredPorts;
    private final ImmutableList<IInPortImpl> inPorts;
    private final ImmutableList<IOutPortImpl> outPorts;
    @Nullable private volatile Executable executable;

    // No need to be volatile because instance variable is only accessed before object is finished, which will always
    // be from a single thread.
    @Nullable private List<BarePort> mutablePorts;

    SimpleModuleDeclarationImpl(BareSimpleModuleDeclaration original, CopyContext parentContext)
            throws LinkerException {
        super(original, parentContext);

        List<? extends BarePort> originalPorts = original.getPorts();

        PortAccumulationState state = new PortAccumulationState();
        Map<SimpleName, PortImpl> newPortMap = new LinkedHashMap<>();
        collect(
            originalPorts,
            "ports",
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

        // At this point, it should be perfectly safe to create a mutable copy of the port list as well. All possible
        // errors should have been detected when creating the list of runtime (immutable) ports.
        mutablePorts = originalPorts.stream().map(MutablePort::copyOfPort).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return BareSimpleModuleDeclaration.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimeModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public ImmutableList<PortImpl> getEnclosedElements() {
        return declaredPorts;
    }

    @Override
    @Nullable
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(simpleName);

        PortImpl port = declaredPortsMap.get(simpleName);
        if (clazz.isInstance(port)) {
            @SuppressWarnings("unchecked")
            T typedElement = (T) port;
            return typedElement;
        } else {
            return null;
        }
    }

    @Override
    public ImmutableList<PortImpl> getPorts() {
        return declaredPorts;
    }

    @Override
    List<? extends BarePort> getBarePorts() {
        if (getState().compareTo(State.FINISHED) >= 0) {
            return declaredPorts;
        } else {
            assert mutablePorts != null : "must be non-null while not yet finished";
            return mutablePorts;
        }
    }

    @Override
    public ImmutableList<IInPortImpl> getInPorts() {
        return inPorts;
    }

    @Override
    public ImmutableList<IOutPortImpl> getOutPorts() {
        return outPorts;
    }

    @Override
    public Executable toExecutable() {
        require(State.FINISHED);
        @Nullable Executable localExecutable = executable;
        if (localExecutable == null) {
            throw new IllegalStateException(String.format(
                "toExecutable() called on %s even though instance is not available. This indicates invalid linker "
                    + "options.", this
            ));
        }
        return localExecutable;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.addAll(declaredPorts);
    }

    @Override
    void finishFreezable(FinishContext context) throws LinkerException {
        mutablePorts = null;
        executable = context.getExecutable(this);
    }
}
