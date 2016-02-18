package com.svbio.cloudkeeper.staging;

import akka.japi.Option;
import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ByteSequenceNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.MarshaledReplacementObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.RawObjectNode;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeByteSequence;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationNode;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationNodeVisitor;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializedString;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Abstract staging area.
 *
 * <p>This class provides a skeletal implementation of the {@link StagingArea} interface to minimize the effort required
 * to implement this interface. All methods (except {@link #getStagingAreaProvider()}) are implemented by final methods
 * that verify arguments and throw the mandated exceptions in case of violations. This class therefore defines new
 * abstract methods that do not need to verify arguments and that also do not need to throw high-level exceptions (just
 * letting {@link IOException}s propagate is fine).
 *
 * <p>Implementations of this abstract class can choose between asynchronous or synchronous execution by implementing
 * the {@link #toFuture(Callable, String, Object...)} accordingly.
 */
public abstract class AbstractStagingArea implements StagingArea {
    private final RuntimeAnnotatedExecutionTrace executionTrace;

    protected AbstractStagingArea(RuntimeAnnotatedExecutionTrace executionTrace) {
        Objects.requireNonNull(executionTrace);
        this.executionTrace = executionTrace;
    }

    /**
     * Verifies that the trace is relative and the reference is non-empty.
     */
    private static void requireRelativeTraceWithReference(RuntimeExecutionTrace trace) {
        if (trace.getReference().isEmpty()) {
            throw new IllegalArgumentException(String.format("Expected relative trace with non-empty reference, " +
                "but got %s.", trace));
        }
    }

    /**
     * Returns a {@link Future} object that will be completed with the result of the given {@link Callable}.
     *
     * @param callable the computation
     * @param format A format string that can be passed to {@link String#format(Locale, String, Object...)}. This should
     *     This should be an infinitive clause (without the "to") that could be appended to "Failed to ...".
     * @param args arguments referenced by the format specifiers in the format string
     * @param <T> type of the future
     * @return the {@link Future} object
     */
    protected abstract <T> Future<T> toFuture(Callable<T> callable, String format, Object... args);

    @Override
    public final RuntimeAnnotatedExecutionTrace getAnnotatedExecutionTrace() {
        return executionTrace;
    }

    /**
     * Deletes all entries with keys that start with the given execution trace (inclusively).
     *
     * <p>This abstract method is called by {@link #delete(RuntimeExecutionTrace)} as well as by
     * {@link #copy(RuntimeExecutionTrace, RuntimeExecutionTrace)}, {@link #putObject(RuntimeExecutionTrace, Object)},
     * and {@link #putSerializationTree(RuntimeExecutionTrace, RuntimeSerializationRoot)}. While the signature of this
     * method is similar, the requirements are less stringent: In particular, this method is not required to verify
     * arguments (the caller is guaranteed to do that).
     *
     * @param prefix execution trace prefix with non-empty call stack or non-empty value reference that does not contain
     *     array indices
     * @param absolutePrefix the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(prefix)}
     * @throws IOException if an I/O error occurs
     */
    protected abstract void delete(RuntimeExecutionTrace prefix, RuntimeAnnotatedExecutionTrace absolutePrefix)
        throws IOException;

    /**
     * Takes necessary action before writing an object to the given execution trace.
     *
     * <p>This method is called (potentially asynchronously) by:
     * <ul><li>
     *     {@link #copy(RuntimeExecutionTrace, RuntimeExecutionTrace)}
     * </li><li>
     *     {@link #putObject(RuntimeExecutionTrace, Object)}
     * </li><li>
     *     {@link #putSerializationTree(RuntimeExecutionTrace, RuntimeSerializationRoot)}
     * </li></ul>
     *
     * @param prefix execution trace prefix with non-empty call stack or non-empty value reference
     * @param absolutePrefix the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(prefix)}
     * @throws IOException if an I/O error occurs
     */
    protected abstract void preWrite(RuntimeExecutionTrace prefix, RuntimeAnnotatedExecutionTrace absolutePrefix)
        throws IOException;

    @Override
    public final Future<RuntimeExecutionTrace> delete(RuntimeExecutionTrace prefix) {
        Objects.requireNonNull(prefix);
        RuntimeExecutionTrace callStack = prefix.getFrames();
        RuntimeExecutionTrace reference = prefix.getReference();
        if (prefix.isEmpty()) {
            throw new IllegalArgumentException(String.format("Expected non-empty trace, but got %s.", prefix));
        } else if (reference.size() > 1) {
            throw new IllegalArgumentException(String.format(
                "Expected no array indices in value reference, but got %s.", prefix
            ));
        } else if (callStack.isEmpty() && reference.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Expected execution trace with non-empty call stack or non-empty value reference, but got %s.", prefix
            ));
        }
        RuntimeAnnotatedExecutionTrace absolutePrefix = executionTrace.resolveExecutionTrace(prefix);

        return toFuture(() -> {
            delete(prefix, absolutePrefix);
            return prefix;
        }, "delete %s", absolutePrefix);
    }

    /**
     * Writes the object for the given source execution trace also as object for the given target execution trace.
     *
     * <p>This abstract method is called by {@link #copy(RuntimeExecutionTrace, RuntimeExecutionTrace)}. While the
     * signature of this method is similar, the requirements are less stringent: In particular, this method is not
     * required to verify arguments (the caller is guaranteed to do that).
     *
     * <p>It is guaranteed that {@link #preWrite(RuntimeExecutionTrace, RuntimeAnnotatedExecutionTrace)} has been called
     * immediately before this method.
     *
     * @param source relative source execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param target relative target execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteSource the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(source)}
     * @param absoluteTarget the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(target)}
     * @throws IOException if an I/O error occurs
     */
    protected abstract void copy(RuntimeExecutionTrace source, RuntimeExecutionTrace target,
        RuntimeAnnotatedExecutionTrace absoluteSource, RuntimeAnnotatedExecutionTrace absoluteTarget)
        throws IOException;

    @Override
    public final Future<RuntimeExecutionTrace> copy(RuntimeExecutionTrace source, RuntimeExecutionTrace target) {
        requireRelativeTraceWithReference(source);
        requireRelativeTraceWithReference(target);
        RuntimeAnnotatedExecutionTrace absoluteSource = executionTrace.resolveExecutionTrace(source);
        RuntimeAnnotatedExecutionTrace absoluteTarget = executionTrace.resolveExecutionTrace(target);

        return toFuture(() -> {
            preWrite(target, absoluteTarget);
            copy(source, target, absoluteSource, absoluteTarget);
            return target;
        }, "copy from %s to %s", absoluteSource, absoluteTarget);
    }

    /**
     * Writes an object for the given execution trace.
     *
     * <p>This abstract method is called by {@link #putObject(RuntimeExecutionTrace, Object)}. While the signature of
     * this method is similar, the requirements are less stringent: In particular, this method is not required to verify
     * arguments (the caller is guaranteed to do that).
     *
     * <p>It is guaranteed that {@link #preWrite(RuntimeExecutionTrace, RuntimeAnnotatedExecutionTrace)} has been called
     * immediately before this method.
     *
     * @param target relative target execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteTarget the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(target)}
     * @param object object that is to be written
     * @throws IOException if an I/O error occurs
     */
    protected abstract void putObject(RuntimeExecutionTrace target, RuntimeAnnotatedExecutionTrace absoluteTarget,
        Object object) throws IOException;

    @Override
    public final Future<RuntimeExecutionTrace> putObject(RuntimeExecutionTrace target, Object object) {
        requireRelativeTraceWithReference(target);
        Objects.requireNonNull(object);
        RuntimeAnnotatedExecutionTrace absoluteTarget = executionTrace.resolveExecutionTrace(target);

        return toFuture(() -> {
            preWrite(target, absoluteTarget);
            putObject(target, absoluteTarget, object);
            return target;
        }, "write object at %s", absoluteTarget);
    }

    /**
     * Writes an object, represented as serialization tree, for the given execution trace.
     *
     * <p>This abstract method is called by {@link #putObject(RuntimeExecutionTrace, Object)}. While the signature of
     * this method is similar, the requirements are less stringent: In particular, this method is not required to verify
     * arguments (the caller is guaranteed to do that).
     *
     * <p>It is guaranteed that {@link #preWrite(RuntimeExecutionTrace, RuntimeAnnotatedExecutionTrace)} has been called
     * immediately before this method.
     *
     * @param target relative target execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteTarget the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(target)}
     * @param serializationTree serialization tree that is to be written
     * @throws IOException if an I/O error occurs
     */
    protected abstract void putSerializationTree(RuntimeExecutionTrace target,
        RuntimeAnnotatedExecutionTrace absoluteTarget, RuntimeSerializationRoot serializationTree)
        throws IOException;

    @Override
    public final Future<RuntimeExecutionTrace> putSerializationTree(RuntimeExecutionTrace target,
            RuntimeSerializationRoot serializationTree) {
        requireRelativeTraceWithReference(target);
        Objects.requireNonNull(serializationTree);
        RuntimeAnnotatedExecutionTrace absoluteTarget = executionTrace.resolveExecutionTrace(target);

        return toFuture(() -> {
            preWrite(target, absoluteTarget);
            putSerializationTree(target, absoluteTarget, serializationTree);
            return target;
        }, "write persistence tree at %s", absoluteTarget);
    }

    /**
     * Returns the object for the given execution trace.
     *
     * @param source relative source execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteSource the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(source)}
     * @return the object for the given execution trace
     * @throws IOException if an I/O error occurs
     */
    protected abstract Object getObject(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource)
        throws IOException;

    @Override
    public final Future<Object> getObject(RuntimeExecutionTrace source) {
        requireRelativeTraceWithReference(source);
        RuntimeAnnotatedExecutionTrace absoluteSource = executionTrace.resolveExecutionTrace(source);

        return toFuture(() -> getObject(source, absoluteSource), "get object from %s", absoluteSource);
    }

    /**
     * Returns whether a value exists at an execution trace.
     *
     * @param source relative source execution trace, {@link RuntimeExecutionTrace#getReference()} is guaranteed
     *     non-empty
     * @param absoluteSource the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(source)}
     * @return {@code true} if an value is present at {@code source}, or {@code false} if not
     * @throws IOException if an I/O error occurs
     */
    protected abstract boolean exists(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource)
        throws IOException;

    @Override
    public final Future<Boolean> exists(RuntimeExecutionTrace source) {
        requireRelativeTraceWithReference(source);
        RuntimeAnnotatedExecutionTrace absoluteSource = executionTrace.resolveExecutionTrace(source);

        return toFuture(() -> exists(source, absoluteSource), "determine if %s exists");
    }

    /**
     * Returns the maximum index present at the given execution trace that is smaller than or equal to the given upper
     * bound.
     *
     * @param trace Relative execution trace. Guaranteed to be of type {@link RuntimeExecutionTrace.Type#CONTENT}, and
     *     {@link RuntimeExecutionTrace#getReference()} is guaranteed non-empty.
     * @param absoluteTrace the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(trace)}
     * @param upperBound upper bound on the index that will be returned; may be null if there is no upper bound
     * @return maximum index present at the given execution trace or an empty {@link Option} if no index exists
     * @throws IOException if an I/O error occurs
     */
    protected abstract Option<Index> getMaximumIndex(RuntimeExecutionTrace trace,
        RuntimeAnnotatedExecutionTrace absoluteTrace, @Nullable Index upperBound) throws IOException;

    @Override
    public final Future<Option<Index>> getMaximumIndex(RuntimeExecutionTrace trace, @Nullable Index upperBound) {
        if (trace.getType() != RuntimeExecutionTrace.Type.CONTENT) {
            throw new IllegalArgumentException(String.format(
                "Expected relative trace of type %s, but got '%s'.", RuntimeExecutionTrace.Type.CONTENT, trace
            ));
        }
        Objects.requireNonNull(upperBound);
        RuntimeAnnotatedExecutionTrace absoluteTrace = executionTrace.resolveExecutionTrace(trace);

        return toFuture(
            () -> getMaximumIndex(trace, absoluteTrace, upperBound), "determine maximum index at %s", absoluteTrace
        );
    }

    /**
     * Returns a staging area for the given descendant execution trace.
     *
     * <p>The execution trace is guaranteed to:
     * <ul><li>
     *     be non-empty,
     * </li><li>
     *     be of type {@link RuntimeExecutionTrace.Type#MODULE} or {@link RuntimeExecutionTrace.Type#ITERATION},
     * </li><li>
     *     have an empty value reference.
     * </li></ul>
     *
     * @param trace execution trace with the guarantees documented above
     * @param absoluteTrace the result of {@code getAnnotatedExecutionTrace().resolveExecutionTrace(trace)}
     * @return the staging area for the given descendant execution trace
     */
    protected abstract AbstractStagingArea resolveDescendant(RuntimeExecutionTrace trace,
        RuntimeAnnotatedExecutionTrace absoluteTrace);

    @Override
    public final AbstractStagingArea resolveDescendant(RuntimeExecutionTrace trace) {
        if (!trace.getReference().isEmpty()) {
            throw new IllegalArgumentException(String.format("Expected relative trace with empty reference, " +
                "but got %s.", trace));
        }
        RuntimeAnnotatedExecutionTrace absoluteTrace = executionTrace.resolveExecutionTrace(trace);

        return trace.isEmpty()
            ? this
            : resolveDescendant(trace, absoluteTrace);
    }

    /**
     * Verifies that {@link StagingArea#getStagingAreaProvider()} may be called and throws the mandated exceptions if
     * not.
     *
     * @throws IllegalStateException if the conditions mandated by {@link StagingArea#getStagingAreaProvider()} are not
     *     satisfied
     */
    protected final void requireValidRequestForProvider() {
        if (!executionTrace.isEmpty()) {
            // A non-empty empty execution trace is only allowed if the execution trace represents a simple module!
            if (executionTrace.getType() == RuntimeExecutionTrace.Type.MODULE) {
                RuntimeModule module = executionTrace.getModule();
                if (module instanceof RuntimeProxyModule
                        && ((BareProxyModule) module).getDeclaration() instanceof RuntimeSimpleModuleDeclaration) {
                    // OK, all is good.
                    return;
                }
            }

            throw new IllegalStateException(String.format(
                "Execution trace '%s' is neither empty nor does it represent a simple module.", executionTrace
            ));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementations should call {@link #requireValidRequestForProvider()} to verify that the conditions mandated
     * by {@link StagingArea} hold.
     */
    @Override
    public abstract StagingAreaProvider getStagingAreaProvider();

    /**
     * Returns a new marshaling tree representing the same marshaled object as the given node.
     *
     * @param node root node of the persistence tree
     * @return the new marshaling tree representing the same marshaled object as the given node
     */
    protected static ObjectNode marshalingTree(RuntimeSerializationRoot node) {
        @Nullable ObjectNode sourceTree = (ObjectNode) node.accept(ConversionVisitor.INSTANCE, null);
        assert sourceTree != null;
        return sourceTree;
    }

    private enum ConversionVisitor implements RuntimeSerializationNodeVisitor<MarshalingTreeNode, Void> {
        INSTANCE;

        @Override
        public ObjectNode visitRoot(RuntimeSerializationRoot root, @Nullable Void ignored) {
            return root.getEntries().get(0).getKey() instanceof NoKey
                ? new MarshaledReplacementObjectNodeImpl(root)
                : new MarshaledObjectNodeImpl(root);
        }

        @Override
        public ByteSequenceNode visitByteSequence(RuntimeByteSequence byteSequence, @Nullable Void ignored) {
            return new ByteSequenceNodeImpl(byteSequence);
        }

        @Override
        public RawObjectNodeImpl visitString(RuntimeSerializedString serializedString, @Nullable Void ignored) {
            return new RawObjectNodeImpl(serializedString);
        }
    }

    private static class MarshaledObjectNodeImpl implements MarshaledObjectNode {
        private final RuntimeSerializationRoot originalNode;

        private MarshaledObjectNodeImpl(RuntimeSerializationRoot originalNode) {
            this.originalNode = originalNode;
        }

        @Override
        public Marshaler<?> getMarshaler() {
            return originalNode.getDeclaration().getInstance();
        }

        @Override
        public Map<Key, MarshalingTreeNode> getChildren() {
            return originalNode.getEntries().stream()
                .collect(Collectors.toMap(
                    RuntimeSerializationNode::getKey,
                    node -> node.accept(ConversionVisitor.INSTANCE, null)
                ));
        }
    }

    private static class MarshaledReplacementObjectNodeImpl implements MarshaledReplacementObjectNode {
        private final RuntimeSerializationRoot originalNode;

        private MarshaledReplacementObjectNodeImpl(RuntimeSerializationRoot originalNode) {
            this.originalNode = originalNode;
        }

        @Override
        public Marshaler<?> getMarshaler() {
            return originalNode.getDeclaration().getInstance();
        }

        @Override
        public MarshalingTreeNode getChild() {
            @Nullable RuntimeSerializationNode originalChild = originalNode.getEntry(NoKey.instance());
            assert originalChild != null;
            @Nullable MarshalingTreeNode child = originalChild.accept(ConversionVisitor.INSTANCE, null);
            assert child != null;
            return child;
        }
    }

    private static class RawObjectNodeImpl implements RawObjectNode {
        private final RuntimeSerializedString originalNode;

        private RawObjectNodeImpl(RuntimeSerializedString originalNode) {
            this.originalNode = originalNode;
        }

        @Nullable
        @Override
        public Marshaler<?> getMarshaler() {
            return null;
        }

        @Override
        public String getObject() {
            return originalNode.getString();
        }
    }

    private static class ByteSequenceNodeImpl implements ByteSequenceNode {
        private final RuntimeByteSequence originalNode;

        private ByteSequenceNodeImpl(RuntimeByteSequence originalNode) {
            this.originalNode = originalNode;
        }

        @Override
        public ByteSequence getByteSequence() {
            return originalNode.toByteSequence();
        }
    }
}
