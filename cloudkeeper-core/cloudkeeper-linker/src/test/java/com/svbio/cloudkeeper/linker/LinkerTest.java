package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.linker.examples.BinarySum;
import com.svbio.cloudkeeper.linker.examples.Memory;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.NotFoundException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSiblingConnection;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.svbio.cloudkeeper.model.immutable.element.Name.qualifiedName;

public class LinkerTest {
    private final LinkerImpl linker = LinkerImpl.getInstance();
    private LinkerOptions linkerOptions;
    private RuntimeRepository fibRepository;

    @BeforeClass
    void setup() throws LinkerException {
        linkerOptions = new LinkerOptions.Builder()
            .setDeserializeSerializationTrees(true)
            .build();
    }

    @Test
    public void simpleRepositoryTest() throws LinkerException {
        BareBundle bareSimpleRepository = Examples.simpleBundle();

        RepositoryImpl simpleRepository
            = linker.createRepository(Collections.singletonList(bareSimpleRepository), linkerOptions);
        Assert.assertTrue(simpleRepository.getState() == AbstractFreezable.State.PRECOMPUTED);
    }

    @Test(dependsOnMethods = "simpleRepositoryTest")
    public void dependentRepositoryTest() throws LinkerException {
        BareBundle bareFibRepository = Examples.fibonacciBundle();

        try {
            linker.createRepository(Collections.singletonList(bareFibRepository), linkerOptions);
            Assert.fail();
        } catch (NotFoundException exception) {
            Assert.assertTrue(exception.getMessage().contains(BinarySum.class.getPackage().getName()));
        }

        fibRepository
            = linker.createRepository(Arrays.asList(bareFibRepository, Examples.simpleBundle()), linkerOptions);
    }

    @Test(dependsOnMethods = "dependentRepositoryTest")
    public void annotationTest() {
        RuntimeSimpleModuleDeclaration declaration =
            fibRepository.getElement(RuntimeSimpleModuleDeclaration.class, qualifiedName(BinarySum.class.getName()));
        Memory memoryAnnotation = declaration.getDeclaredAnnotations().get(0).getJavaAnnotation(Memory.class);
        Memory originalAnnotation = BinarySum.class.getAnnotation(Memory.class);

        Assert.assertEquals(memoryAnnotation, originalAnnotation);
        Assert.assertEquals(originalAnnotation, memoryAnnotation);
    }

    private RuntimeCompositeModule createLinkedCompositeModule(
            BareCompositeModule bareCompositeModule) throws LinkerException {
        RuntimeRepository systemRepository
            = linker.createRepository(Collections.<BareBundle>emptyList(), linkerOptions);
        return (RuntimeCompositeModule) linker
            .createAnnotatedExecutionTrace(
                ExecutionTrace.empty(), bareCompositeModule, Collections.emptyList(), systemRepository, linkerOptions)
            .getModule();
    }

    @Test
    public void selfConnection() throws LinkerException {
        // Create valid composite module that contains a single input module with a single connection
        MutableCompositeModule compositeModule = new MutableCompositeModule()
            .setModules(Collections.<MutableModule<?>>singletonList(
                new MutableInputModule()
                    .setSimpleName("input")
                    .setOutPortType(new MutableDeclaredType().setDeclaration(Integer.class.getName()))
                    .setValue(4)
            ))
            .setDeclaredPorts(Collections.<MutablePort<?>>singletonList(
                new MutableOutPort()
                    .setSimpleName("result")
                    .setType(new MutableDeclaredType().setDeclaration(Integer.class.getName()))
            ))
            .setConnections(Collections.<MutableConnection<?>>singletonList(
                new MutableChildOutToParentOutConnection()
                    .setFromModule("input")
                    .setFromPort(BareInputModule.OUT_PORT_NAME)
                    .setToPort("result")
            ));

        // First start of with this valid declaration.
        createLinkedCompositeModule(compositeModule);

        // Now change the connection target to input module
        MutableSiblingConnection siblingConnection = new MutableSiblingConnection()
            .setFromModule("input")
            .setFromPort("foo")
            .setToModule("input")
            .setToPort(BareInputModule.OUT_PORT_NAME);
        compositeModule.setConnections(Collections.<MutableConnection<?>>singletonList(siblingConnection));
        try {
            createLinkedCompositeModule(compositeModule);
            Assert.fail();
        } catch (NotFoundException exception) {
            String message = exception.getMessage();
            Assert.assertTrue(message.contains("out-port") && message.contains("'foo'"));
        }

        // Now both from- and to-port are input#value. This is not a valid connection.
        siblingConnection
            .setFromPort(BareInputModule.OUT_PORT_NAME)
            .setToPort(BareInputModule.OUT_PORT_NAME);
        try {
            createLinkedCompositeModule(compositeModule);
            Assert.fail();
        } catch (NotFoundException exception) {
            String message = exception.getMessage();
            Assert.assertTrue(message.contains("in-port") && message.contains(BareInputModule.OUT_PORT_NAME));
        }
    }
}
