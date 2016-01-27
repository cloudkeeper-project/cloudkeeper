package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class CompositeModuleContextTest {
    /**
     * Verifies correct construction of a module of this form:
     *
     * {@code
     *     ----------------
     * x --|------ b      |
     *     | \   /   \    |
     *     |   a       d --- p
     *     | /   \   /    |
     * y --|------ c      |
     *     |              |
     * z --|---------------- q
     *     ----------------
     * }
     */
    @Test
    public void test() throws LinkerException {
        CompositeModuleContext context = CompositeModuleContext.fromConnections(Arrays.asList(
            "x -> a.ax",
            "y -> a.ay",

            "x -> b.bx",
            "a.ap -> b.by",

            "a.ap -> c.cx",
            "y -> c.cy",

            "b.bp -> d.dx",
            "c.cp -> d.dy",

            "d.dp -> p",

            "z -> q"
        ));

        RuntimeAnnotatedExecutionTrace executionTrace = context.getExecutionTrace();
        RuntimeCompositeModule compositeModule = (
                (RuntimeCompositeModuleDeclaration) ((RuntimeProxyModule) executionTrace.getModule()).getDeclaration()
            ).getTemplate();
        Assert.assertEquals(
            compositeModule.getInPorts().stream()
                .map(port -> port.getSimpleName().toString())
                .collect(Collectors.toList()),
            Arrays.asList("x", "y", "z")
        );
        Assert.assertEquals(
            compositeModule.getOutPorts().stream()
                .map(port -> port.getSimpleName().toString())
                .collect(Collectors.toList()),
            Arrays.asList("p", "q")
        );
        Assert.assertEquals(
            compositeModule.getModules().stream()
                .map(module -> module.getSimpleName().toString())
                .collect(Collectors.toList()),
            Arrays.asList("a", "b", "c", "d")
        );
        Assert.assertEquals(
            Objects.requireNonNull(compositeModule.getEnclosedElement(RuntimeInPort.class, SimpleName.identifier("x")))
                .getOutConnections()
                .stream()
                .map(connection -> connection.getToPort().getSimpleName().toString())
                .collect(Collectors.toList()),
            Arrays.asList("ax", "bx")
        );
        Assert.assertEquals(
            Objects.requireNonNull(compositeModule.getEnclosedElement(RuntimeInPort.class, SimpleName.identifier("y")))
                .getOutConnections()
                .stream()
                .map(connection -> connection.getToPort().getSimpleName().toString())
                .collect(Collectors.toList()),
            Arrays.asList("ay", "cy")
        );
        Assert.assertEquals(
            Objects.requireNonNull(compositeModule.getEnclosedElement(RuntimeInPort.class, SimpleName.identifier("z")))
                .getOutConnections()
                .stream()
                .map(connection -> connection.getToPort().getSimpleName().toString())
                .collect(Collectors.toList()),
            Collections.singletonList("q")
        );
    }
}
