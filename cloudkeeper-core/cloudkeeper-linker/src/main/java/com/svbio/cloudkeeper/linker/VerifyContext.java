package com.svbio.cloudkeeper.linker;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.marshaling.DelegatingUnmarshalContext;
import com.svbio.cloudkeeper.marshaling.UnmarshalSource;
import com.svbio.cloudkeeper.model.PreprocessingException;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.MarshalingException;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareByteSequence;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializedString;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeByteSequence;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationNode;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

final class VerifyContext {
    private final LinkerImpl linker;
    private final LinkerOptions linkerOptions;

    VerifyContext(LinkerImpl linker, LinkerOptions linkerOptions) {
        this.linker = linker;
        this.linkerOptions = linkerOptions;
    }

    private static final class StringUnmarshalSourceImpl implements UnmarshalSource {
        private final SerializedStringImpl serializedString;

        private StringUnmarshalSourceImpl(SerializedStringImpl serializedString) {
            this.serializedString = serializedString;
        }

        private static UnsupportedOperationException fail() {
            return new UnsupportedOperationException(
                "Impossible operation for unmarshal source that represents raw-object node."
            );
        }

        @Override
        public Marshaler<?> getMarshaler() throws MarshalingException {
            throw fail();
        }

        @Nullable
        @Override
        public String getObject() throws MarshalingException {
            return serializedString.getString();
        }

        @Override
        public ByteSequence getByteSequence(Key key) throws IOException {
            throw fail();
        }

        @Override
        public UnmarshalSource resolve(Key key) throws IOException {
            throw fail();
        }
    }

    private static final class RootUnmarshalSourceImpl implements UnmarshalSource {
        private final SerializationRootImpl root;

        private RootUnmarshalSourceImpl(SerializationRootImpl root) {
            this.root = root;
        }

        @Override
        public Marshaler<?> getMarshaler() {
            return root.getDeclaration().getInstance();
        }

        @Nullable
        @Override
        public Object getObject() {
            return null;
        }

        @Nullable
        @Override
        public ByteSequence getByteSequence(Key key) throws MarshalingException {
            @Nullable RuntimeSerializationNode node = root.getEntry(key);
            if (node instanceof ByteSequenceImpl) {
                return ((RuntimeByteSequence) node).toByteSequence();
            } else if (node != null) {
                throw new MarshalingException(String.format(
                    "Expected '%s' in %s to be of %s, but got %s.", key, root, BareByteSequence.class, node
                ));
            } else {
                return null;
            }
        }

        @Nullable
        @Override
        public UnmarshalSource resolve(Key key) throws MarshalingException {
            @Nullable RuntimeSerializationNode node = root.getEntry(key);
            if (node instanceof SerializationRootImpl) {
                return new RootUnmarshalSourceImpl((SerializationRootImpl) node);
            } else if (node  instanceof SerializedStringImpl) {
                return new StringUnmarshalSourceImpl((SerializedStringImpl) node);
            } else if (node != null) {
                throw new MarshalingException(String.format(
                    "Expected node '%s' in %s to be of %s or %s, but got %s.", key, root, BareSerializationRoot.class,
                    BareSerializedString.class, node
                ));
            } else {
                return null;
            }
        }
    }

    Object valueFromSerializationTree(SerializationRootImpl serializationTree) throws PreprocessingException {
        Objects.requireNonNull(serializationTree);
        if (!linkerOptions.isDeserializeSerializationTrees()) {
            return null;
        }

        try {
            UnmarshalSource unmarshalSource = new RootUnmarshalSourceImpl(serializationTree);
            DelegatingUnmarshalContext unmarshalContext = DelegatingUnmarshalContext.create(
                unmarshalSource,
                linkerOptions.getUnmarshalClassLoader().orElse(Thread.currentThread().getContextClassLoader())
            );
            return unmarshalContext.process();
        } catch (IOException exception) {
            throw new PreprocessingException("Unmarshaling failed.", exception,
                serializationTree.getCopyContext().toLinkerTrace());
        }
    }

    /**
     * Returns a serialization tree representing the native value of the given input module.
     *
     * @param inputModule input module whose value is to be serialized
     * @return the serialization tree
     * @throws PreprocessingException if serialization fails
     */
    @Nullable
    SerializationRootImpl serializationTreeFromValue(InputModuleImpl inputModule)
            throws PreprocessingException {
        Objects.requireNonNull(inputModule);
        Objects.requireNonNull(inputModule.getValue());
        if (!linkerOptions.isSerializeValues()) {
            return null;
        }

        // TODO: Rethink use of AnnotatedExecutionTraceImpl. Might break for default arguments in the future.
        AnnotatedExecutionTraceImpl emptyAbsoluteTrace
            = new AnnotatedExecutionTraceImpl(ExecutionTrace.empty(), inputModule,
                ImmutableList.<OverrideImpl>of(), linker.getDefaultSerializationDeclarations());
        AnnotatedExecutionTraceImpl portAbsoluteTrace;

        // Each port is either an out-port, an in-port, or both. If it's both (that is, an I/O port), it's fine to
        // to arbitrarily treat the port as an out-port, because the execution trace does not matter here. After
        // all, the list of overrides used for constructing emptyAbsoluteTrace was empty.
        portAbsoluteTrace = emptyAbsoluteTrace.resolveExecutionTrace(
            ExecutionTrace.empty().resolveOutPort(SimpleName.identifier(BareInputModule.OUT_PORT_NAME))
        );

        try {
            return (SerializationRootImpl) Linker.marshalToTree(
                inputModule.getValue(), portAbsoluteTrace.getSerializationDeclarations());
        } catch (IOException exception) {
            throw new PreprocessingException(String.format(
                "Marshaling input-module value (of %s) into tree representation failed.",
                inputModule.getValue().getClass()
            ), exception, inputModule.getCopyContext().toLinkerTrace());
        }
    }
}
