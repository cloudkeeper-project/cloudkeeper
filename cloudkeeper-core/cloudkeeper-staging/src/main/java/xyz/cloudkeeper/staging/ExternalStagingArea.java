package xyz.cloudkeeper.staging;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import cloudkeeper.serialization.StringMarshaler;
import cloudkeeper.types.ByteSequence;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import xyz.cloudkeeper.marshaling.DelegatingMarshalContext;
import xyz.cloudkeeper.marshaling.DelegatingUnmarshalContext;
import xyz.cloudkeeper.marshaling.MarshalTarget;
import xyz.cloudkeeper.marshaling.UnmarshalSource;
import xyz.cloudkeeper.model.api.MarshalContext;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.MarshalingException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.UnmarshalContext;
import xyz.cloudkeeper.model.api.staging.StagingException;
import xyz.cloudkeeper.model.api.util.ScalaFutures;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.NoKey;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeByteSequence;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationNode;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationNodeVisitor;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializedString;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Abstract staging area that is backed by some external storage.
 *
 * Redirect nodes in the marshaling tree are coalesced, and information about used {@link Marshaler} classes is stored
 * in metadata of class {@link MutableObjectMetadata}.
 */
public abstract class ExternalStagingArea extends AbstractStagingArea {
    private final RuntimeContext runtimeContext;
    private final RuntimeSerializationDeclaration stringSerialization;
    private final ExecutionContext executionContext;

    protected ExternalStagingArea(RuntimeAnnotatedExecutionTrace executionTrace, RuntimeContext runtimeContext,
            ExecutionContext executionContext) {
        super(executionTrace);
        this.runtimeContext = runtimeContext;
        stringSerialization = Objects.requireNonNull(runtimeContext.getRepository().getElement(
            RuntimeSerializationDeclaration.class, Name.qualifiedName(StringMarshaler.class.getName())
        ));
        this.executionContext = executionContext;
    }

    /**
     * Returns the runtime context of this staging area.
     *
     * <p>Callers must not close the returned runtime context.
     *
     * @return the runtime context of this staging area
     */
    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    /**
     * Returns the execution context of this staging area
     *
     * @return the execution context of this staging area
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Override
    protected final <T> Future<T> toFuture(Callable<T> callable, String format, Object... args) {
        return Futures.future(callable, executionContext)
            .transform(
                ScalaFutures.identityMapper(),
                new Mapper<Throwable, Throwable>() {
                    @Override
                    public Throwable apply(Throwable throwable) {
                        return new StagingException(
                            String.format("Failed to %s.", String.format(format, args)),
                            throwable
                        );
                    }
                },
                executionContext
            );
    }

    /**
     * Creates a new {@link WriteContext} instance for the given execution trace.
     *
     * <p>Note that the {@link WriteContext#resolve(Key)} method of the returned object will be called, with
     * {@code target.getKey()} as argument. That is, the returned {@link WriteContext} should correspond to the parent
     * of {@code target}.
     *
     * @param target execution trace that a value will be serialized to
     * @return the new {@link WriteContext} instance
     */
    protected abstract WriteContext newWriteContext(RuntimeExecutionTrace target);

    /**
     * Creates a new {@link ReadContext} instance for the given execution trace.
     *
     * <p>Note that the {@link ReadContext#resolve(Key)} method of the returned object will be called, with
     * {@code source.getKey()} as argument. That is, the returned {@link ReadContext} should correspond to the parent
     * of {@code target}.
     *
     * @param source execution trace that a value will be deserialized from
     * @return the new {@link ReadContext} instance
     */
    protected abstract ReadContext newReadContext(RuntimeExecutionTrace source);

    @Override
    protected final void putObject(RuntimeExecutionTrace target, RuntimeAnnotatedExecutionTrace absoluteTarget,
            Object object) throws IOException {
        ImmutableList<RuntimeSerializationDeclaration> declarations
            = ImmutableList.copyOf(absoluteTarget.getSerializationDeclarations());

        IdentityHashMap<Marshaler<?>, RuntimeSerializationDeclaration> marshalerMap
            = new IdentityHashMap<>(declarations.size());
        List<Marshaler<?>> marshalers = new ArrayList<>(declarations.size());
        for (RuntimeSerializationDeclaration declaration: declarations) {
            Marshaler<?> instance = declaration.getInstance();
            marshalerMap.put(instance, declaration);
            marshalers.add(instance);
        }

        DelegatingMarshalContext.marshal(
            object,
            marshalers,
            new MarshalTargetImpl(newWriteContext(target), marshalerMap)
        );
    }

    private final class MarshalerMapBuilderVisitor implements RuntimeSerializationNodeVisitor<Void, Void> {
        private final List<Marshaler<?>> marshalers = new ArrayList<>();
        private final IdentityHashMap<Marshaler<?>, RuntimeSerializationDeclaration> marshalerMap
            = new IdentityHashMap<>();

        @Nullable
        @Override
        public Void visitRoot(RuntimeSerializationRoot root, @Nullable Void ignored) {
            RuntimeSerializationDeclaration declaration = root.getDeclaration();
            Marshaler<?> instance = declaration.getInstance();
            marshalerMap.put(instance, declaration);
            marshalers.add(instance);
            for (RuntimeSerializationNode entry: root.getEntries()) {
                entry.accept(this, null);
            }
            return null;
        }

        @Nullable
        @Override
        public Void visitByteSequence(RuntimeByteSequence byteSequence, @Nullable Void ignored) {
            return null;
        }

        @Nullable
        @Override
        public Void visitString(RuntimeSerializedString serializedString, @Nullable Void ignored) {
            Marshaler<?> instance = stringSerialization.getInstance();
            marshalerMap.put(instance, stringSerialization);
            marshalers.add(instance);
            return null;
        }
    }

    @Override
    protected final void putSerializationTree(RuntimeExecutionTrace target,
            RuntimeAnnotatedExecutionTrace absoluteTarget, RuntimeSerializationRoot serializationTree)
            throws IOException {
        MarshalerMapBuilderVisitor visitor = new MarshalerMapBuilderVisitor();
        serializationTree.accept(visitor, null);
        DelegatingMarshalContext.processMarshalingTree(
            marshalingTree(serializationTree),
            ImmutableList.copyOf(visitor.marshalers),
            new MarshalTargetImpl(newWriteContext(target), visitor.marshalerMap)
        );
    }

    @Override
    protected final Object getObject(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource)
            throws IOException {
        return DelegatingUnmarshalContext.unmarshal(
            new UnmarshalSourceImpl(newReadContext(source), runtimeContext.getRepository()),
            runtimeContext.getClassLoader()
        );
    }

    /**
     * Delegate of the {@link UnmarshalSource} implementation used by {@link ExternalStagingArea}.
     *
     * <p>This interface is similar to, but provides a slightly higher-level abstraction than, {@link UnmarshalSource}.
     * Subclasses are not required to verify arguments.
     */
    public interface ReadContext {
        /**
         * Returns the metadata for the object represented by this context.
         *
         * <p>This method is the inverse of {@link WriteContext#putMetadata(MutableObjectMetadata)}.
         *
         * @return metadata for the byte sequence represented by this context
         * @throws IOException if an I/O error occurs
         */
        MutableObjectMetadata getMetadata() throws IOException;

        /**
         * Returns the byte sequence identified by the given key.
         *
         * @see UnmarshalContext#getByteSequence(Key)
         */
        ByteSequence getByteSequence(Key key) throws IOException;

        /**
         * Returns a delegate for the given key.
         *
         * <p>Implementations are free to return the current instance if appropriate.
         *
         * @param key index or simple name for the read context
         * @return read context for the given key
         * @throws IOException if an I/O error occurred
         */
        ReadContext resolve(Key key) throws IOException;
    }

    /**
     * Delegate of the {@link MarshalTarget} implementation used by {@link ExternalStagingArea}.
     *
     * <p>This interface is similar to, but provides a slightly higher-level abstraction than, {@link MarshalTarget}.
     * Subclasses are not required to verify arguments.
     */
    public interface WriteContext {
        /**
         * Returns an {@link OutputStream} for writing a byte stream to the location represented by this
         * context.
         *
         * @param key index or simple name of the new byte stream, may be empty
         * @param metadata Metadata for byte stream, or {@code null} if there is none. If not null (implying that the
         *     current serialized object consists of a single stream), then the same object reference will be passed to
         *     {@link #putMetadata(MutableObjectMetadata)} if the returned output stream is successfully written to and
         *     subsequently closed.
         *
         * @see MarshalContext#newOutputStream(Key)
         */
        OutputStream newOutputStream(Key key, @Nullable MutableObjectMetadata metadata) throws IOException;

        /**
         * Called after all streams of an object have been successfully written.
         *
         * <p>This method is the inverse of {@link ReadContext#getMetadata()}.
         *
         * @param metadata Metadata for the current serialization node. If the serialization tree of the current object
         *     consists of a single unnamed stream, then this argument is the same object reference that was previously
         *     passed to {@link #newOutputStream(Key, MutableObjectMetadata)} (with
         *     {@link NoKey} as first argument). Never null.
         *
         * @throws IOException if an I/O error occurs
         */
        void putMetadata(MutableObjectMetadata metadata) throws IOException;

        /**
         * Writes the given byte sequence to the location represented by this context.
         *
         * @param byteSequence byte sequence
         * @param key index or simple name of the new byte stream, may be empty
         * @param metadata Metadata for byte stream, or {@code null} if there is none.
         *
         * @see MarshalContext#putByteSequence(ByteSequence, Key)
         */
        void putByteSequence(ByteSequence byteSequence, Key key, @Nullable MutableObjectMetadata metadata)
            throws IOException;

        /**
         * Returns a write context for the given key.
         *
         * <p>Implementations are free to return the current instance if appropriate.
         *
         * @param key index or simple name for the write context
         * @return write context for the given key
         * @throws IOException if an I/O error occurred
         */
        WriteContext resolve(Key key) throws IOException;
    }

    private static final class UnmarshalSourceImpl implements UnmarshalSource {
        private final ReadContext readContext;
        private final RuntimeRepository repository;
        @Nullable private List<Marshaler<?>> marshalers;
        private final int indexOfCurrentMarshaler;

        private UnmarshalSourceImpl(ReadContext readContext, RuntimeRepository repository) throws MarshalingException {
            this(readContext, repository, null, 0);
        }

        private UnmarshalSourceImpl(ReadContext readContext, RuntimeRepository repository,
                @Nullable List<Marshaler<?>> marshalers, int indexOfCurrentMarshaler) throws MarshalingException {
            assert marshalers != null || indexOfCurrentMarshaler == 0;
            this.readContext = readContext;
            this.repository = repository;
            this.marshalers = marshalers;
            this.indexOfCurrentMarshaler = indexOfCurrentMarshaler;
        }

        @Override
        public Marshaler<?> getMarshaler() throws IOException {
            if (marshalers == null) {
                MutableObjectMetadata metadata = readContext.getMetadata();
                List<MutableMarshalerIdentifier> marshalerIdentifiers = metadata.getMarshalers();
                marshalers = new ArrayList<>(marshalerIdentifiers.size());
                for (MutableMarshalerIdentifier serialization: metadata.getMarshalers()) {
                    @Nullable RuntimeSerializationDeclaration declaration = repository.getElement(
                        RuntimeSerializationDeclaration.class, serialization.getName());
                    if (declaration == null) {
                        throw new MarshalingException(String.format(
                            "Could not find %s '%s' in repository.",
                            BareSerializationDeclaration.NAME, serialization.getName()
                        ));
                    }
                    marshalers.add(declaration.getInstance());
                }
            }
            if (indexOfCurrentMarshaler > marshalers.size()) {
                throw new MarshalingException(
                    "Metadata in staging area does not represent marshaling tree of expected form."
                );
            }
            return marshalers.get(indexOfCurrentMarshaler);
        }

        @Nullable
        @Override
        public Object getObject() throws IOException {
            return null;
        }

        @Nullable
        @Override
        public ByteSequence getByteSequence(Key key) throws IOException {
            return readContext.getByteSequence(key);
        }

        @Nullable
        @Override
        public UnmarshalSourceImpl resolve(Key key) throws IOException {
            if (key instanceof NoKey) {
                return new UnmarshalSourceImpl(readContext, repository, marshalers, indexOfCurrentMarshaler + 1);
            } else {
                return new UnmarshalSourceImpl(readContext.resolve(key), repository);
            }
        }
    }

    private static final class MarshalTargetImpl implements MarshalTarget {
        private final WriteContext writeContext;
        private final Map<Marshaler<?>, RuntimeSerializationDeclaration> marshalerMap;
        @Nullable private MutableObjectMetadata metadata;

        private MarshalTargetImpl(WriteContext writeContext, Map<Marshaler<?>,
                RuntimeSerializationDeclaration> marshalerMap) {
            this.writeContext = writeContext;
            this.marshalerMap = marshalerMap;
        }

        @Override
        public void start(Marshaler<?> marshaler) { }

        @Override
        public void marshalerChain(List<Marshaler<?>> marshalerChain) {
            metadata = new MutableObjectMetadata();
            List<MutableMarshalerIdentifier> marshalerIdentifiers = metadata.getMarshalers();
            for (Marshaler<?> marshaler: marshalerChain) {
                @Nullable RuntimeSerializationDeclaration declaration = marshalerMap.get(marshaler);
                if (declaration == null) {
                    throw new IllegalStateException(String.format(
                        "Marshaler chain contains instance of %s, even though this %s subclass is not referenced by "
                            + "the current execution trace.", marshaler.getClass(), Marshaler.class.getSimpleName()
                    ));
                }
                marshalerIdentifiers.add(
                    new MutableMarshalerIdentifier()
                        .setName(declaration.getQualifiedName())
                        .setBundleIdentifier(declaration.getPackage().getBundleIdentifier())
                );
            }
        }

        @Override
        public OutputStream newOutputStream(Key key) throws IOException {
            return writeContext.newOutputStream(key, metadata);
        }

        @Override
        public void putByteSequence(ByteSequence byteSequence, Key key) throws IOException {
            writeContext.putByteSequence(byteSequence, key, metadata);
        }

        @Override
        public ObjectAction putImmutableObject(Object object) throws IOException {
            return ObjectAction.MARSHAL;
        }

        @Override
        public MarshalTarget resolve(Key key) throws IOException {
            return key instanceof NoKey
                ? this
                : new MarshalTargetImpl(writeContext.resolve(key), marshalerMap);
        }

        @Override
        public void finish() throws IOException {
            if (metadata == null) {
                return;
            }

            writeContext.putMetadata(metadata);
        }
    }
}
