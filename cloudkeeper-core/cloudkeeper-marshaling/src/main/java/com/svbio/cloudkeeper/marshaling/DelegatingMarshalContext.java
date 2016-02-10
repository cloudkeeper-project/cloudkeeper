package com.svbio.cloudkeeper.marshaling;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.marshaling.MarshalTarget.ObjectAction;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ByteSequenceNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.MarshalingException;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Marshal context that recursively creates sub-contexts when {@link MarshalContext#writeObject(Object, Key)} is called
 * and that delegates elementary marshal operations to a {@link MarshalTarget} instance.
 *
 * <p>Instances of this marshal context have a list of {@link Marshaler} instances, and when creating a sub-context, the
 * first instance <em>capable of</em> marshaling the object passed to {@link #writeObject(Object, Key)} will be used.
 */
public final class DelegatingMarshalContext implements MarshalContext {
    private final Key contextKey;
    private final MarshalTarget marshalTarget;
    private final Set<Key> entries = new HashSet<>();
    private final ImmutableList<Marshaler<?>> marshalers;
    private final NodeVisitor nodeVisitor = new NodeVisitor();

    private final Marshaler<?> marshaler;
    @Nullable private final DelegatingMarshalContext ancestor;
    private final Map<MonitoredOutputStream, Key> openOutputStreams = new LinkedHashMap<>();

    private boolean closed = false;
    private boolean marshalTargetStarted = false;

    private DelegatingMarshalContext(Key contextKey, MarshalTarget marshalTarget, Marshaler<?> marshaler,
            ImmutableList<Marshaler<?>> marshalers, @Nullable DelegatingMarshalContext ancestor) {
        this.contextKey = contextKey;
        this.marshalTarget = marshalTarget;
        this.marshaler = marshaler;
        this.marshalers = marshalers;
        this.ancestor = ancestor;
    }

    /**
     * Creates a new marshal context that delegates elementary marshal operations to the given marshal target.
     *
     * <p>In most cases, {@link #marshal(Object, Collection, MarshalTarget)} is the more appropriate high-level
     * alternative to instantiating a marshal context explicitly.
     *
     * @param currentMarshaler {@link Marshaler} instance that will receive the returned marshal context as argument to
     *     {@link Marshaler#put(Object, MarshalContext)}
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link MarshalContext#writeObject(Object, Key)} recursively
     * @param marshalTarget marshal target used for elementary marshal operations
     * @return the new marshal context
     */
    public static DelegatingMarshalContext create(Marshaler<?> currentMarshaler,
            Collection<? extends Marshaler<?>> marshalers, MarshalTarget marshalTarget) {
        Objects.requireNonNull(currentMarshaler);
        Objects.requireNonNull(marshalers);
        Objects.requireNonNull(marshalTarget);
        return new DelegatingMarshalContext(NoKey.instance(), marshalTarget, currentMarshaler,
            ImmutableList.copyOf(marshalers), null);
    }

    /**
     * Marshals an object using the given marshal target.
     *
     * <p>This method performs the equivalent of the following steps:
     * <ul><li>
     *     find the appropriate marshaler using {@link #findMarshaler(Object, Collection)},
     * </li><li>
     *     create a marshal context using {@link #create(Marshaler, Collection, MarshalTarget)},
     * </li><li>
     *     process the object using {@link #acceptObject(Object)}.
     * </li></ul>
     *
     * @param object object to marshal
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link MarshalContext#writeObject(Object, Key)} recursively
     * @param marshalTarget marshal target used for elementary marshal operations
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static void marshal(Object object, Collection<? extends Marshaler<?>> marshalers,
            MarshalTarget marshalTarget) throws IOException {
        Objects.requireNonNull(object);
        Objects.requireNonNull(marshalers);
        Objects.requireNonNull(marshalTarget);
        Marshaler<?> marshaler = findMarshaler(object, marshalers);
        try (DelegatingMarshalContext marshalContext = create(marshaler, marshalers, marshalTarget)) {
            marshalContext.acceptObject(object);
        }
    }

    /**
     * Writes the given marshaling tree to the given marshal target.
     *
     * <p>Any contained {@link RawObjectNode} will be marshaled in the same way as by
     * {@link #marshal(Object, Collection, MarshalTarget)}.
     *
     * <p>This method performs the equivalent of the following steps:
     * <ul><li>
     *     find the appropriate marshaler, either the one returned by {@link ObjectNode#getMarshaler()} if it is
     *     non-null, or otherwise the one returned by {@link #findMarshaler(Object, Collection)},
     * </li><li>
     *     create a marshal context using {@link #create(Marshaler, Collection, MarshalTarget)},
     * </li><li>
     *     process the marshaling tree using {@link #acceptTree(ObjectNode)}.
     * </li></ul>
     *
     * @param tree marshaling tree that needs to be transformed and marshaled
     * @param marshalers collection of {@link Marshaler} instances to choose from when a marshaler invokes
     *     {@link MarshalContext#writeObject(Object, Key)} recursively
     * @param marshalTarget marshal target for elementary marshal operations
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static void processMarshalingTree(ObjectNode tree, Collection<? extends Marshaler<?>> marshalers,
            MarshalTarget marshalTarget) throws IOException {
        Objects.requireNonNull(tree);
        Objects.requireNonNull(marshalers);
        Objects.requireNonNull(marshalTarget);
        Marshaler<?> marshaler = findMarshalerForObjectNode(tree, marshalers);
        try (DelegatingMarshalContext marshalContext = create(marshaler, marshalers, marshalTarget)) {
            marshalContext.acceptTree(tree);
        }
    }

    /**
     * Returns the first marshaler (in iteration order) that is capable of marshaling the given object.
     *
     * @param object object that the returned marshaler must be capable of handling
     * @param marshalers collection of marshalers
     * @return the marshaler
     * @throws MarshalingException if no marshaler is capable
     */
    public static Marshaler<?> findMarshaler(Object object, Collection<? extends Marshaler<?>> marshalers)
            throws MarshalingException {
        Objects.requireNonNull(object);
        Objects.requireNonNull(marshalers);
        return marshalers.stream()
            .filter(marshaler -> marshaler.canHandle(object))
            .findFirst()
            .orElseThrow(
                () -> new MarshalingException(String.format(
                    "None of %s is capable of marshaling instance: %s.", marshalers, object
                ))
            );
    }

    private static Marshaler<?> findMarshalerForObjectNode(ObjectNode objectNode,
            Collection<? extends Marshaler<?>> marshalers) throws MarshalingException {
        @Nullable Marshaler<?> nodeMarshaller = objectNode.getMarshaler();
        assert objectNode instanceof RawObjectNode || nodeMarshaller != null;
        return nodeMarshaller == null
            ? findMarshaler(((RawObjectNode) objectNode).getObject(), marshalers)
            : nodeMarshaller;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        marshalTarget.finish();

        if (!openOutputStreams.isEmpty()) {
            try (Closer closer = new Closer()) {
                openOutputStreams.keySet().forEach(closer::register);
                throw new MarshalingException(String.format(
                    "Failed to serialize object at %s, because output streams for the "
                        + "following keys were not closed: %s", this, openOutputStreams.values()
                ));
            }
        }

        if (!marshalTargetStarted) {
            throw new MarshalingException(String.format(
                "Marshaler of %s did not call any %s methods.", marshaler.getClass(), MarshalContext.class
            ));
        }
    }

    @Override
    public String toString() {
        LinkedList<Key> path = new LinkedList<>();
        @Nullable DelegatingMarshalContext currentContext = this;
        do {
            Key currentKey = currentContext.contextKey;
            if (!(currentKey instanceof NoKey)) {
                path.add(currentKey);
            }
            currentContext = currentContext.ancestor;
        } while (currentContext != null);

        StringBuilder stringBuilder = new StringBuilder(256);
        stringBuilder.append("context '");
        boolean first = true;
        Iterator<Key> it = path.descendingIterator();
        while (it.hasNext()) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('/');
            }
            stringBuilder.append(it.next());
        }
        return stringBuilder.append('\'').toString();
    }

    private void requireValidNewToken(Key key) throws MarshalingException {
        Objects.requireNonNull(key);
        if (closed) {
            throw new MarshalingException("Marshal context used after it was closed.");
        } else if (entries.contains(key)) {
            throw new MarshalingException(String.format("Entry with key '%s' already exists.", key));
        } else if ((!(key instanceof NoKey) && entries.contains(NoKey.instance()))) {
            throw new MarshalingException(String.format(
                "Tried to add key '%s' to serialization node that already contains empty key.", key
            ));
        } else if (key instanceof NoKey && !entries.isEmpty()) {
            throw new MarshalingException(String.format(
                "Tried to add empty key to non-empty serialization node (already contains at least key '%s').",
                entries.iterator().next()
            ));
        }
    }

    /**
     * Calls {@link MarshalTarget#marshalerChain(List)} in order inform the write context that
     * {@link MarshalTarget#resolve(Key)} will not be called with an empty key.
     */
    private void sendPersistenceChain() throws IOException {
        if (!entries.isEmpty()) {
            // Write context was already informed
            return;
        }

        LinkedList<Marshaler<?>> marshalerChain = new LinkedList<>();
        @Nullable DelegatingMarshalContext currentContext = this;
        do {
            marshalerChain.addFirst(currentContext.marshaler);
            currentContext = currentContext.ancestor;
        } while (currentContext != null && currentContext.contextKey instanceof NoKey);
        marshalTarget.marshalerChain(Collections.unmodifiableList(marshalerChain));
    }

    private void startIfNeeded() throws IOException {
        if (!marshalTargetStarted) {
            marshalTargetStarted = true;
            marshalTarget.start(marshaler);
        }
    }

    @Override
    public OutputStream newOutputStream(Key key) throws IOException {
        requireValidNewToken(key);
        startIfNeeded();
        sendPersistenceChain();
        entries.add(key);

        MonitoredOutputStream outputStream = new MonitoredOutputStream(marshalTarget.newOutputStream(key));
        openOutputStreams.put(outputStream, key);
        return outputStream;
    }

    @Override
    public void putByteSequence(ByteSequence byteSequence, Key key) throws IOException {
        requireValidNewToken(key);
        startIfNeeded();
        sendPersistenceChain();
        entries.add(key);

        marshalTarget.putByteSequence(byteSequence, key);
    }


    private void newKeyForObject(Key key) throws IOException {
        requireValidNewToken(key);
        startIfNeeded();
        if (!(key instanceof NoKey)) {
            sendPersistenceChain();
        }
        entries.add(key);
    }

    @Override
    public void writeObject(Object object, Key key) throws IOException {
        Objects.requireNonNull(object);
        newKeyForObject(key);

        Marshaler<?> childMarshaler = findMarshaler(object, marshalers);
        try (DelegatingMarshalContext childContext
                = new DelegatingMarshalContext(key, marshalTarget.resolve(key), childMarshaler, marshalers, this)) {
            childContext.acceptObject(object);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean isSelfContained(Marshaler<T> marshaler, Object object) {
        return marshaler.isImmutable((T) object);
    }

    @SuppressWarnings("unchecked")
    private <T> void uncheckedPut(Marshaler<T> marshaler, Object object) throws IOException {
        marshaler.put((T) object, this);
    }

    /**
     * Writes the given object within the current marshal context.
     *
     * <p>This method is unlike {@link #writeObject(Object, Key)}, which creates a new child marshal context. This
     * method instead uses the current context to marshal the object, using the {@link Marshaler} instance specified at
     * construction time.
     *
     * @param object object to be written
     * @throws MarshalingException if marshaling the given object fails (for instance, when a user-defined
     *     {@link Marshaler#put(Object, MarshalContext)} implementation throws or causes a
     *     {@link MarshalingException})
     * @throws IOException if the operation failed because of an I/O error
     */
    public void acceptObject(Object object) throws IOException {
        startIfNeeded();

        ObjectAction action = ObjectAction.MARSHAL;
        if (isSelfContained(marshaler, object)) {
            action = Objects.requireNonNull(marshalTarget.putImmutableObject(object));
        }
        if (action == ObjectAction.MARSHAL) {
            uncheckedPut(marshaler, object);
        }
    }

    /**
     * Writes the given marshaling tree within the current marshal context.
     *
     * <p>Similar to {@link #acceptObject(Object)}, this method uses the current context to marshal the object-node,
     * using the {@link Marshaler} instance specified at construction. This means that the {@link Marshaler} returned by
     * {@link ObjectNode#getMarshaler()} will be ignored for the given node (for child nodes, the marshaler property
     * will not be ignored).
     *
     * @param tree tree represented by object-node
     * @throws MarshalingException if marshaling the given object fails (for instance, when a user-defined
     *     {@link Marshaler#put(Object, MarshalContext)} implementation throws or causes a
     *     {@link MarshalingException})
     * @throws IOException if the operation failed because of an I/O error
     */
    public void acceptTree(ObjectNode tree) throws IOException {
        @Nullable IOException exception = tree.accept(ObjectNodeVisitor.INSTANCE, this);
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Output stream that wraps another output stream and removes the current stream from a map upon close.
     *
     * <p>This class simply calls {@link Map#remove(Object)} within {@link #close()}, synchronized on
     * {@link #openOutputStreams}.
     */
    private final class MonitoredOutputStream extends FilterOutputStream {
        /**
         * Constructor that does <em>not</em> add this decorated output stream to {@code openOutputStreams}. It is the
         * caller's responsibility to do so.
         *
         * @param outputStream the wrapped output stream
         */
        MonitoredOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        public void close() throws IOException {
            super.close();
            openOutputStreams.remove(this);
        }
    }

    @FunctionalInterface
    interface IORunnable {
        void run() throws IOException;
    }

    private static IOException run(IORunnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (IOException exception) {
            return exception;
        }
    }

    private enum ObjectNodeVisitor implements MarshalingTreeNodeVisitor<IOException, DelegatingMarshalContext> {
        INSTANCE;

        @Nullable
        @Override
        public IOException visitRawObjectNode(RawObjectNode node, @Nullable DelegatingMarshalContext context) {
            assert context != null;
            return run(() -> context.acceptObject(node.getObject()));
        }

        @Nullable
        @Override
        public IOException visitMarshaledObjectNode(MarshaledObjectNode node,
                @Nullable DelegatingMarshalContext context) {
            assert context != null;
            for (Map.Entry<Key, MarshalingTreeNode> entry : node.getChildren().entrySet()) {
                @Nullable IOException exception = entry.getValue().accept(context.nodeVisitor, entry.getKey());
                if (exception != null) {
                    return exception;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public IOException visitMarshaledReplacementNode(MarshaledReplacementObjectNode node,
                @Nullable DelegatingMarshalContext context) {
            assert context != null;
            return node.getChild().accept(context.nodeVisitor, NoKey.instance());
        }

        @Nullable
        @Override
        public IOException visitByteSequenceNode(ByteSequenceNode node, @Nullable DelegatingMarshalContext context) {
            throw new IllegalStateException("Cannot happen.");
        }
    }

    private final class NodeVisitor implements MarshalingTreeNodeVisitor<IOException, Key> {
        private IOException writeObjectNode(ObjectNode objectNode, @Nullable Key key) {
            assert key != null;
            try (
                DelegatingMarshalContext childContext = new DelegatingMarshalContext(
                    key,
                    marshalTarget.resolve(key),
                    findMarshalerForObjectNode(objectNode, marshalers),
                    marshalers,
                    DelegatingMarshalContext.this
                )
            ) {
                newKeyForObject(key);
                childContext.acceptTree(objectNode);
                return null;
            } catch (IOException exception) {
                return exception;
            }
        }

        @Nullable
        @Override
        public IOException visitRawObjectNode(RawObjectNode node, @Nullable Key key) {
            return writeObjectNode(node, key);
        }

        @Nullable
        @Override
        public IOException visitMarshaledObjectNode(MarshaledObjectNode node, @Nullable Key key) {
            return writeObjectNode(node, key);
        }

        @Nullable
        @Override
        public IOException visitMarshaledReplacementNode(MarshaledReplacementObjectNode node, @Nullable Key key) {
            return writeObjectNode(node, key);
        }

        @Nullable
        @Override
        public IOException visitByteSequenceNode(ByteSequenceNode node, @Nullable Key key) {
            assert key != null;
            return run(() -> putByteSequence(node.getByteSequence(), key));
        }
    }
}
