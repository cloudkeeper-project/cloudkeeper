package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareParentModule;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Composite-module instance.
 *
 * <p>Composite modules contain arbitrary other modules, called <strong>child modules</strong>. The graph defined by all
 * children and connections between children is always a directed acyclic graph.
 */
abstract class ParentModuleImpl extends ModuleImpl implements RuntimeParentModule {
    private final Map<SimpleName, IElementImpl> enclosedElementsMap;
    private final ImmutableList<PortImpl> allPorts;
    private final ImmutableList<PortImpl> declaredPorts;
    private final ImmutableList<IInPortImpl> inPorts;
    private final ImmutableList<IOutPortImpl> outPorts;
    private final ImmutableList<ModuleImpl> modules;
    private final ImmutableList<ConnectionImpl> connections;

    ParentModuleImpl(@Nullable BareParentModule original, CopyContext parentContext, int index,
            List<? extends BarePort> implicitPorts) throws LinkerException {
        super(original, parentContext, index);
        assert original != null;

        CopyContext context = getCopyContext();

        Map<SimpleName, IElementImpl> newEnclosedElementsMap = new LinkedHashMap<>();
        PortAccumulationState state = new PortAccumulationState();
        List<Accumulator<PortImpl>> accumulators = Arrays.asList(
            mapAccumulator(newEnclosedElementsMap, PortImpl::getSimpleName),
            portAccumulator(state)
        );
        collect(original.getDeclaredPorts(), "declaredPorts", portConstructor(state), accumulators);
        declaredPorts = ImmutableList.copyOf(state.getAllPorts());
        collect(implicitPorts, context.newSystemContext("implicit ports").newContextForListProperty("implicitPorts"),
            portConstructor(state), accumulators);
        allPorts = implicitPorts.isEmpty()
            ? declaredPorts
            : ImmutableList.copyOf(state.getAllPorts());
        inPorts = ImmutableList.copyOf(state.getInPorts());
        outPorts = ImmutableList.copyOf(state.getOutPorts());

        ArrayList<ModuleImpl> newModules = new ArrayList<>();
        collect(
            original.getModules(),
            context.newContextForListProperty("modules"),
            (module, copyContext) -> ModuleImpl.copyOf(module, copyContext, newModules.size()),
            Arrays.asList(
                listAccumulator(newModules),
                mapAccumulator(newEnclosedElementsMap, ModuleImpl::getSimpleName)
            )
        );
        modules = ImmutableList.copyOf(newModules);
        enclosedElementsMap = Collections.unmodifiableMap(newEnclosedElementsMap);

        ArrayList<ConnectionImpl> newConnections = new ArrayList<>();
        collect(
            original.getConnections(), context.newContextForListProperty("connections"),
            (originalConnection, elementContext)
                -> ConnectionImpl.copyOf(originalConnection, elementContext, newConnections.size()),
            Collections.singletonList(listAccumulator(newConnections))
        );
        connections = ImmutableList.copyOf(newConnections);
    }

    /**
     * {@inheritDoc}
     *
     * A parent module does not inherit any annotations.
     */
    @Override
    public IElementImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(simpleName);

        IElementImpl element = enclosedElementsMap.get(simpleName);
        if (clazz.isInstance(element)) {
            @SuppressWarnings("unchecked")
            T typedElement = (T) element;
            return typedElement;
        } else {
            return null;
        }
    }

    @Override
    public final ImmutableList<? extends IElementImpl> getEnclosedElements() {
        return ImmutableList.copyOf(enclosedElementsMap.values());
    }

    @Override
    public final ImmutableList<PortImpl> getPorts() {
        return allPorts;
    }

    @Override
    public final ImmutableList<PortImpl> getDeclaredPorts() {
        return declaredPorts;
    }

    @Override
    public final ImmutableList<IInPortImpl> getInPorts() {
        return inPorts;
    }

    @Override
    public final ImmutableList<IOutPortImpl> getOutPorts() {
        return outPorts;
    }

    @Override
    public final ImmutableList<ModuleImpl> getModules() {
        return modules;
    }

    @Override
    public final ImmutableList<ConnectionImpl> getConnections() {
        return connections;
    }

    @Override
    @Nullable
    public final ModuleImpl getModule(SimpleName name) {
        Objects.requireNonNull(name);
        for (ModuleImpl module: modules) {
            assert module.getSimpleName() != null : "child modules must have name";
            if (module.getSimpleName().contentEquals(name)) {
                return module;
            }
        }
        return null;
    }

    @Override
    final void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.addAll(declaredPorts);
        freezables.addAll(modules);

        // We need to insert the ports into proxy module *before* we can resolve the ports in our connections.
        // Therefore, connections must follow modules.
        freezables.addAll(connections);
        collectEnclosedByParentModule(freezables);
    }

    abstract void collectEnclosedByParentModule(Collection<AbstractFreezable> freezables);

    @Override
    final void preProcessFreezable(FinishContext context) { }

    @Override
    final void verifyFreezable(VerifyContext context) { }
}
