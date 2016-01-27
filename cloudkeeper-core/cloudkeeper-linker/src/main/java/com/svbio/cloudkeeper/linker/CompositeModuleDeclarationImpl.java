package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class CompositeModuleDeclarationImpl extends ModuleDeclarationImpl implements RuntimeCompositeModuleDeclaration {
    private final CompositeModuleImpl template;

    // No need to be volatile because instance variable is only accessed before object is finished, which will always
    // be from a single thread.
    @Nullable private List<MutablePort<?>> mutablePorts;

    CompositeModuleDeclarationImpl(BareCompositeModuleDeclaration original, CopyContext parentContext)
            throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        @Nullable BareCompositeModule originalTemplate = original.getTemplate();
        CopyContext templateContext = context.newContextForProperty("template");
        template = new CompositeModuleImpl(originalTemplate, templateContext, -1);
        assert originalTemplate != null : "non-null due to successful CompositeModuleImpl constructor";
        Preconditions.requireCondition(template.getDeclaredAnnotations().isEmpty(), templateContext,
            "Template module of composite-module declaration cannot have annotations.");

        // At this point, it should be perfectly safe to create a mutable copy of the port list as well. All possible
        // errors should have been detected when creating the list of runtime (immutable) ports.
        List<? extends BarePort> originalPorts = originalTemplate.getDeclaredPorts();
        mutablePorts = originalPorts.stream().map(MutablePort::copyOfPort).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return BareCompositeModuleDeclaration.Default.toString(this);
    }

    @Override
    public ImmutableList<? extends IElementImpl> getEnclosedElements() {
        return ImmutableList.of(template);
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(simpleName);

        if (simpleName.contentEquals(TEMPLATE_ELEMENT_NAME) && clazz.isInstance(template)) {
            @SuppressWarnings("unchecked")
            T typedTemplate = (T) template;
            return typedTemplate;
        } else {
            return null;
        }
    }

    @Override
    public ImmutableList<PortImpl> getPorts() {
        return template.getPorts();
    }

    @Override
    List<? extends BarePort> getBarePorts() {
        if (getState().compareTo(State.FINISHED) >= 0) {
            return getPorts();
        } else {
            assert mutablePorts != null : "must be non-null while not yet finished";
            return mutablePorts;
        }
    }

    @Override
    public ImmutableList<IInPortImpl> getInPorts() {
        return template.getInPorts();
    }

    @Override
    public ImmutableList<IOutPortImpl> getOutPorts() {
        return template.getOutPorts();
    }

    @Override
    public CompositeModuleImpl getTemplate() {
        return template;
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
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.add(template);
    }

    @Override
    void finishFreezable(FinishContext context) {
        mutablePorts = null;
    }
}
