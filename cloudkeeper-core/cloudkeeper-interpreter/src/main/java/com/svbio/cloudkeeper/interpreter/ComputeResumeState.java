package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.interpreter.DependencyGraph.DependencyGraphNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.HasValue;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.InPortNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.OutPortNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.PortState;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.SubmoduleInPortNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.SubmoduleNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.ValueNode;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeConnection;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimePort;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Class that implements the <em>ComputeResumeState</em> algorithm.
 *
 * <p>Since the algorithm makes asynchronous calls, instances of this class have a lifecycle.
 */
final class ComputeResumeState {
    private final DependencyGraph dependencyGraph;

    private final BitSet recomputedInPorts;

    /**
     * Queue that contains all dependency graph nodes that had state {@link PortState#READY} when they were enqueued as
     * part of the <em>ComputeResumeState</em> algorithm.
     *
     * <p>When {@link #dequeue()} is called, the state no longer needs to be {@link PortState#READY}. See
     * {@link #dequeue()} for an explanation why there are two {@link Deque} instances in this implementation.
     */
    private final Deque<DependencyGraphNode> readyQueue = new ArrayDeque<>();

    /**
     * Queue that contains all dependency graph nodes that had state {@link PortState#RECOMPUTE} when they were
     * enqueued as part of the <em>ComputeResumeState</em> algorithm.
     *
     * <p> See {@link #dequeue()} for an explanation why there are two {@link Deque} instances in this implementation.
     */
    private final Deque<DependencyGraphNode> recomputeQueue = new ArrayDeque<>();

    /**
     * Operation that is triggered in order to check was a {@link ValueNode} has a value.
     *
     * <p>It is expected that after invoking the operation, {@link #updateHasValue(ValueNode, boolean)}
     * will be eventually be called for that {@link ValueNode}. It is guaranteed that {@link ValueNode#getHasValue()} is
     * {@link HasValue#PENDING_VALUE_CHECK} at the time the operation is called.
     */
    private final Consumer<ValueNode> checkHasValue;

    /**
     * Array of sets, one for each submodule, containing the indices of all out-ports that need to be computed.
     */
    private final BitSet[] submodulesNeededOutPorts;

    /**
     * Number of asynchronous operations (during the <em>ComputeResumeState</em> algorithm).
     */
    private int numAsynchronousCalls = 0;


    ComputeResumeState(DependencyGraph dependencyGraph, BitSet recomputedInPorts, Consumer<ValueNode> checkHasValue) {
        this.dependencyGraph = dependencyGraph;
        this.recomputedInPorts = (BitSet) recomputedInPorts.clone();
        this.checkHasValue = checkHasValue;

        recomputedInPorts.stream()
            .mapToObj(id -> dependencyGraph.inPortNodes().get(id))
            .filter(DependencyGraphNode::isOnPathToOutPort)
            .forEach(node -> {
                node.setPortState(PortState.RECOMPUTE);
                recomputeQueue.add(node);
            });
        dependencyGraph.getRequestedOutPorts().stream()
            .mapToObj(id -> dependencyGraph.outPortNodes().get(id))
            .forEach(node -> {
                assert node.isOnPathToOutPort();
                node.setPortState(PortState.READY);
                readyQueue.add(node);
            });

        List<? extends RuntimeModule> submodules = dependencyGraph.getParentModule().getModules();
        submodulesNeededOutPorts = new BitSet[submodules.size()];
        for (RuntimeModule submodule: submodules) {
            submodulesNeededOutPorts[submodule.getIndex()] = new BitSet(submodule.getOutPorts().size());
        }
    }

    private boolean isEmpty() {
        return recomputeQueue.isEmpty() && readyQueue.isEmpty();
    }

    /**
     * Returns the first element in the combined queue.
     *
     * <p>This method always returns a node in the {@link #recomputeQueue} if there is one. This serves as optimization:
     * For nodes with state {@link PortState#READY}, the algorithm would start an asynchronous task (see method
     * {@link #run()}), even though that may not be necessary if the {@link PortState#RECOMPUTE} state would have
     * propagated first through the dependency graph.
     */
    private DependencyGraphNode dequeue() {
        @Nullable DependencyGraphNode node = recomputeQueue.poll();
        if (node == null) {
            node = readyQueue.poll();
        }
        return node;
    }

    /**
     * Marks all submodule out-ports that "witness" that there is a edge between {@code start} and {@code end} in the
     * dependency graph.
     *
     * <p>If {@code start} does not represent an in-port in a submodule, then this method is a no-op.
     *
     * @param start start node of edge, must be of type {@link InPortNode}, {@link SubmoduleInPortNode},
     *     or {@link SubmoduleNode}
     * @param end end node of edge, must be of type {@link SubmoduleInPortNode} or {@link OutPortNode}
     */
    private void addNeededOutPorts(DependencyGraphNode start, ValueNode end) {
        assert start instanceof InPortNode || start instanceof SubmoduleInPortNode || start instanceof SubmoduleNode;
        assert end instanceof SubmoduleInPortNode || end instanceof OutPortNode;

        List<? extends RuntimeOutPort> submoduleOutPorts;
        if (start instanceof InPortNode) {
            submoduleOutPorts = Collections.emptyList();
        } else if (start instanceof SubmoduleInPortNode) {
            submoduleOutPorts = ((SubmoduleInPortNode) start).getElement().getDependentOutPorts();
        } else {
            // start instanceof SubmoduleNode
            submoduleOutPorts = ((SubmoduleNode) start).getElement().getOutPorts();
        }

        RuntimePort toPort = end.getElement();

        for (RuntimeOutPort submoduleOutPort: submoduleOutPorts) {
            for (RuntimeConnection connection: submoduleOutPort.getOutConnections()) {
                if (connection.getToPort() == toPort) {
                    submodulesNeededOutPorts[submoduleOutPort.getModule().getIndex()]
                        .set(submoduleOutPort.getOutIndex());
                }
            }
        }
    }

    /**
     * Returns whether the given dependency-graph node has a value, triggering an asynchronous call in order to find out
     * if this is currently unknown.
     *
     * <p>If {@code node} is a {@link ValueNode} and {@link ValueNode#getHasValue()} returns {@link HasValue#UNKNOWN},
     * this method will call the {@link Consumer} passed to constructor
     * {@link #ComputeResumeState(DependencyGraph, BitSet, Consumer)}. It is required that this results in a
     * corresponding call to {@link #updateHasValue(ValueNode, boolean)}.
     */
    private HasValue computeHasValue(DependencyGraphNode node) {
        HasValue hasValue = node.getHasValue();
        if (hasValue == HasValue.UNKNOWN) {
            assert node instanceof ValueNode;
            ValueNode valueNode = (ValueNode) node;
            hasValue = HasValue.PENDING_VALUE_CHECK;
            valueNode.setHasValue(hasValue);
            ++numAsynchronousCalls;
            checkHasValue.accept(valueNode);
        }
        return hasValue;
    }

    /**
     * Continue running the <em>ComputeResumeState</em> as documented in the CloudKeeper design document.
     *
     * <p>This method performs a modified breadth-first-search. When this method returns, the algorithm has not
     * necessarily finished, because asynchronous calls can have been made (using {@link #checkHasValue}). Callers must
     * ensure that {@link #isFinished()} returns {@code true} before using the results produced by the algorithm.
     *
     * <p>This method is a no-op if {@link #isFinished()} returns {@code true}.
     */
    void run() {
        while (!isEmpty()) {
            DependencyGraphNode node = dequeue();

            if (node.getPortState() == PortState.READY) {
                HasValue hasValue = computeHasValue(node);
                assert EnumSet.of(HasValue.PENDING_VALUE_CHECK, HasValue.HAS_VALUE, HasValue.NO_VALUE)
                    .contains(hasValue);

                if (hasValue == HasValue.PENDING_VALUE_CHECK) {
                    // We will not process this node right now. Instead, we will process the node once we known whether
                    // the node has a value.
                    continue;
                } else if (hasValue == HasValue.NO_VALUE) {
                    node.setPortState(PortState.RECOMPUTE);
                }
            }

            if (node.getPortState() == PortState.RECOMPUTE) {
                assert node instanceof ValueNode;
                for (DependencyGraphNode predecessor: node.predecessors()) {
                    if (predecessor.getPortState() == PortState.IRRELEVANT) {
                        predecessor.setPortState(PortState.READY);
                        readyQueue.add(predecessor);
                    }
                }
            }

            for (ValueNode successor: node.successors()) {
                addNeededOutPorts(node, successor);
                if (successor.getPortState() != PortState.RECOMPUTE) {
                    successor.setPortState(PortState.RECOMPUTE);
                    recomputeQueue.add(successor);
                }
            }
        }

        failOnInPortWithMissingValue();
    }

    /**
     * Throws an exception if interpretation of the parent module has no chance of succeeding because one or more
     * required in-ports would never receive a value.
     *
     * @throws IllegalStateException if the algorithm detects {@link InPortNode} with state {@link PortState#RECOMPUTE}
     *     even though they are not listed in {@link #recomputedInPorts}
     */
    private void failOnInPortWithMissingValue() {
        assert isEmpty() : "only expected to be called from run()";
        if (numAsynchronousCalls > 0) {
            return;
        }

        List<RuntimeInPort> inPortsMissingValue = dependencyGraph.inPortNodes().stream()
            .filter(inPortNode -> PortState.RECOMPUTE == inPortNode.getPortState())
            .filter(inPortNode -> !recomputedInPorts.get(inPortNode.getElement().getInIndex()))
            .map(InPortNode::getElement)
            .collect(Collectors.toList());
        if (!inPortsMissingValue.isEmpty()) {
            throw new IllegalStateException(String.format(
                "Expected the following in-ports to either have or receive a value, because they are required for "
                    + "computing the requested out-ports: %s", inPortsMissingValue
            ));
        }
    }

    /**
     * Handles information whether a {@link ValueNode} has or does not have a value present.
     *
     * <p>Note that if the state of the given dependency-graph node is not {@link PortState#READY} any more, then the
     * node was visited again after {@link ValueNode#getHasValue()} started the asynchronous staging-area operation
     * (when {@link ValueNode#getHasValue()} was called, the state was {@link PortState#READY}). In this case there is
     * no need to add the node to the queue again in this method.
     *
     * @param node node in the dependency graph
     * @param hasValue whether the node in the dependency graph has a value
     */
    void updateHasValue(ValueNode node, boolean hasValue) {
        Objects.requireNonNull(node);
        requireNotFinished();
        if (node.getHasValue() != HasValue.PENDING_VALUE_CHECK) {
            throw new IllegalArgumentException(String.format(
                "Expected node with state %s, but actual state of %s is %s.",
                HasValue.PENDING_VALUE_CHECK, node, node.getHasValue()
            ));
        }

        --numAsynchronousCalls;
        node.setHasValue(
            hasValue
                ? HasValue.HAS_VALUE
                : HasValue.NO_VALUE
        );
        if (node.getPortState() == PortState.READY) {
            readyQueue.add(node);
        }
        run();
    }

    private void requireNotFinished() {
        if (isFinished()) {
            throw new IllegalStateException(String.format(
                "Tried to modify instance of %s, even though algorithm already finished.", getClass()
            ));
        }
    }

    private void requireFinished() {
        if (!isFinished()) {
            throw new IllegalStateException(String.format(
                "Tried to query instance of %s, even though algorithm has not yet finished.", getClass()
            ));
        }
    }

    /**
     * Returns whether the algorithm has finished or whether another call of {@link #updateHasValue(ValueNode, boolean)}
     * is required.
     *
     * @return whether the algorithm has finished
     */
    boolean isFinished() {
        assert numAsynchronousCalls >= 0;
        return isEmpty() && numAsynchronousCalls == 0;
    }

    /**
     * Returns the array of sets, one for each submodule, containing the indices of all submodule out-ports that need to
     * be computed.
     *
     * @return The array of sets. This is the array associated with this instance, no defensive copy is created.
     *
     * @throws IllegalStateException if {@link #isFinished()} is false
     */
    BitSet[] getSubmodulesNeededOutPorts() {
        requireFinished();
        return submodulesNeededOutPorts;
    }
}
