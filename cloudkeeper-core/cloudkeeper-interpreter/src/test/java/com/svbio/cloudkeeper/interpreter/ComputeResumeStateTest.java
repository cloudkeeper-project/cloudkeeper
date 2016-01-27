package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.interpreter.DependencyGraph.DependencyGraphNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.HasValue;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.InPortNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.PortState;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.SubmoduleInPortNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.SubmoduleNode;
import com.svbio.cloudkeeper.interpreter.DependencyGraph.ValueNode;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeConnection;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ComputeResumeStateTest {
    /**
     * Composite-module context of the following form, where each child module has two in-ports {@code <name>x} and
     * {@code <name>y}. Submodule {@code a} has two out-ports {@code ap} and {@code aq}, submodule {@code b} has one
     * out-port {@code bp}, and submodule {@code c} has no out-ports.
     *
     * {@code
     *     ------------
     * x --|------ b ---- p
     *     | \   /    |
     *     |   a      |
     *     | /   \    |
     * y --|------ c  |
     *     | \        |
     *     |  ----------- q
     *     ------------
     * }
     */
    private static final RuntimeParentModule COMPOSITE_MODULE
        = (RuntimeParentModule) CompositeModuleContext.fromConnections(Arrays.asList(
            "x -> a.ax",
            "y -> a.ay",

            "x -> b.bx",
            "a.ap -> b.by",

            "a.aq -> c.cx",
            "y -> c.cy",

            "b.bp -> p",

            "y -> q"
        ))
        .getModule();

    private static BitSet bitSetOfInt(int integer) {
        return BitSet.valueOf(new long[] { integer });
    }

    private static final class TestCase {
        private final DependencyGraph dependencyGraph;
        private final List<ValueNode> hasValueList;
        private final BitSet recomputedInPorts;

        private TestCase(DependencyGraph dependencyGraph, List<ValueNode> hasValueList,
            BitSet recomputedInPorts) {
            this.dependencyGraph = dependencyGraph;
            this.hasValueList = hasValueList;
            this.recomputedInPorts = recomputedInPorts;
        }

        @Override
        public String toString() {
            RuntimeParentModule parentModule = dependencyGraph.getParentModule();
            return String.format(
                "Test case { requested out-ports: %s, recomputed in-ports: %s, ports with value: %s }",
                dependencyGraph.getRequestedOutPorts().stream()
                    .mapToObj(parentModule.getOutPorts()::get)
                    .map(RuntimeOutPort::getSimpleName)
                    .collect(Collectors.toList()),
                recomputedInPorts.stream()
                    .mapToObj(parentModule.getInPorts()::get)
                    .map(RuntimeInPort::getSimpleName)
                    .collect(Collectors.toList()),
                hasValueList.stream()
                    .map(ValueNode::getElement)
                    .collect(Collectors.toList())
            );
        }
    }

    /**
     * Runs the <em>ComputeResumeState</em> algorithm and verifies that it terminates.
     */
    private static ComputeResumeState computeResumeState(TestCase testCase) {
        DependencyGraph dependencyGraph = testCase.dependencyGraph;
        Set<ExecutionTrace> executionTraceWithValue = testCase.hasValueList.stream()
            .map(ValueNode::getExecutionTrace)
            .collect(Collectors.toSet());
        BitSet recomputedInPorts = testCase.recomputedInPorts;

        Deque<ValueNode> queriedQueue = new ArrayDeque<>();
        ComputeResumeState computeResumeState
            = new ComputeResumeState(dependencyGraph, recomputedInPorts, queriedQueue::add);

        Assert.assertFalse(
            computeResumeState.isFinished(),
            "Algorithm finished after construction, even though there is a requested out-port."
        );

        try {
            computeResumeState.getSubmodulesNeededOutPorts();
            Assert.fail("Retrieving results didn't fail even though the algorithm hasn't finished.");
        } catch (IllegalStateException ignored) { }

        try {
            computeResumeState.updateHasValue(dependencyGraph.outPortNodes().get(0), true);
            Assert.fail(String.format(
                "Updating value of dependency-graph node didn't fail, even though state was not %s.",
                HasValue.PENDING_VALUE_CHECK
            ));
        } catch (IllegalArgumentException ignored) { }

        computeResumeState.run();
        while (!queriedQueue.isEmpty()) {
            ValueNode node = queriedQueue.poll();
            computeResumeState.updateHasValue(node, executionTraceWithValue.contains(node.getExecutionTrace()));
        }
        Assert.assertTrue(computeResumeState.isFinished());

        try {
            computeResumeState.updateHasValue(dependencyGraph.outPortNodes().get(0), true);
            Assert.fail(
                "Updating value of dependency-graph node didn't fail, even though algorithm has already finished.");
        } catch (IllegalStateException ignored) { }

        return computeResumeState;
    }

    private static final class Label {
        /**
         * The expected port state.
         *
         * <p>The expected port state is update as the BFS progresses. The possible modification, however, is from
         * {@link PortState#IRRELEVANT} to either {@link PortState#READY} or {@link PortState#RECOMPUTE}.
         */
        private PortState expectedPortState = PortState.IRRELEVANT;
    }

    /**
     * Performs a BFS in order to verify that every maximal path, starting from an in-port node or from a submodule
     * node, contains at most one node with {@link PortState#READY}. This check exists to ensure that "no port will
     * receive a new value more than once" (see the CloudKeeper design document).
     *
     * <p>This test also verifies that all requested out-ports have a upstream node with state {@link PortState#READY}
     * or {@link PortState#RECOMPUTE}.
     */
    private static void consistentCheckPoint(TestCase testCase) {
        DependencyGraph dependencyGraph = testCase.dependencyGraph;

        // Add all source nodes (with no incoming edges)
        Deque<DependencyGraphNode> queue = new ArrayDeque<>();
        queue.addAll(dependencyGraph.inPortNodes());
        queue.addAll(dependencyGraph.submoduleNodes());
        Map<DependencyGraphNode, Label> stateMap = dependencyGraph.nodeStream()
            .collect(Collectors.toMap(node -> node, node -> new Label()));
        while (!queue.isEmpty()) {
            DependencyGraphNode node = queue.poll();
            Label label = stateMap.get(node);

            // Verify that "no port will receive a new value more than once"
            Assert.assertTrue(
                node.getPortState() != PortState.READY
                    || EnumSet.of(PortState.IRRELEVANT, PortState.READY).contains(label.expectedPortState),
                String.format(
                    "Found %s node in dependency graph that will receive a value: %s. %s",
                    PortState.READY, node, testCase
                )
            );

            boolean readyOrRecompute
                = node.getPortState() == PortState.READY || node.getPortState() == PortState.RECOMPUTE;
            if (readyOrRecompute) {
                label.expectedPortState = node.getPortState();
            }

            for (DependencyGraphNode successor: node.successors()) {
                Label successorLabel = stateMap.get(successor);
                if (readyOrRecompute) {
                    successorLabel.expectedPortState = PortState.RECOMPUTE;
                }
                queue.add(successor);
            }
        }

        // Verify that "resuming interpretation from all nodes in the consistent checkpoint eventually leads to the
        // computation of all required out-ports".
        dependencyGraph.getRequestedOutPorts().stream()
            .mapToObj(dependencyGraph.outPortNodes()::get)
            .forEach(
                node -> Assert.assertTrue(
                    stateMap.get(node).expectedPortState != PortState.IRRELEVANT,
                    String.format(
                        "Found requested-out-port node in dependency graph that will not receive a value: %s. %s",
                        node, testCase
                    )
                )
            );
    }

    /**
     * Verify correctness of the set of submodule out-ports that must receive a new value (during interpretation of the
     * current parent module).
     */
    private static void submoduleOutPorts(DependencyGraph dependencyGraph, ComputeResumeState computeResumeState) {
        RuntimeParentModule parentModule = dependencyGraph.getParentModule();
        Set<RuntimeOutPort> expectedSubmoduleOutPorts = new LinkedHashSet<>();

        // Add all source nodes (with no incoming edges)
        Deque<DependencyGraphNode> queue = new ArrayDeque<>();
        queue.addAll(dependencyGraph.inPortNodes());
        queue.addAll(dependencyGraph.submoduleNodes());
        while (!queue.isEmpty()) {
            DependencyGraphNode node = queue.poll();
            for (ValueNode successor: node.successors()) {
                if (node.getPortState() == PortState.READY || node.getPortState() == PortState.RECOMPUTE) {
                    assert node instanceof InPortNode || node instanceof SubmoduleInPortNode
                        || node instanceof SubmoduleNode;
                    List<? extends RuntimeOutPort> outPorts = Collections.emptyList();
                    if (node instanceof SubmoduleInPortNode) {
                        outPorts = ((SubmoduleInPortNode) node).getElement().getDependentOutPorts();
                    } else if (node instanceof SubmoduleNode) {
                        outPorts = ((SubmoduleNode) node).getElement().getOutPorts();
                    }
                    outPorts.stream()
                        .filter(
                            outPort -> outPort.getOutConnections().stream()
                                .map(RuntimeConnection::getToPort)
                                .anyMatch(successor.getElement()::equals)
                        )
                        .forEach(expectedSubmoduleOutPorts::add);
                }
                queue.add(successor);
            }
        }

        Set<RuntimeOutPort> actual = parentModule.getModules().stream()
            .flatMap(
                submodule -> computeResumeState.getSubmodulesNeededOutPorts()[submodule.getIndex()].stream()
                    .mapToObj(index -> submodule.getOutPorts().get(index))
            )
            .collect(Collectors.toSet());
        Assert.assertEquals(actual, expectedSubmoduleOutPorts);
    }

    private static void run(TestCase testCase) {
        DependencyGraph dependencyGraph = testCase.dependencyGraph;
        RuntimeParentModule parentModule = dependencyGraph.getParentModule();
        List<RuntimeInPort> inPortsPossiblyMissingValue = dependencyGraph.getRequestedOutPorts().stream()
            .mapToObj(outPortId -> parentModule.getOutPorts().get(outPortId))
            .flatMap(outPort -> outPort.getInPortDependencies().stream())
            .filter(inPort -> !testCase.recomputedInPorts.get(inPort.getInIndex()))
            .distinct()
            .collect(Collectors.toList());

        ComputeResumeState computeResumeState;
        try {
            computeResumeState = computeResumeState(testCase);
        } catch (IllegalStateException exception) {
            // An exception is acceptable if in-port may be missing
            Assert.assertTrue(!inPortsPossiblyMissingValue.isEmpty());
            Assert.assertTrue(
                inPortsPossiblyMissingValue.stream()
                    .filter(inPort -> exception.getMessage().contains(inPort.toString()))
                    .findAny()
                    .isPresent()
            );
            return;
        }

        consistentCheckPoint(testCase);
        submoduleOutPorts(testCase.dependencyGraph, computeResumeState);
    }

    /**
     * Construct all possible inputs for the given parent module.
     */
    private TestCase[] getTestCases(RuntimeParentModule module) {
        int numValueNodes
            = module.getModules().stream().mapToInt(submodule -> submodule.getInPorts().size()).sum()
            + module.getOutPorts().size()
            + module.getInPorts().size();
        int numTestCases
            = ((1 << module.getOutPorts().size()) - 1)
            * (1 << module.getInPorts().size())
            * (1 << numValueNodes);
        TestCase[] testCases = new TestCase[numTestCases];

        int i = 0;
        for (int requestedOutPortsInt = 1; requestedOutPortsInt < (1 << module.getOutPorts().size());
                ++requestedOutPortsInt) {
            BitSet requestedOutPorts = bitSetOfInt(requestedOutPortsInt);
            for (int recomputedInPortsInt = 0; recomputedInPortsInt < (1 << module.getInPorts().size());
                    ++recomputedInPortsInt) {
                BitSet recomputedInPorts = bitSetOfInt(recomputedInPortsInt);
                for (int hasValues = 0; hasValues < (1 << numValueNodes); ++hasValues) {
                    DependencyGraph dependencyGraph = new DependencyGraph(module, requestedOutPorts);
                    List<ValueNode> nodes = dependencyGraph.nodeStream()
                        .filter(node -> node instanceof ValueNode)
                        .map(node -> (ValueNode) node)
                        .collect(Collectors.toList());
                    List<ValueNode> hasValueList = bitSetOfInt(hasValues).stream()
                        .mapToObj(nodes::get)
                        .collect(Collectors.toList());
                    testCases[i] = new TestCase(dependencyGraph, hasValueList, recomputedInPorts);
                    ++i;
                }
            }
        }

        assert i == numTestCases;
        return testCases;
    }

    @Test
    public void allInputCombinations() {
        Arrays.stream(getTestCases(COMPOSITE_MODULE))
            .parallel()
            .forEach(ComputeResumeStateTest::run);
    }
}
