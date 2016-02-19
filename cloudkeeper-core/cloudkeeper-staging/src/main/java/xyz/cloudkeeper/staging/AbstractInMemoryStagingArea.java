package xyz.cloudkeeper.staging;

import xyz.cloudkeeper.marshaling.DelegatingUnmarshalContext;
import xyz.cloudkeeper.marshaling.MarshalTarget;
import xyz.cloudkeeper.marshaling.MarshalingTreeBuilder;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ByteSequenceNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import xyz.cloudkeeper.marshaling.MarshalingTreeNodeVisitor;
import xyz.cloudkeeper.marshaling.MarshalingTreeUnmarshalSource;
import xyz.cloudkeeper.model.api.ExecutionTraceNotFoundException;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration;
import xyz.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract in-memory staging area that stores persistence-tree representations of values.
 */
public abstract class AbstractInMemoryStagingArea extends AbstractStagingArea {
    private final RuntimeContext runtimeContext;

    protected AbstractInMemoryStagingArea(RuntimeContext runtimeContext,
            RuntimeAnnotatedExecutionTrace executionTrace) {
        super(executionTrace);
        this.runtimeContext = runtimeContext;
    }

    /**
     * Context for storing nodes of a persistence tree, as the result of a call to
     * {@link #putObject(RuntimeExecutionTrace, Object)} or
     * {@link #putSerializationTree(RuntimeExecutionTrace, RuntimeSerializationRoot)}.
     *
     * <p>A new node context is created for every {@link MarshalTarget} that corresponds to an {@link ExecutionTrace}.
     */
    protected interface NodeContext {
        /**
         * Returns a new node context for the given array index.
         *
         * @param index array index
         * @return the new node context
         */
        NodeContext resolve(Index index);

        /**
         * Stores the given node for
         */
        void storeNode(ObjectNode node);
    }

    /**
     * Returns a node context for the given execution trace.
     *
     * @param target relative target execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteTarget the absolute annotated execution trace corresponding to {@code target}
     * @return the node context
     */
    protected abstract NodeContext getNodeContext(RuntimeExecutionTrace target,
        RuntimeAnnotatedExecutionTrace absoluteTarget);

    private static List<Marshaler<?>> marshalers(RuntimeAnnotatedExecutionTrace absoluteTarget) {
        return absoluteTarget.getSerializationDeclarations().stream()
            .map(RuntimeSerializationDeclaration::getInstance).collect(Collectors.toList());
    }

    private enum StoreVisitor implements MarshalingTreeNodeVisitor<Void, NodeContext> {
        INSTANCE;

        @Nullable
        @Override
        public Void visitRawObjectNode(RawObjectNode node, @Nullable NodeContext nodeContext) {
            assert nodeContext != null;
            nodeContext.storeNode(node);
            return null;
        }

        @Nullable
        @Override
        public Void visitMarshaledObjectNode(MarshaledObjectNode node, @Nullable NodeContext nodeContext) {
            assert nodeContext != null;
            nodeContext.storeNode(node);
            for (Map.Entry<Key, MarshalingTreeNode> entry: node.getChildren().entrySet()) {
                Key key = entry.getKey();
                if (key instanceof Index) {
                    entry.getValue().accept(this, nodeContext.resolve((Index) key));
                }
            }
            return null;
        }

        @Nullable
        @Override
        public Void visitMarshaledReplacementNode(MarshaledReplacementObjectNode node,
                @Nullable NodeContext nodeContext) {
            assert nodeContext != null;
            nodeContext.storeNode(node);
            return null;
        }

        @Nullable
        @Override
        public Void visitByteSequenceNode(ByteSequenceNode node, @Nullable NodeContext nodeContext) {
            return null;
        }
    }

    private static boolean shouldMarshalSelfContainedObject(List<Key> path, Marshaler<?> marshaler, Object object) {
        return false;
    }

    @Override
    protected final void putObject(RuntimeExecutionTrace target, RuntimeAnnotatedExecutionTrace absoluteTarget,
            Object object) throws IOException {
        ObjectNode tree = MarshalingTreeBuilder.marshal(
            object, marshalers(absoluteTarget), AbstractInMemoryStagingArea::shouldMarshalSelfContainedObject);
        tree.accept(StoreVisitor.INSTANCE, getNodeContext(target, absoluteTarget));
    }


    @Override
    protected final void putSerializationTree(RuntimeExecutionTrace target,
            RuntimeAnnotatedExecutionTrace absoluteTarget, RuntimeSerializationRoot serializationTree)
            throws IOException {
        ObjectNode sourceTree = marshalingTree(serializationTree);
        ObjectNode tree = MarshalingTreeBuilder.processMarshalingTree(
            sourceTree, marshalers(absoluteTarget), (path, marshaler, currentObject) -> true);
        tree.accept(StoreVisitor.INSTANCE, getNodeContext(target, absoluteTarget));
    }

    /**
     * Returns the marshaling-tree node for the given execution trace.
     *
     * @param source relative source execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteSource the absolute annotated execution trace corresponding to {@code source}
     * @return the marshaling-tree node for the given execution trace
     * @throws ExecutionTraceNotFoundException if there is no persistence-tree node at the given execution trace
     */
    protected abstract ObjectNode getNode(RuntimeExecutionTrace source,
        RuntimeAnnotatedExecutionTrace absoluteSource) throws ExecutionTraceNotFoundException;

    @Override
    protected final Object getObject(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource)
            throws IOException {
        return DelegatingUnmarshalContext.unmarshal(
            MarshalingTreeUnmarshalSource.create(getNode(source, absoluteSource)),
            runtimeContext.getClassLoader()
        );
    }

    /**
     * Returns a staging area for the given descendant execution trace.
     *
     * <p>This method has the same requirements and behaves in the same way as
     * {@link #resolveDescendant(RuntimeExecutionTrace, RuntimeAnnotatedExecutionTrace)}. However, it has an additional
     * argument of type {@link RuntimeContext} (needed for constructor
     * {@link #AbstractInMemoryStagingArea(RuntimeContext, RuntimeAnnotatedExecutionTrace)}).
     *
     * @param trace execution trace with the guarantees documented for
     *     {@link #resolveDescendant(RuntimeExecutionTrace, RuntimeAnnotatedExecutionTrace)}
     * @param absoluteTrace annotated execution trace that is the result of
     *     {@code getAnnotatedExecutionTrace().resolveExecutionTrace(trace)}
     * @param runtimeContext runtime context containing the CloudKeeper repository, the Java class loader, and the
     *     serialization utilities
     * @return the staging area for the given descendant execution trace
     */
    protected abstract AbstractInMemoryStagingArea resolveDescendant(RuntimeExecutionTrace trace,
        RuntimeAnnotatedExecutionTrace absoluteTrace, RuntimeContext runtimeContext);

    @Override
    protected final AbstractStagingArea resolveDescendant(RuntimeExecutionTrace trace,
            RuntimeAnnotatedExecutionTrace absoluteTrace) {
        return resolveDescendant(trace, absoluteTrace, runtimeContext);
    }
}
