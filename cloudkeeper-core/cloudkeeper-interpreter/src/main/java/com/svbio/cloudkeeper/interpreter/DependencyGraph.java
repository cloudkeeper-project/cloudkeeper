package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeConnection;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeIOPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimePort;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Dependency graph as described in the CloudKeeper design document.
 *
 * <p>The nodes in the graph are available through {@link #nodeStream()}, the edges for each node are available through
 * {@link DependencyGraphNode#predecessorStream()} and {@link DependencyGraphNode#successorStream()}.
 */
final class DependencyGraph {
    /**
     * The parent module this dependency graph represents.
     */
    private final RuntimeParentModule parentModule;

    /**
     * Array that contains the dependency-graph nodes corresponding to all in-ports (of {@link #parentModule}).
     */
    private final InPortNode[] inPortNodes;
    private final List<InPortNode> inPortNodeList;

    /**
     * Array that contains the dependency-graph nodes corresponding to all out-ports (of {@link #parentModule}).
     */
    private final OutPortNode[] outPortNodes;
    private final List<OutPortNode> outPortNodeList;

    /**
     * Array that contains the dependency-graph nodes corresponding to all submodules (of {@link #parentModule}).
     */
    private final SubmoduleNode[] submoduleNodes;
    private final List<SubmoduleNode> submoduleNodeList;

    /**
     * Array that contains the dependency-graph nodes corresponding to all submodules' in-ports.
     */
    private final SubmoduleInPortNode[][] submodulesInPortNodes;
    private final List<List<SubmoduleInPortNode>> submodulesInPortNodesList;

    /**
     * Set containing the indices of the requested out-ports.
     */
    private final BitSet requestedOutPorts;

    DependencyGraph(RuntimeParentModule parentModule, BitSet requestedOutPorts) {
        if (requestedOutPorts.length() > parentModule.getOutPorts().size()) {
            throw new IllegalArgumentException(String.format(
                "Expected list of out-port indices, but got %s. The number of out-ports is only %s.",
                requestedOutPorts, parentModule.getOutPorts().size()
            ));
        }

        this.parentModule = parentModule;
        this.requestedOutPorts = (BitSet) requestedOutPorts.clone();

        List<? extends RuntimeModule> submodules = parentModule.getModules();
        int numSubmodules = submodules.size();
        int numInPorts = parentModule.getInPorts().size();

        inPortNodes = new InPortNode[numInPorts];
        inPortNodeList = Collections.unmodifiableList(Arrays.asList(inPortNodes));
        for (RuntimeInPort inPort: parentModule.getInPorts()) {
            inPortNodes[inPort.getInIndex()] = new InPortNode(inPort);
        }

        List<? extends RuntimeOutPort> outPorts = parentModule.getOutPorts();
        outPortNodes = new OutPortNode[outPorts.size()];
        outPortNodeList = Collections.unmodifiableList(Arrays.asList(outPortNodes));
        for (RuntimeOutPort outPort: outPorts) {
            outPortNodes[outPort.getOutIndex()] = new OutPortNode(outPort);
        }

        submoduleNodes = new SubmoduleNode[numSubmodules];
        submoduleNodeList = Collections.unmodifiableList(Arrays.asList(submoduleNodes));

        submodulesInPortNodes = new SubmoduleInPortNode[numSubmodules][];
        List<List<SubmoduleInPortNode>> newSubmodulesInPortNodesList = new ArrayList<>(submodules.size());
        for (RuntimeModule submodule: submodules) {
            int submoduleId = submodule.getIndex();
            submoduleNodes[submoduleId] = new SubmoduleNode(submodule);

            List<? extends RuntimeInPort> submoduleInPorts = submodule.getInPorts();
            submodulesInPortNodes[submoduleId] = new SubmoduleInPortNode[submoduleInPorts.size()];
            newSubmodulesInPortNodesList.add(
                Collections.unmodifiableList(Arrays.asList(submodulesInPortNodes[submoduleId]))
            );
            for (RuntimeInPort inPort: submoduleInPorts) {
                submodulesInPortNodes[submoduleId][inPort.getInIndex()] = new SubmoduleInPortNode(inPort);
            }
        }
        submodulesInPortNodesList = Collections.unmodifiableList(newSubmodulesInPortNodesList);

        computeOnPathToOutPort(requestedOutPorts);
    }

    RuntimeParentModule getParentModule() {
        return parentModule;
    }

    BitSet getRequestedOutPorts() {
        return (BitSet) requestedOutPorts.clone();
    }

    /**
     * Returns a {@link Stream} of all nodes in the dependency graph, including those for which
     * {@link DependencyGraphNode#isOnPathToOutPort()} returns false.
     */
    Stream<DependencyGraphNode> nodeStream() {
        return Stream.concat(
            Stream.of(inPortNodes, outPortNodes, submoduleNodes)
                .map(Stream::of)
                .flatMap(x -> x),
            Stream.of(submodulesInPortNodes)
                .flatMap(Stream::of)
        );
    }

    /**
     * Set {@link DependencyGraphNode#onPathToOutPort} to {@code true} for all nodes in the dependency graph that have a
     * path to an out-port whose index is contained in {@code requestedOutPorts}.
     *
     * <p>The purpose of this method is to weed out all those nodes in the dependency graph that have no effect on the
     * requested out-ports of the current parent module. Here, “no effect” means that there is no path from such a node
     * to {@link OutPortNode} whose out-port index (that is, {@link RuntimeOutPort#getOutIndex()}) is contained in
     * {@code requestedOutPorts}.
     *
     * <p>This method performs a breadth-first-search starting from all out-port nodes in {@code requestedOutPorts}.
     */
    private void computeOnPathToOutPort(BitSet requestedOutPorts) {
        assert requestedOutPorts.length() <= outPortNodes.length;

        Deque<DependencyGraphNode> queue = new ArrayDeque<>();
        requestedOutPorts.stream()
            .<DependencyGraphNode>mapToObj(id -> outPortNodes[id])
            .forEach(node -> {
                node.onPathToOutPort = true;
                queue.add(node);
            });
        while (!queue.isEmpty()) {
            DependencyGraphNode node = queue.poll();
            node.unprunedPredecessorStream()
                .filter(predecessor -> !predecessor.isOnPathToOutPort())
                .forEach(predecessor -> {
                    predecessor.onPathToOutPort = true;
                    queue.add(predecessor);
                });
        }
    }

    /**
     * Returns the node in the dependency graph that corresponds to the given port.
     *
     * @param port in-port or out-port of the current module, or a submodule's in-port
     * @param preferInPortNode only relevant if {@code port} is an {@link RuntimeIOPort}: in that case {@code true}
     *     indicates that the corresponding {@link InPortNode} should be returned, {@code false} indicates that the
     *     {@link OutPortNode} is requested
     * @return the node in the dependency graph
     * @throws IllegalArgumentException if the given port neither belongs to the parent module represented by this
     *     dependency graph nor to any of its submodules
     */
    private ValueNode node(RuntimePort port, boolean preferInPortNode) {
        Objects.requireNonNull(port);

        RuntimeModule portModule = port.getModule();
        if (portModule == parentModule) {
            boolean isInPortNode = port instanceof RuntimeIOPort
                ? preferInPortNode
                : port instanceof RuntimeInPort;
            if (isInPortNode) {
                return inPortNodes[((RuntimeInPort) port).getInIndex()];
            } else {
                return outPortNodes[((RuntimeOutPort) port).getOutIndex()];
            }
        } else if (portModule.getParent() == parentModule) {
            return submodulesInPortNodes[portModule.getIndex()][((RuntimeInPort) port).getInIndex()];
        }

        throw new IllegalArgumentException(String.format(
            "Expected in-port, out-port, or a submodule's in-port. However, got %s.", port
        ));
    }

    /**
     * Returns the node in the dependency graph that corresponds to the given port; returns an {@link InPortNode} if
     * the given port is an i/o-port of the current parent module.
     *
     * @param port port of the current module or in-port of a submodule
     * @return the node in the dependency graph
     * @throws IllegalArgumentException if the given port neither belongs to the parent module represented by this
     *     dependency graph nor to any of its submodules
     *
     * @see #targetNode(RuntimePort)
     */
    ValueNode sourceNode(RuntimePort port) {
        return node(port, true);
    }

    /**
     * Returns the node in the dependency graph that corresponds to the given port; returns an {@link OutPortNode} if
     * the given port is an i/o-port of the current parent module.
     *
     * @param port port of the current module or in-port of a submodule
     * @return the node in the dependency graph
     * @throws IllegalArgumentException if the given port neither belongs to the parent module represented by this
     *     dependency graph nor to any of its submodules
     *
     * @see #sourceNode(RuntimePort)
     */
    ValueNode targetNode(RuntimePort port) {
        return node(port, false);
    }

    /**
     * Returns the node in the dependency graph that corresponds to the given submodule.
     *
     * @param submodule submodule of the current module
     * @return the node in the dependency graph
     * @throws IllegalArgumentException if the given module is not a submodule of the parent module represented by this
     *     dependency graph
     */
    SubmoduleNode submodule(RuntimeModule submodule) {
        Objects.requireNonNull(submodule);
        if (submodule.getParent() != parentModule) {
            throw new IllegalArgumentException(String.format("Expected submodule, but got %s.", submodule));
        }

        return submoduleNodes[submodule.getIndex()];
    }

    /**
     * Returns the (unmodifiable) list of in-port nodes, <em>including</em> those that have no edges in the pruned
     * dependency graph.
     */
    List<InPortNode> inPortNodes() {
        return inPortNodeList;
    }

    /**
     * Returns the (unmodifiable) list of out-port nodes, <em>including</em> those that have no edges in the pruned
     * dependency graph.
     */
    List<OutPortNode> outPortNodes() {
        return outPortNodeList;
    }

    /**
     * Returns the (unmodifiable) list of submodule nodes, <em>including</em> those that have no edges in the pruned
     * dependency graph.
     */
    List<SubmoduleNode> submoduleNodes() {
        return submoduleNodeList;
    }

    /**
     * Returns the (unmodifiable) list that, for each submodule, contains an (unmodifiable) list of in-port nodes. This
     * <em>includes</em> those that have no edges in the pruned dependency graph.
     */
    List<List<SubmoduleInPortNode>> submodulesInPortNodes() {
        return submodulesInPortNodesList;
    }

    /**
     * Predicate that indicates whether a connection belongs to the current parent module.
     *
     * <p>Note that even connections outgoing from in-ports of the current module may not belong to this parent module.
     * For instance, the port maybe an I/O-port, in which case it may have an outgoing connections to the parent of the
     * current parent module.
     */
    private boolean isWithinParent(RuntimeConnection connection) {
        return connection.getParentModule() == parentModule;
    }

    /**
     * Returns a stream of all nodes in the dependency graph that correspond to the target port of the given
     * connections.
     *
     * <p>All elements in the stream are guaranteed to be distinct.
     */
    private Stream<ValueNode> nodesOfConnectionTargets(Stream<? extends RuntimeConnection> connections) {
        return connections.filter(this::isWithinParent)
            .map(RuntimeConnection::getToPort)
            .map(this::targetNode)
            .distinct();
    }

    /**
     * Returns a stream of all nodes in the dependency graph that have any of the source ports of the given connections
     * as witness.
     *
     * <p>All elements in the stream are guaranteed to be distinct.
     */
    private Stream<DependencyGraphNode> nodesOfConnectionSources(Stream<? extends RuntimeConnection> connections) {
        return connections
            .filter(this::isWithinParent)
            .map(RuntimeConnection::getFromPort)
            .flatMap(fromPort -> {
                if (fromPort.getModule() == parentModule) {
                    return Stream.of(sourceNode(fromPort));
                } else {
                    RuntimeOutPort outPort = (RuntimeOutPort) fromPort;
                    return Stream.concat(
                        Stream.of(submodule(outPort.getModule())),
                        outPort.getInPortDependencies().stream()
                            .map(this::sourceNode)
                    );
                }
            })
            .distinct();
    }

    /**
     * Available information about whether a node has a value or not.
     */
    enum HasValue {
        /**
         * It is currently unknown whether the node has a value. No asynchronous call has been issued in order to update
         * this status.
         *
         * <p>Only for a {@link ValueNode} it can be unknown whether it has a value present. In this case,
         * {@link ValueNode#setHasValue(HasValue)} can (only) be called with {@link #PENDING_VALUE_CHECK} as argument.
         */
        UNKNOWN,

        /**
         * It is currently unknown whether the node has a value. However, an asynchronous call has been issued to update
         * this status.
         *
         * <p>Only for a {@link ValueNode} it can be unknown whether it has a value present. In this case,
         * {@link ValueNode#setHasValue(HasValue)} can (only) be called with {@link #HAS_VALUE} or {@link #NO_VALUE} as
         * argument.
         */
        PENDING_VALUE_CHECK,

        /**
         * The node has a value.
         */
        HAS_VALUE,

        /**
         * The node does not have a value.
         */
        NO_VALUE
    }

    /**
     * State of a node in the dependency graph when computing the resume state for algorithm
     * <em>ComputeResumeState</em>.
     */
    enum PortState {
        /**
         * This port is irrelevant because a downstream port has a value that can be used to resume interpretation.
         */
        IRRELEVANT,

        /**
         * There is a value at this port, and it will be used to resume the interpretation.
         */
        READY,

        /**
         * The value at this port will be recomputed.
         */
        RECOMPUTE
    }

    /**
     * Node in the dependency graph.
     *
     * <p>Note that instances of this class are mutable, hence they should not be used in hash-based collections and
     * maps.
     */
    abstract static class DependencyGraphNode {
        private final ExecutionTrace executionTrace;

        /**
         * The state of this node.
         */
        private PortState portState = PortState.IRRELEVANT;

        /**
         * Whether this node has a path to a node of type {@link OutPortNode}.
         */
        private boolean onPathToOutPort = false;

        @Override
        public boolean equals(@Nullable Object otherObject) {
            if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            DependencyGraphNode other = (DependencyGraphNode) otherObject;
            return portState == other.portState
                && onPathToOutPort == other.onPathToOutPort;
        }

        @Override
        public int hashCode() {
            return Objects.hash(portState, onPathToOutPort);
        }

        @Override
        public abstract String toString();

        private DependencyGraphNode(ExecutionTrace executionTrace) {
            this.executionTrace = executionTrace;
        }

        final ExecutionTrace getExecutionTrace() {
            return executionTrace;
        }

        abstract RuntimeElement getElement();

        final PortState getPortState() {
            return portState;
        }

        final void setPortState(PortState portState) {
            Objects.requireNonNull(portState);
            this.portState = portState;
        }

        final boolean isOnPathToOutPort() {
            return onPathToOutPort;
        }

        /**
         * Returns currently available information about whether this port has a value.
         */
        abstract HasValue getHasValue();

        /**
         * Returns a stream of all nodes in the <em>unpruned</em> dependency graph that have an outgoing edge to the
         * current node.
         */
        abstract Stream<DependencyGraphNode> unprunedPredecessorStream();

        /**
         * Returns a stream of all nodes in the <em>unpruned</em> dependency graph that have an incoming edge from this
         * node.
         */
        abstract Stream<ValueNode> unprunedSuccessorStream();

        /**
         * Returns a stream of all nodes in the dependency graph that have an outgoing edge to the current node.
         *
         * <p>For all nodes in the stream {@link DependencyGraphNode#isOnPathToOutPort()} returns {@code true}.
         * Moreover, all nodes are of type {@link InPortNode}, {@link SubmoduleInPortNode}, or {@link SubmoduleNode},
         * but never of type {@link OutPortNode}. Unless a predecessor node represents an in-port of the current module,
         * there is a "witness" submodule out-port that established the link.
         */
        final Stream<DependencyGraphNode> predecessorStream() {
            return onPathToOutPort
                ? unprunedPredecessorStream().filter(DependencyGraphNode::isOnPathToOutPort)
                : Stream.of();
        }

        /**
         * Returns a stream of all nodes in the dependency graph that have an incoming edge from this node.
         *
         * <p>For all nodes in the returned stream {@link ValueNode#isOnPathToOutPort()} returns {@code true}. Moreover,
         * all nodes are of type {@link SubmoduleInPortNode} or {@link OutPortNode}, but never of type
         * {@link InPortNode} or {@link SubmoduleNode}. Unless this node is an {@link InPortNode}, there is a "witness"
         * submodule out-port that establishes the link.
         */
        final Stream<ValueNode> successorStream() {
            return onPathToOutPort
                ? unprunedSuccessorStream().filter(ValueNode::isOnPathToOutPort)
                : Stream.of();
        }

        /**
         * Returns an {@link Iterable} corresponding to {@link #predecessorStream()}.
         */
        final Iterable<DependencyGraphNode> predecessors() {
            return predecessorStream()::iterator;
        }

        /**
         * Returns an {@link Iterable} corresponding to {@link #successorStream()}.
         */
        final Iterable<ValueNode> successors() {
            return successorStream()::iterator;
        }
    }

    final class SubmoduleNode extends DependencyGraphNode {
        private final RuntimeModule submodule;

        private SubmoduleNode(RuntimeModule submodule) {
            super(ExecutionTrace.empty().resolveContent().resolveModule(submodule.getSimpleName()));
            this.submodule = submodule;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject) && submodule.equals(((SubmoduleNode) otherObject).submodule)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + submodule.hashCode();
        }

        @Override
        public String toString() {
            return String.format("submodule '%s' (%s)", submodule.getSimpleName(), getPortState());
        }

        @Override
        RuntimeModule getElement() {
            return submodule;
        }

        @Override
        HasValue getHasValue() {
            return HasValue.HAS_VALUE;
        }

        @Override
        Stream<DependencyGraphNode> unprunedPredecessorStream() {
            return Stream.of();
        }

        @Override
        Stream<ValueNode> unprunedSuccessorStream() {
            return nodesOfConnectionTargets(
                submodule.getOutPorts().stream()
                    .flatMap(outPort -> outPort.getOutConnections().stream())
            );
        }
    }

    abstract class ValueNode extends DependencyGraphNode {
        private HasValue hasValue = HasValue.UNKNOWN;

        ValueNode(ExecutionTrace executionTrace) {
            super(executionTrace);
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return super.equals(otherObject) && hasValue == ((ValueNode) otherObject).hasValue;
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + hasValue.hashCode();
        }

        @Override
        public final String toString() {
            StringBuilder stringBuilder = new StringBuilder(32);
            addName(stringBuilder);
            stringBuilder.append(" (").append(getPortState()).append(", ").append(hasValue).append(')');
            return stringBuilder.toString();
        }

        abstract void addName(StringBuilder stringBuilder);

        @Override
        abstract RuntimePort getElement();

        final void setHasValue(HasValue hasValue) {
            if (
                this.hasValue == hasValue
                || this.hasValue == HasValue.UNKNOWN
                || (this.hasValue == HasValue.PENDING_VALUE_CHECK
                        && (hasValue == HasValue.NO_VALUE || hasValue == HasValue.HAS_VALUE))
            ) {
                this.hasValue = hasValue;
            } else {
                throw new IllegalArgumentException(String.format(
                    "Tried to change property 'hasValue' from %s to %s.", this.hasValue, hasValue
                ));
            }
        }

        @Override
        final HasValue getHasValue() {
            return hasValue;
        }
    }

    final class InPortNode extends ValueNode {
        private final RuntimeInPort inPort;

        private InPortNode(RuntimeInPort inPort) {
            super(ExecutionTrace.empty().resolveInPort(inPort.getSimpleName()));
            this.inPort = inPort;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject) && inPort.equals(((InPortNode) otherObject).inPort)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + inPort.hashCode();
        }

        @Override
        RuntimeInPort getElement() {
            return inPort;
        }

        @Override
        void addName(StringBuilder stringBuilder) {
            stringBuilder.append("in-port '").append(inPort.getSimpleName()).append('\'');
        }

        @Override
        Stream<DependencyGraphNode> unprunedPredecessorStream() {
            return Stream.of();
        }

        @Override
        Stream<ValueNode> unprunedSuccessorStream() {
            return nodesOfConnectionTargets(inPort.getOutConnections().stream());
        }
    }

    final class OutPortNode extends ValueNode {
        private final RuntimeOutPort outPort;

        private OutPortNode(RuntimeOutPort outPort) {
            super(ExecutionTrace.empty().resolveOutPort(outPort.getSimpleName()));
            this.outPort = outPort;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject) && outPort.equals(((OutPortNode) otherObject).outPort)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + outPort.hashCode();
        }

        @Override
        RuntimeOutPort getElement() {
            return outPort;
        }

        @Override
        void addName(StringBuilder stringBuilder) {
            stringBuilder.append("out-port '").append(outPort.getSimpleName()).append('\'');
        }

        @Override
        Stream<ValueNode> unprunedSuccessorStream() {
            return Stream.of();
        }

        @Override
        Stream<DependencyGraphNode> unprunedPredecessorStream() {
            return nodesOfConnectionSources(outPort.getInConnections().stream());
        }
    }

    final class SubmoduleInPortNode extends ValueNode {
        private final RuntimeInPort inPort;

        private SubmoduleInPortNode(RuntimeInPort inPort) {
            super(
                ExecutionTrace.empty()
                    .resolveContent()
                    .resolveModule(inPort.getModule().getSimpleName())
                    .resolveInPort(inPort.getSimpleName())
            );
            this.inPort = inPort;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                super.equals(otherObject) && inPort.equals(((SubmoduleInPortNode) otherObject).inPort)
            );
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + inPort.hashCode();
        }

        @Override
        RuntimeInPort getElement() {
            return inPort;
        }

        @Override
        void addName(StringBuilder stringBuilder) {
            stringBuilder
                .append("submodule in-port '").append(inPort.getModule().getSimpleName())
                .append('#').append(inPort.getSimpleName()).append('\'');
        }

        @Override
        Stream<DependencyGraphNode> unprunedPredecessorStream() {
            return nodesOfConnectionSources(inPort.getInConnections().stream());
        }

        @Override
        Stream<ValueNode> unprunedSuccessorStream() {
            return nodesOfConnectionTargets(
                inPort.getDependentOutPorts().stream()
                    .flatMap(outPort -> outPort.getOutConnections().stream())
            );
        }
    }
}
