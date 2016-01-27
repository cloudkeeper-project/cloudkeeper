package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleVisitor;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;

/**
 * An input module is specified by an object-store key or content specified inline.
 * It has no in-ports and a single out-port named "value", through which it provides the content it encloses.
 */
final class InputModuleImpl extends ModuleImpl implements RuntimeInputModule {
    private final TypeMirrorImpl outPortType;
    private final PortImpl.OutPortImpl outPort;

    private final boolean rawLifecycleReponsible;
    @Nullable private SerializationRootImpl raw;
    @Nullable private Object value;

    InputModuleImpl(BareInputModule original, CopyContext parentContext, int index) throws LinkerException {
        super(original, parentContext, index);

        CopyContext context = getCopyContext();
        outPortType = TypeMirrorImpl.copyOf(original.getOutPortType(), context.newContextForProperty("outPortType"));
        outPort = new PortImpl.OutPortImpl(context.newSystemContext("implicit out-port"),
            SimpleName.identifier(OUT_PORT_NAME), outPortType, 0, 0);

        value = original.getValue();

        @Nullable BareSerializationRoot originalRaw = original.getRaw();
        raw = originalRaw == null
            ? null
            : new SerializationRootImpl(originalRaw, context.newContextForProperty("raw"));
        rawLifecycleReponsible = raw != null;
    }

    @Override
    public String toString() {
        return BareInputModule.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitInputModule(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimeModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    /**
     * {@inheritDoc}
     *
     * An input module does not inherit any annotations.
     */
    @Override
    public IElementImpl getSuperAnnotatedConstruct() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * An input module only encloses its out-port.
     */
    @Override
    public ImmutableList<PortImpl.OutPortImpl> getEnclosedElements() {
        return ImmutableList.of(outPort);
    }

    @Override
    @Nullable
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(simpleName);

        if (simpleName.contentEquals(BareInputModule.OUT_PORT_NAME) && clazz.isInstance(outPort)) {
            @SuppressWarnings("unchecked")
            T typedOutPort = (T) outPort;
            return typedOutPort;
        } else {
            return null;
        }
    }

    @Override
    public ImmutableList<PortImpl> getPorts() {
        return ImmutableList.<PortImpl>of(outPort);
    }

    @Override
    public ImmutableList<IInPortImpl> getInPorts() {
        return ImmutableList.of();
    }

    @Override
    public ImmutableList<IOutPortImpl> getOutPorts() {
        return ImmutableList.<IOutPortImpl>of(outPort);
    }

    @Override
    public RuntimeTypeMirror getOutPortType() {
        return outPort.getType();
    }

    @Override
    @Nullable
    public Object getValue() {
        return value;
    }

    @Override
    @Nullable
    public SerializationRootImpl getRaw() {
        return raw;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.add(outPortType);
        freezables.add(outPort);
        if (rawLifecycleReponsible) {
            freezables.add(raw);
        }
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) throws LinkerException {
        CopyContext copyContext = getCopyContext();
        Preconditions.requireCondition(value != null || raw != null, copyContext,
            "Expected either value or raw to be non-null");

        if (value == null) {
            assert raw != null;
            value = context.valueFromSerializationTree(raw);
        } else if (raw == null) {
            assert value != null;
            raw = context.serializationTreeFromValue(this);
        }
    }
}
