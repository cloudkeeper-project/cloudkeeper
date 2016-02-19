package xyz.cloudkeeper.interpreter;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.interpreter.DependencyGraph.DependencyGraphNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.InPortNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.OutPortNode;
import xyz.cloudkeeper.interpreter.DependencyGraph.SubmoduleNode;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimePort;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace.Type;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyGraphTest {
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

    private DependencyGraph diamondGraph;

    @BeforeClass
    public void setup() {
        BitSet allOutPorts = new BitSet();
        allOutPorts.set(0, COMPOSITE_MODULE.getOutPorts().size());
        diamondGraph = new DependencyGraph(COMPOSITE_MODULE, allOutPorts);
    }

    @Test
    public void nodeStream() {
        Set<String> executionTracesOnPath = diamondGraph.nodeStream()
            .map(DependencyGraphNode::getExecutionTrace)
            .map(ExecutionTrace::toString)
            .collect(Collectors.toSet());

        Assert.assertEquals(
            executionTracesOnPath,
            new HashSet<>(Arrays.asList(
                ":in:x", ":in:y", "/a", "/a:in:ax", "/a:in:ay", "/b", "/b:in:bx", "/b:in:by", "/c", "/c:in:cx",
                "/c:in:cy", ":out:p", ":out:q"
            ))
        );
    }

    @Test
    public void inPortNodes() {
        List<InPortNode> inPortNodes = diamondGraph.inPortNodes();
        Assert.assertEquals(COMPOSITE_MODULE.getInPorts().size(), 2);
        Assert.assertEquals(
            inPortNodes.stream().map(InPortNode::getElement).collect(Collectors.toList()),
            COMPOSITE_MODULE.getInPorts()
        );
        Assert.assertTrue(
            COMPOSITE_MODULE.getInPorts().stream()
                .allMatch(inPort -> inPortNodes.get(inPort.getInIndex()) == diamondGraph.sourceNode(inPort))
        );
    }

    @Test
    public void outPortNodes() {
        List<OutPortNode> outPortNodes = diamondGraph.outPortNodes();
        Assert.assertEquals(COMPOSITE_MODULE.getOutPorts().size(), 2);
        Assert.assertEquals(
            outPortNodes.stream().map(OutPortNode::getElement).collect(Collectors.toList()),
            COMPOSITE_MODULE.getOutPorts()
        );
        Assert.assertTrue(
            COMPOSITE_MODULE.getOutPorts().stream()
                .allMatch(outPort -> outPortNodes.get(outPort.getOutIndex()) == diamondGraph.targetNode(outPort))
        );
    }

    @Test
    public void submoduleNode() {
        List<SubmoduleNode> submoduleNodes = diamondGraph.submoduleNodes();
        Assert.assertEquals(COMPOSITE_MODULE.getModules().size(), 3);
        Assert.assertEquals(
            submoduleNodes.stream().map(SubmoduleNode::getElement).collect(Collectors.toList()),
            COMPOSITE_MODULE.getModules()
        );
    }

    @Test
    public void onPathToOutPort() {
        Set<String> executionTracesOnPath = diamondGraph.nodeStream()
            .filter(DependencyGraphNode::isOnPathToOutPort)
            .map(DependencyGraphNode::getExecutionTrace)
            .map(ExecutionTrace::toString)
            .collect(Collectors.toSet());

        Assert.assertEquals(
            executionTracesOnPath,
            new HashSet<>(Arrays.asList(
                ":in:x", ":in:y", "/a", "/a:in:ax", "/a:in:ay", "/b", "/b:in:bx", "/b:in:by", ":out:p", ":out:q"
            ))
        );
    }

    private DependencyGraphNode node(String trace) {
        ExecutionTrace nodeTrace = ExecutionTrace.valueOf(trace);
        int traceSize = nodeTrace.size();
        Type type = nodeTrace.getType();
        assert (traceSize == 1 && (type == Type.IN_PORT || type == Type.OUT_PORT))
            || (traceSize == 2 && type == Type.MODULE)
            || (traceSize == 3 && type == Type.IN_PORT);
        boolean isSourceNode;
        @Nullable RuntimePort port;

        if (traceSize == 1) {
            isSourceNode = type == Type.IN_PORT;
            port = COMPOSITE_MODULE.getEnclosedElement(RuntimePort.class, nodeTrace.getSimpleName());
        } else if (traceSize == 2) {
            @Nullable RuntimeModule submodule
                = COMPOSITE_MODULE.getEnclosedElement(RuntimeModule.class, nodeTrace.getSimpleName());
            assert submodule != null;
            return diamondGraph.submodule(submodule);
        } else {
            isSourceNode = true;
            @Nullable RuntimeModule submodule = COMPOSITE_MODULE.getEnclosedElement(
                RuntimeModule.class, nodeTrace.asElementList().get(1).getSimpleName());
            assert submodule != null;
            port = submodule.getEnclosedElement(RuntimeInPort.class, nodeTrace.getSimpleName());
        }
        assert port != null;
        return isSourceNode
            ? diamondGraph.sourceNode(port)
            : diamondGraph.targetNode(port);
    }

    private void assertPredecessors(String node, String... predecessors) {
        Assert.assertEquals(
            node(node).predecessorStream().collect(Collectors.toSet()),
            Stream.of(predecessors).map(this::node).collect(Collectors.toSet())
        );
    }

    private void assertSuccessors(String node, String... successors) {
        Assert.assertEquals(
            node(node).successorStream().collect(Collectors.toSet()),
            Stream.of(successors).map(this::node).collect(Collectors.toSet())
        );
    }

    @Test
    public void predecessors() {
        assertPredecessors(":in:x");
        assertPredecessors(":in:y");

        assertPredecessors("/a");
        assertPredecessors("/a:in:ax", ":in:x");
        assertPredecessors("/a:in:ay", ":in:y");

        assertPredecessors("/b");
        assertPredecessors("/b:in:bx", ":in:x");
        assertPredecessors("/b:in:by", "/a", "/a:in:ax", "/a:in:ay");

        assertPredecessors("/c");
        assertPredecessors("/c:in:cx");
        assertPredecessors("/c:in:cy");

        assertPredecessors(":out:p", "/b", "/b:in:bx", "/b:in:by");
        assertPredecessors(":out:q", ":in:y");
    }

    @Test
    public void successors() {
        assertSuccessors(":in:x", "/a:in:ax", "/b:in:bx");
        assertSuccessors(":in:y", "/a:in:ay", ":out:q");

        assertSuccessors("/a", "/b:in:by");
        assertSuccessors("/a:in:ax", "/b:in:by");
        assertSuccessors("/a:in:ay", "/b:in:by");

        assertSuccessors("/b", ":out:p");
        assertSuccessors("/b:in:bx", ":out:p");
        assertSuccessors("/b:in:by", ":out:p");

        assertSuccessors("/c");
        assertSuccessors("/c:in:cx");
        assertSuccessors("/c:in:cy");

        assertSuccessors(":out:p");
        assertSuccessors(":out:q");
    }

    @Test
    public void setHasValue() {
        DependencyGraph dependencyGraph = new DependencyGraph(COMPOSITE_MODULE, new BitSet());
        InPortNode inPortNode = dependencyGraph.inPortNodes().get(0);
        Assert.assertEquals(inPortNode.getHasValue(), DependencyGraph.HasValue.UNKNOWN);

        inPortNode.setHasValue(DependencyGraph.HasValue.UNKNOWN);
        Assert.assertEquals(inPortNode.getHasValue(), DependencyGraph.HasValue.UNKNOWN);

        inPortNode.setHasValue(DependencyGraph.HasValue.PENDING_VALUE_CHECK);
        inPortNode.setHasValue(DependencyGraph.HasValue.PENDING_VALUE_CHECK);

        try {
            inPortNode.setHasValue(DependencyGraph.HasValue.UNKNOWN);
            Assert.fail();
        } catch (IllegalArgumentException ignore) { }

        Assert.assertEquals(inPortNode.getHasValue(), DependencyGraph.HasValue.PENDING_VALUE_CHECK);
        inPortNode.setHasValue(DependencyGraph.HasValue.HAS_VALUE);

        try {
            inPortNode.setHasValue(DependencyGraph.HasValue.UNKNOWN);
            Assert.fail();
        } catch (IllegalArgumentException ignore) { }

        try {
            inPortNode.setHasValue(DependencyGraph.HasValue.PENDING_VALUE_CHECK);
            Assert.fail();
        } catch (IllegalArgumentException ignore) { }

        try {
            inPortNode.setHasValue(DependencyGraph.HasValue.NO_VALUE);
            Assert.fail();
        } catch (IllegalArgumentException ignore) { }
    }
}
