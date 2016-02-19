package xyz.cloudkeeper.interpreter;

import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.interpreter.ExecutorActorInterface.CancelExecution;
import xyz.cloudkeeper.interpreter.ExecutorActorInterface.ExecuteTrace;
import xyz.cloudkeeper.interpreter.InterpreterInterface.InPortHasSignal;
import xyz.cloudkeeper.interpreter.InterpreterInterface.SubmoduleOutPortHasSignal;
import xyz.cloudkeeper.interpreter.event.BeginExecutionTraceEvent;
import xyz.cloudkeeper.interpreter.event.EndSimpleModuleTraceEvent;
import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.linker.LinkerOptions;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.beans.element.MutableBundle;
import xyz.cloudkeeper.model.beans.element.MutablePackage;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.beans.element.module.MutableInPort;
import xyz.cloudkeeper.model.beans.element.module.MutableOutPort;
import xyz.cloudkeeper.model.beans.element.module.MutableProxyModule;
import xyz.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static xyz.cloudkeeper.interpreter.ModuleInterpretation.await;
import static xyz.cloudkeeper.interpreter.ModuleInterpretation.bitSetOf;

public class SimpleModuleInterpreterActorTest {
    private static final Name PACKAGE
        = Name.qualifiedName(SimpleModuleInterpreterActorTest.class.getPackage().getName());
    private static final int MODULE_ID = 3;
    private static final SimpleName TEST_MODULE_NAME = SimpleName.identifier("TestModule");
    private static final String EXPECTED_MSG = "Just kidding. This is a test.";

    @Nullable private RuntimeRepository repository;
    @Nullable private RuntimeAnnotatedExecutionTrace executionTrace;

    @BeforeClass
    public void setup() throws LinkerException {
        MutableTypeMirror<?> booleanType = new MutableDeclaredType()
            .setDeclaration(Boolean.class.getName());
        MutableBundle bundle = new MutableBundle()
            .setBundleIdentifier(URI.create("x-test:" + SimpleModuleInterpreterActorTest.class.getName()))
            .setPackages(Collections.singletonList(
                new MutablePackage()
                    .setQualifiedName(PACKAGE)
                    .setDeclarations(Collections.singletonList(
                        new MutableSimpleModuleDeclaration()
                            .setSimpleName(TEST_MODULE_NAME)
                            .setPorts(Arrays.asList(
                                new MutableInPort()
                                    .setSimpleName("x")
                                    .setType(booleanType),
                                new MutableInPort()
                                    .setSimpleName("y")
                                    .setType(booleanType),
                                new MutableOutPort()
                                    .setSimpleName("p")
                                    .setType(booleanType)
                            ))
                    ))
            ));
        MutableProxyModule module = new MutableProxyModule()
            .setDeclaration(new MutableQualifiedNamable().setQualifiedName(PACKAGE.join(TEST_MODULE_NAME)));
        repository = Linker.createRepository(Collections.singletonList(bundle), LinkerOptions.nonExecutable());
        executionTrace = Linker.createAnnotatedExecutionTrace(ExecutionTrace.valueOf("/loop/2/sum"), module,
            Collections.emptyList(), repository, LinkerOptions.nonExecutable());
    }

    private ModuleInterpretation.Builder testCaseBuilder(String testCaseName) {
        assert repository != null && executionTrace != null;
        return new ModuleInterpretation.Builder(testCaseName, repository, executionTrace);
    }

    @Test
    public void lifecycleEvents() {
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("lifecycleEvents").build()) {
            Props props = moduleInterpretation.toSimpleModuleInterpreterProps(MODULE_ID, bitSetOf(), bitSetOf(0));
            long timestampLowerBound = System.currentTimeMillis();
            TestActorRef<SimpleModuleInterpreterActor> actorRef = TestActorRef.create(
                moduleInterpretation.getActorSystem(), props, moduleInterpretation.getSupervisor());

            // Verify start message
            TestProbe eventProbe = moduleInterpretation.getEventProbe();
            BeginExecutionTraceEvent beginExecutionTraceEvent
                = eventProbe.expectMsgClass(BeginExecutionTraceEvent.class);
            LocalInterpreterProperties localInterpreterProperties
                = moduleInterpretation.getLocalInterpreterProperties();
            Assert.assertEquals(
                beginExecutionTraceEvent,
                BeginExecutionTraceEvent.of(
                    localInterpreterProperties.getExecutionId(),
                    beginExecutionTraceEvent.getTimestamp(),
                    moduleInterpretation.getStagingArea().getAnnotatedExecutionTrace()
                )
            );
            Assert.assertTrue(timestampLowerBound <= beginExecutionTraceEvent.getTimestamp()
                && beginExecutionTraceEvent.getTimestamp() <= System.currentTimeMillis());
            Assert.assertFalse(eventProbe.msgAvailable());

            // Verify that executor receives an ExecuteTrace message because all inputs are already available. No need
            // to verify the message, this is done in inPortHasSignal()
            TestProbe executorProbe = moduleInterpretation.getExecutorProbe();
            executorProbe.expectMsgClass(ExecuteTrace.class);
            Assert.assertFalse(executorProbe.msgAvailable());

            // Kill the actor
            actorRef.stop();

            // Verify that the executor was informed
            executorProbe.expectMsg(new CancelExecution("Simple-module interpreter terminated."));

            // Verify EndExecutionTraceEvent message
            EndSimpleModuleTraceEvent endExecutionTraceEvent
                = eventProbe.expectMsgClass(EndSimpleModuleTraceEvent.class);
            Assert.assertEquals(
                endExecutionTraceEvent,
                EndSimpleModuleTraceEvent.of(
                    localInterpreterProperties.getExecutionId(),
                    endExecutionTraceEvent.getTimestamp(),
                    moduleInterpretation.getStagingArea().getAnnotatedExecutionTrace(),
                    null
                )
            );
            Assert.assertTrue(beginExecutionTraceEvent.getTimestamp() <= endExecutionTraceEvent.getTimestamp()
                && endExecutionTraceEvent.getTimestamp() <= System.currentTimeMillis());
            Assert.assertFalse(eventProbe.msgAvailable());
        }
    }

    @Test
    public void inPortHasSignal() {
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("successfulExecution").build()) {
            Props props = moduleInterpretation.toSimpleModuleInterpreterProps(MODULE_ID, bitSetOf(0, 1), bitSetOf(0));
            TestActorRef<SimpleModuleInterpreterActor> actorRef = TestActorRef.create(
                moduleInterpretation.getActorSystem(), props, moduleInterpretation.getSupervisor());

            // Now send InPortHasSignal messages
            TestProbe executorProbe = moduleInterpretation.getExecutorProbe();
            Assert.assertFalse(executorProbe.msgAvailable());
            actorRef.receive(new InPortHasSignal(0));
            Assert.assertFalse(executorProbe.msgAvailable());
            actorRef.receive(new InPortHasSignal(1));

            // Verify that executor receives an ExecuteTrace message
            ExecuteTrace executeTraceMessage = executorProbe.expectMsgClass(ExecuteTrace.class);
            RuntimeStateProvider runtimeStateProvider = executeTraceMessage.getRuntimeStateProvider();
            InstanceProvider instanceProvider = mock(InstanceProvider.class);
            RuntimeContext restoredRuntimeContext = await(runtimeStateProvider.provideRuntimeContext(instanceProvider));
            Assert.assertSame(restoredRuntimeContext.getRepository(), repository);

            // Send the SimpleModuleExecutorResult that the executor would send. Verify that the parent actor receives a
            // SubmoduleOutPortHasSignal message
            SimpleModuleExecutorResult executionResult = new SimpleModuleExecutorResult.Builder(
                    Name.qualifiedName(SimpleModuleExecutorResult.class.getName()))
                .build();
            actorRef.receive(executionResult);
            TestProbe parentProbe = moduleInterpretation.getParentProbe();
            parentProbe.expectMsg(new SubmoduleOutPortHasSignal(MODULE_ID, 0));

            // Verify that the actor terminates properly. The Akka manual says: "the watching actor will receive a
            // Terminated message even if the watched actor has already been terminated at the time of registration."
            parentProbe.watch(actorRef);
            parentProbe.expectMsgClass(Terminated.class);
            Assert.assertFalse(parentProbe.msgAvailable());
        }
    }

    @Test
    public void failure() {
        assert executionTrace != null;
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("successfulExecution").build()) {
            Props props = moduleInterpretation.toSimpleModuleInterpreterProps(MODULE_ID, bitSetOf(), bitSetOf(0));
            TestActorRef<SimpleModuleInterpreterActor> actorRef = TestActorRef.create(
                moduleInterpretation.getActorSystem(), props, moduleInterpretation.getSupervisor());

            // Send a SimpleModuleExecutorResult indicating failure, similar to Class#newInstance().
            SimpleModuleExecutorResult executionResult = new SimpleModuleExecutorResult.Builder(
                    Name.qualifiedName(SimpleModuleExecutorResult.class.getName()))
                .setException(new ExecutionException(EXPECTED_MSG))
                .build();

            try {
                // Unfortunately, receive() bypasses the compile-time exception checking
                actorRef.receive(executionResult);
                Assert.fail();
            } catch (Exception exception) {
                InterpreterException interpreterException = (InterpreterException) exception;
                Assert.assertEquals(interpreterException.getExecutionTrace(), ExecutionTrace.copyOf(executionTrace));
                ExecutionException cause = (ExecutionException) interpreterException.getCause();
                Assert.assertEquals(cause.getMessage(), EXPECTED_MSG);
            }
        }
    }
}
