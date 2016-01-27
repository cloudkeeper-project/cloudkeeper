package com.svbio.cloudkeeper.linker;

import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.marshaling.TreeBuilder;
import com.svbio.cloudkeeper.marshaling.TreeBuilderMarshalTarget;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.MarshalingException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class consists of static methods for transforming unverified abstract syntax trees (the bare model) into the
 * linked and verified runtime state.
 */
public final class Linker {
    private Linker() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns an absolute annotated execution trace.
     *
     * <p>This method links the given module using the provided annotation overrides and the given repository. It
     * creates an annotated execution trace from the given (bare) execution trace, which will be of type module and
     * represent the given module.
     *
     * @param absoluteTrace absolute execution trace that will correspond to the given module
     * @param bareModule bare module
     * @param overrides list of annotation overrides
     * @param repository repository previously created with {@link #createRepository(List, LinkerOptions)}
     * @param linkerOptions linker options
     * @return absolute annotated execution trace
     * @throws IllegalArgumentException if {@code repository} was not created with
     *     {@link #createRepository(List, LinkerOptions)}
     * @throws LinkerException if linking fails because of inconsistent or incomplete input
     */

    public static RuntimeAnnotatedExecutionTrace createAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace,
            BareModule bareModule, List<? extends BareOverride> overrides, RuntimeRepository repository,
            LinkerOptions linkerOptions) throws LinkerException {
        return LinkerImpl.getInstance().createAnnotatedExecutionTrace(absoluteTrace, bareModule, overrides,
            repository, linkerOptions);
    }

    /**
     * Returns a new repository containing the linked and verified plug-in declarations from the given bundles.
     *
     * @param bundles List of bare bundles. This list must not contain the system bundle.
     * @param linkerOptions linker options
     * @return the repository
     * @throws LinkerException if linking fails because of inconsistent or incomplete input
     */
    public static RuntimeRepository createRepository(List<? extends BareBundle> bundles, LinkerOptions linkerOptions)
            throws LinkerException {
        return LinkerImpl.getInstance().createRepository(bundles, linkerOptions);
    }

    private static Collection<? extends SerializationDeclarationImpl> toTypedCollection(
            Collection<? extends RuntimeSerializationDeclaration> declarations) {
        for (RuntimeSerializationDeclaration declaration: declarations) {
            if (!(declaration instanceof SerializationDeclarationImpl)) {
                throw new IllegalArgumentException(String.format(
                    "Expected marshaler declaration created by %s, but got %s.", Linker.class, declaration
                ));
            }
        }
        @SuppressWarnings("unchecked")
        Collection<? extends SerializationDeclarationImpl> collection
            = (Collection<? extends SerializationDeclarationImpl>) declarations;
        return collection;
    }

    /**
     * Marshals the given object into a tree representation.
     *
     * @param object object to marshal
     * @param declarations collection of {@link RuntimeSerializationDeclaration} instances to choose from when a
     *     marshaler invokes {@link com.svbio.cloudkeeper.model.api.MarshalContext#writeObject(Object, Key)}
     * @return the tree representation
     * @throws MarshalingException if a marshaling error occurs
     * @throws IOException if an I/O error occurs
     */
    public static RuntimeSerializationRoot marshalToTree(Object object,
            Collection<? extends RuntimeSerializationDeclaration> declarations) throws IOException {
        Objects.requireNonNull(object);
        Objects.requireNonNull(declarations);
        declarations.forEach(Objects::requireNonNull);

        IdentityHashMap<Marshaler<?>, SerializationDeclarationImpl> marshalerMap
            = new IdentityHashMap<>(declarations.size());
        List<Marshaler<?>> marshalers = new ArrayList<>(declarations.size());
        for (SerializationDeclarationImpl declaration: toTypedCollection(declarations)) {
            Marshaler<?> instance = declaration.getInstance();
            marshalerMap.put(instance, declaration);
            marshalers.add(instance);
        }

        TreeBuilderImpl treeBuilder = new TreeBuilderImpl(0, NoKey.instance(), marshalerMap);
        return (RuntimeSerializationRoot) TreeBuilderMarshalTarget.marshal(object, marshalers, treeBuilder);
    }

    private static final class TreeBuilderImpl implements TreeBuilder<SerializationNodeImpl> {
        private final int level;
        private final Key key;
        private final IdentityHashMap<Marshaler<?>, SerializationDeclarationImpl> marshalerMap;

        private TreeBuilderImpl(int level, Key key, IdentityHashMap<Marshaler<?>,
                SerializationDeclarationImpl> marshalerMap) {
            assert level >= 0;
            this.level = level;
            this.key = key;
            this.marshalerMap = marshalerMap;
        }

        @Override
        public boolean shouldMarshal(Marshaler<?> marshaler, Object object) {
            return level == 0 || !(object instanceof String);
        }

        @Override
        public SerializedStringImpl createObjectNode(Marshaler<?> marshaler, Object object) {
            assert object instanceof String : "contradiction to shouldMarshal()";
            return new SerializedStringImpl(key, (String) object);
        }

        @Override
        public ByteSequenceImpl createByteSequenceNode(ByteSequence byteSequence) throws IOException {
            return new ByteSequenceImpl(key, byteSequence.toByteArray());
        }

        private SerializationDeclarationImpl declaration(Marshaler<?> marshaler) {
            // The following should never throw.
            return Objects.requireNonNull(marshalerMap.get(marshaler));
        }

        @Override
        public SerializationRootImpl createRedirectNode(Marshaler<?> marshaler, SerializationNodeImpl node) {
            return new SerializationRootImpl(
                key, declaration(marshaler), Collections.singletonMap(NoKey.instance(), node));
        }

        @Override
        public SerializationRootImpl createParentNode(Marshaler<?> marshaler,
                Map<Key, SerializationNodeImpl> children) {
            return new SerializationRootImpl(key, declaration(marshaler), children);
        }

        @Override
        public TreeBuilderImpl resolve(Key key) {
            return new TreeBuilderImpl(level + 1, key, marshalerMap);
        }
    }
}
