package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;

final class SerializationDeclarationImpl extends PluginDeclarationImpl implements RuntimeSerializationDeclaration {
    @Nullable private volatile Marshaler<?> marshaler;

    SerializationDeclarationImpl(BareSerializationDeclaration original, CopyContext parentContext)
            throws LinkerException {
        super(original, parentContext);
    }

    @Override
    public <T, P> T accept(RuntimePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public String toString() {
        return BareSerializationDeclaration.Default.toString(this);
    }

    @Override
    public PluginDeclarationImpl getSuperAnnotatedConstruct() {
        // TODO: No inheritance?
        return null;
    }

    @Override
    public ImmutableList<? extends IElementImpl> getEnclosedElements() {
        return ImmutableList.of();
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        return null;
    }

    @Override
    public IElementImpl getEnclosingElement() {
        return null;
    }

    @Override
    public Marshaler<?> getInstance() {
        require(State.FINISHED);
        @Nullable Marshaler<?> localMarshalerInstance = marshaler;
        assert localMarshalerInstance != null;
        return localMarshalerInstance;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) { }

    private final class ThrowingMarshaler<T> implements Marshaler<T> {
        private IllegalStateException exception() {
            return new IllegalStateException(String.format(
                "Tried to use the %s instance corresponding to %s even though the class provider configured in the "
                    + "linker options returned no class.",
                Marshaler.class.getSimpleName(), SerializationDeclarationImpl.this
            ));
        }

        @Override
        public boolean canHandle(Object object) {
            throw exception();
        }

        @Override
        public boolean isImmutable(T object) {
            throw exception();
        }

        @Override
        public void put(T object, MarshalContext context) {
            throw exception();
        }

        @Override
        public T get(UnmarshalContext context) {
            throw exception();
        }
    }

    @Override
    void finishFreezable(FinishContext context) throws LinkerException {
        @SuppressWarnings("unchecked")
        Class<Marshaler<?>> serializationInterface = (Class<Marshaler<?>>) (Class<?>) Marshaler.class;
        @Nullable Marshaler<?> newMarshaler = context.instanceOfJavaClass(this, serializationInterface);
        if (newMarshaler == null) {
            newMarshaler = new ThrowingMarshaler<>();
        }
        marshaler = newMarshaler;
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
