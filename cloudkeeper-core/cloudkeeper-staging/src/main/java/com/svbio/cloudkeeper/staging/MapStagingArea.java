package com.svbio.cloudkeeper.staging;

import akka.dispatch.Futures;
import akka.japi.Option;
import com.svbio.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import com.svbio.cloudkeeper.model.api.ExecutionTraceNotFoundException;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider;
import com.svbio.cloudkeeper.model.api.staging.StagingException;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * Simple in-memory staging area backed by a sorted map with keys of type {@link ExecutionTrace}.
 *
 * <p>This staging-area implementation is particularly useful for debugging and testing because it keeps the entire
 * content of the staging area in a single sorted map. Moreover, this staging area performs all operation synchronously.
 *
 * <p>This implementation is, however, not optimized for performance.
 */
public final class MapStagingArea extends AbstractInMemoryStagingArea {
    private final Object monitor;
    private final SortedMap<ExecutionTrace, ObjectNode> objects;
    private final SynchronizedMap unmodifiableMap;

    public MapStagingArea(RuntimeContext runtimeContext, RuntimeAnnotatedExecutionTrace executionTrace) {
        this(runtimeContext, executionTrace, new Object(), new TreeMap<>());
    }

    private MapStagingArea(RuntimeContext runtimeContext, RuntimeAnnotatedExecutionTrace executionTrace, Object monitor,
            SortedMap<ExecutionTrace, ObjectNode> objects) {
        super(runtimeContext, executionTrace);
        this.monitor = monitor;
        this.objects = objects;
        unmodifiableMap = new SynchronizedMap();
    }

    @Override
    protected <T> Future<T> toFuture(Callable<T> callable, String format, Object... args) {
        try {
            return Futures.successful(callable.call());
        } catch (Exception exception) {
            return Futures.failed(new StagingException(
                String.format("Failed to %s.", String.format(format, args)),
                exception
            ));
        }
    }

    @Override
    protected void delete(RuntimeExecutionTrace prefix, RuntimeAnnotatedExecutionTrace absoluteAnnotatedPrefix) {
        ExecutionTrace absolutePrefix = ExecutionTrace.copyOf(absoluteAnnotatedPrefix);
        int absolutePrefixLength = absolutePrefix.size();
        @Nullable ExecutionTrace lastKey = null;

        synchronized (monitor) {
            for (ExecutionTrace currentKey: objects.tailMap(absolutePrefix).keySet()) {
                if (currentKey.size() < absolutePrefixLength
                        || !currentKey.subtrace(0, absolutePrefixLength).equals(absolutePrefix)) {
                    lastKey = currentKey;
                    break;
                }
            }

            if (lastKey == null) {
                objects.tailMap(absolutePrefix).clear();
            } else {
                objects.subMap(absolutePrefix, lastKey).clear();
            }
        }
    }

    @Override
    protected void preWrite(RuntimeExecutionTrace prefix, RuntimeAnnotatedExecutionTrace absolutePrefix)
            throws IOException {
        // AbstractStagingArea#delete() expects an execution trace without array indices, but not a problem in the
        // implementation in this class
        delete(prefix, absolutePrefix);
    }

    @Override
    protected NodeContext getNodeContext(RuntimeExecutionTrace target, RuntimeAnnotatedExecutionTrace absoluteTarget) {
        return new NodeContextImpl(ExecutionTrace.copyOf(absoluteTarget));
    }

    private final class NodeContextImpl implements NodeContext {
        private final ExecutionTrace executionTrace;

        private NodeContextImpl(ExecutionTrace executionTrace) {
            this.executionTrace = executionTrace;
        }

        @Override
        public NodeContext resolve(Index index) {
            return new NodeContextImpl(executionTrace.resolveArrayIndex(index));
        }

        @Override
        public void storeNode(ObjectNode node) {
            synchronized (monitor) {
                objects.put(executionTrace, node);
            }
        }
    }

    @Nullable
    private ObjectNode internalGetNode(ExecutionTrace absoluteSource) {
        synchronized (monitor) {
            return objects.get(absoluteSource);
        }
    }

    @Override
    protected ObjectNode getNode(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteAnnotatedSource)
            throws ExecutionTraceNotFoundException {
        ExecutionTrace absoluteSource = ExecutionTrace.copyOf(absoluteAnnotatedSource);
        @Nullable ObjectNode existing = internalGetNode(absoluteSource);
        if (existing == null) {
            throw new ExecutionTraceNotFoundException(absoluteSource);
        }
        return existing;
    }

    @Override
    protected void copy(RuntimeExecutionTrace source, RuntimeExecutionTrace target,
            RuntimeAnnotatedExecutionTrace absoluteSource, RuntimeAnnotatedExecutionTrace absoluteTarget)
            throws ExecutionTraceNotFoundException {
        synchronized (monitor) {
            objects.put(ExecutionTrace.copyOf(absoluteTarget), getNode(source, absoluteSource));
        }
    }

    @Override
    protected boolean exists(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource) {
        return internalGetNode(ExecutionTrace.copyOf(absoluteSource)) != null;
    }

    @Override
    protected Option<Index> getMaximumIndex(RuntimeExecutionTrace trace,
            RuntimeAnnotatedExecutionTrace absoluteAnnotatedTrace, @Nullable Index upperBound) {
        ExecutionTrace absoluteTrace = ExecutionTrace.copyOf(absoluteAnnotatedTrace);
        ExecutionTrace first = absoluteTrace.resolveArrayIndex(Index.index(0));
        Index lastIndex = upperBound == null
            ? Index.index(Integer.MAX_VALUE)
            : upperBound;
        ExecutionTrace last = absoluteTrace.resolveArrayIndex(lastIndex);
        Option<Index> optionalIndex;
        synchronized (monitor) {
            if (objects.containsKey(last)) {
                optionalIndex = Option.some(lastIndex);
            } else {
                SortedMap<ExecutionTrace, ObjectNode> searchMap = objects.subMap(first, last);
                optionalIndex = searchMap.isEmpty()
                    ? Option.<Index>none()
                    : Option.some(searchMap.lastKey().getIndex());
            }
        }
        return optionalIndex;
    }

    @Override
    protected AbstractInMemoryStagingArea resolveDescendant(RuntimeExecutionTrace trace,
            RuntimeAnnotatedExecutionTrace absoluteTrace, RuntimeContext runtimeContext) {
        return new MapStagingArea(runtimeContext, absoluteTrace, monitor, objects);
    }

    @Override
    public StagingAreaProvider getStagingAreaProvider() {
        requireValidRequestForProvider();
        throw new UnsupportedOperationException(String.format(
            "A %s instance cannot be serialized.", MapStagingArea.class.getSimpleName()
        ));
    }

    /**
     * Returns an unmodifiable view of the map backing this staging area.
     *
     * <p>The returned map provides "read-only" access. Query operations on the returned map "read through", and
     * attempts to modify the returned map, whether direct or via its collection views, result in an
     * {@link UnsupportedOperationException}.
     *
     * <p>The returned map contains <em>absolute</em> keys. That is, it returns the map initially created when the
     * public constructor {@link #MapStagingArea(RuntimeContext, RuntimeAnnotatedExecutionTrace)} was called. In
     * other words, an instance created using {@link #resolveDescendant(RuntimeExecutionTrace)} returns the same map
     * as the original staging area.
     *
     * <p>The intended use of this method is for testing and debugging.
     *
     * @return unmodifiable view of the map backing this staging area
     */
    public Map<ExecutionTrace, ObjectNode> toUnmodifiableMap() {
        return unmodifiableMap;
    }

    private final class SynchronizedMap extends AbstractMap<ExecutionTrace, ObjectNode> {
        @Override
        public Set<Entry<ExecutionTrace, ObjectNode>> entrySet() {
            return new SynchronizedSet();
        }
    }

    private final class SynchronizedSet extends AbstractSet<Map.Entry<ExecutionTrace, ObjectNode>> {
        @Override
        public Iterator<Map.Entry<ExecutionTrace, ObjectNode>> iterator() {
            return new SynchronizedIterator();
        }

        @Override
        public int size() {
            synchronized (monitor) {
                return objects.size();
            }
        }
    }

    private final class SynchronizedIterator implements Iterator<Map.Entry<ExecutionTrace, ObjectNode>> {
        private final Iterator<Map.Entry<ExecutionTrace, ObjectNode>> iterator;

        private SynchronizedIterator() {
            synchronized (monitor) {
                iterator = objects.entrySet().iterator();
            }
        }

        @Override
        public boolean hasNext() {
            synchronized (monitor) {
                return iterator.hasNext();
            }
        }

        @Override
        public Map.Entry<ExecutionTrace, ObjectNode> next() {
            synchronized (monitor) {
                return iterator.next();
            }
        }
    }
}
