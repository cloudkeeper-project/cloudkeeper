package xyz.cloudkeeper.linker;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.linker.examples.BinarySum;
import xyz.cloudkeeper.linker.examples.Fibonacci;
import xyz.cloudkeeper.linker.examples.Memory;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.BareSimpleNameable;
import xyz.cloudkeeper.model.beans.element.module.MutableProxyModule;
import xyz.cloudkeeper.model.beans.execution.MutableExecutionTraceTarget;
import xyz.cloudkeeper.model.beans.execution.MutableOverride;
import xyz.cloudkeeper.model.beans.execution.MutableOverrideTarget;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AbsoluteExecutionTraceTest {
    private RepositoryImpl fibRepository;
    private final LinkerOptions linkerOptions = LinkerOptions.nonExecutable();

    @BeforeClass
    public void setup() throws LinkerException {
        fibRepository = (RepositoryImpl) Linker.createRepository(
            Arrays.asList(Examples.simpleBundle(), Examples.fibonacciBundle()),
            linkerOptions
        );
    }

    static List<SimpleName> names(List<? extends BareSimpleNameable> namables) {
        List<SimpleName> newList = new ArrayList<>(namables.size());
        for (BareSimpleNameable namable: namables) {
            newList.add(namable.getSimpleName());
        }
        return newList;
    }

    @Test
    public void test() throws LinkerException {
        RuntimeAnnotatedExecutionTrace root = Linker.createAnnotatedExecutionTrace(
            ExecutionTrace.empty(),
            new MutableProxyModule()
                .setDeclaration(Fibonacci.class.getName()),
            Collections.singletonList(
                new MutableOverride()
                    .setTargets(Collections.<MutableOverrideTarget<?>>singletonList(
                        new MutableExecutionTraceTarget()
                            .setExecutionTrace("/zero")
                    ))
                    .setDeclaredAnnotations(Collections.singletonList(
                        Memory.Beans.createAnnotation(12, "MiB")
                    ))
            ),
            fibRepository,
            linkerOptions
        );

        Assert.assertEquals(
            names(root.getModule().getPorts()),
            names(
                fibRepository
                    .getElement(ModuleDeclarationImpl.class, Name.qualifiedName(Fibonacci.class.getName()))
                    .getPorts()
            )
        );
        try {
            root.getInPort();
            Assert.fail();
        } catch (IllegalStateException ignored) { }
        try {
            root.getOutPort();
            Assert.fail();
        } catch (IllegalStateException ignored) { }


        RuntimeAnnotatedExecutionTrace zeroTrace = root.resolveContent().resolveModule(SimpleName.identifier("zero"));
        Assert.assertTrue(zeroTrace.getModule() instanceof InputModuleImpl);
        try {
            zeroTrace.getInPort();
            Assert.fail();
        } catch (IllegalStateException ignored) { }
        try {
            zeroTrace.getOutPort();
            Assert.fail();
        } catch (IllegalStateException ignored) { }

        Memory memory = zeroTrace.getAnnotation(Memory.class);
        Assert.assertEquals(memory.value(), 12);
        Assert.assertEquals(memory.unit(), "MiB");


        RuntimeAnnotatedExecutionTrace sumTrace = root.resolveContent().resolveModule(SimpleName.identifier("loop"))
            .resolveContent().resolveModule(SimpleName.identifier("sum"));
        Assert.assertEquals(
            names(sumTrace.getModule().getPorts()),
            names(
                fibRepository
                    .getElement(ModuleDeclarationImpl.class, Name.qualifiedName(BinarySum.class.getName()))
                    .getPorts()
            )
        );
        try {
            sumTrace.getInPort();
            Assert.fail();
        } catch (IllegalStateException ignored) { }
        try {
            sumTrace.getOutPort();
            Assert.fail();
        } catch (IllegalStateException ignored) { }
    }
}
