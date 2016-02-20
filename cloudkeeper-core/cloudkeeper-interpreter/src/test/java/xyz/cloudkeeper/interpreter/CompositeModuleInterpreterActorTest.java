package xyz.cloudkeeper.interpreter;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.Logging.LogEvent;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.interpreter.InterpreterInterface.InPortHasSignal;
import xyz.cloudkeeper.interpreter.InterpreterInterface.SubmoduleOutPortHasSignal;
import xyz.cloudkeeper.interpreter.event.BeginExecutionTraceEvent;
import xyz.cloudkeeper.interpreter.event.EndExecutionTraceEvent;
import xyz.cloudkeeper.marshaling.MarshalingTreeNode.ObjectNode;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import xyz.cloudkeeper.model.util.ImmutableList;
import xyz.cloudkeeper.staging.MapStagingArea;
import xyz.cloudkeeper.testkit.CallingThreadExecutor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static xyz.cloudkeeper.interpreter.ModuleInterpretation.await;
import static xyz.cloudkeeper.interpreter.ModuleInterpretation.bitSetOf;

public class CompositeModuleInterpreterActorTest {
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
     *     |          |
     *     |       d ---- r
     *     ------------
     * }
     */
    private static final ImmutableList<String> CONNECTIONS = ImmutableList.copyOf(Arrays.asList(
        "x -> a.ax",
        "y -> a.ay",

        "x -> b.bx",
        "a.ap -> b.by",

        "a.aq -> c.cx",
        "y -> c.cy",

        "b.bp -> p",

        "y -> q",

        "d.bp -> r"
    ));


    private static final CompositeModuleContext PROXY_CONTEXT = CompositeModuleContext.fromConnections(CONNECTIONS);

    private static final CompositeModuleContext INLINE_CONTEXT;

    static {
        CompositeModuleContext.Builder builder
            = new CompositeModuleContext.Builder(CompositeModuleContext.DEFAULT_BUNDLE_IDENTIFIER)
                .setInlineTestModule(true);
        CONNECTIONS.forEach(builder::addConnection);
        INLINE_CONTEXT = builder.build();
    }

    private static void put(StagingArea stagingArea, String... keys) {
        for (String key: keys) {
            await(stagingArea.putObject(ExecutionTrace.valueOf(key), 0));
        }
    }

    private static ActorRef actor(ActorSystem actorSystem, ActorPath path) {
        return await(actorSystem.actorSelection(path).resolveOne(ModuleInterpretation.DEFAULT_DURATION));
    }

    private static Set<ExecutionTrace> tracesSet(String... traces) {
        return Arrays.stream(traces)
            .map(ExecutionTrace::valueOf)
            .map(CompositeModuleContext.TEST_MODULE_TRACE::resolveExecutionTrace)
            .collect(Collectors.toSet());
    }

    private static ModuleInterpretation.Builder testCaseBuilder(String testCaseName, CompositeModuleContext context) {
        return new ModuleInterpretation.Builder(testCaseName, context.getRepository(),
                context.getExecutionTrace())
            .setConfig(Collections.singletonMap(
                "akka.loggers", Collections.singletonList(SwallowingLogger.class.getName())
            ));
    }

    private static RuntimeModule submodule(ModuleInterpretation interpretation, String name) {
        @Nullable RuntimeModule submodule = interpretation.getResolvedModule()
            .getEnclosedElement(RuntimeModule.class, SimpleName.identifier(name));
        assert submodule != null;
        return submodule;
    }

    /**
     * Verifies that events are sent in {@link CompositeModuleInterpreterActor#preStart()} and
     * {@link CompositeModuleInterpreterActor#postStop()}.
     */
    @Test
    public void lifecycleEvents() {
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("lifecycleEvents", INLINE_CONTEXT).build()) {
            Props props = moduleInterpretation.toCompositeModuleInterpreterProps(3, bitSetOf(), bitSetOf(2));
            long timestampLowerBound = System.currentTimeMillis();
            TestActorRef<CompositeModuleInterpreterActor> actorRef
                = TestActorRef.create(moduleInterpretation.getActorSystem(), props);

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

            // Verify that the submodule interpreter has not yet been started (pending the check whether the out-port
            // "r" already has a value). Then process events so that the ComputeResumeState algorithm finishes. Verify
            // that the submodule interpreter has been started then.
            CompositeModuleInterpreterActor actor = actorRef.underlyingActor();
            int dId = submodule(moduleInterpretation, "d").getIndex();
            Assert.assertNull(actor.getChildExecutor(dId));
            moduleInterpretation.getAsyncTaskExecutor().executeAll();
            Assert.assertNotNull(actor.getChildExecutor(dId));

            // Verify EndExecutionTraceEvent message in case that actor is stopped abruptly
            actorRef.stop();
            EndExecutionTraceEvent endExecutionTraceEvent = eventProbe.expectMsgClass(EndExecutionTraceEvent.class);
            Assert.assertEquals(
                endExecutionTraceEvent,
                EndExecutionTraceEvent.of(
                    localInterpreterProperties.getExecutionId(),
                    endExecutionTraceEvent.getTimestamp(),
                    moduleInterpretation.getStagingArea().getAnnotatedExecutionTrace(),
                    false
                )
            );
            Assert.assertTrue(beginExecutionTraceEvent.getTimestamp() <= endExecutionTraceEvent.getTimestamp()
                && endExecutionTraceEvent.getTimestamp() <= System.currentTimeMillis());
            Assert.assertFalse(eventProbe.msgAvailable());
        }
    }

    /**
     * Verifies correct handling of {@link InPortHasSignal} messages.
     */
    @Test
    public void inPortHasSignal() {
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("inPortHasSignal", PROXY_CONTEXT).build()) {
            MapStagingArea stagingArea = moduleInterpretation.getStagingArea();
            Map<ExecutionTrace, ObjectNode> stagingAreaMap = stagingArea.toUnmodifiableMap();
            put(stagingArea, ":in:x", ":in:y");

            int moduleId = 3;
            Props props = moduleInterpretation.toCompositeModuleInterpreterProps(moduleId, bitSetOf(0), bitSetOf(0, 1));
            ActorSystem actorSystem = moduleInterpretation.getActorSystem();
            TestActorRef<CompositeModuleInterpreterActor> actorRef
                = TestActorRef.create(actorSystem, props, moduleInterpretation.getSupervisor());

            TestProbe akkaEventProbe = new TestProbe(actorSystem);
            actorSystem.eventStream().subscribe(akkaEventProbe.ref(), Object.class);

            // Verify that we see a warning if the InPortHasSignal message is sent twice
            actorRef.receive(new InPortHasSignal(0));
            actorRef.receive(new InPortHasSignal(0));
            LogEvent redundantMsgLogEvent = akkaEventProbe.expectMsgClass(LogEvent.class);
            Assert.assertEquals(redundantMsgLogEvent.logSource(), actorRef.path().toString());
            String redundantMsg = (String) redundantMsgLogEvent.message();
            Assert.assertTrue(redundantMsg.contains("redundant"));
            Assert.assertTrue(redundantMsg.contains(PROXY_CONTEXT.getModule().getInPorts().get(0).toString()));

            // Empty the executor queue, and subsequently verify that the in-port values have been copied and that the
            // parent was informed about the new out-port value.
            CallingThreadExecutor asyncTaskExecutor = moduleInterpretation.getAsyncTaskExecutor();
            asyncTaskExecutor.executeAll();
            TestProbe parentProbe = moduleInterpretation.getParentProbe();
            Assert.assertEquals(
                parentProbe.expectMsgClass(Object.class),
                new SubmoduleOutPortHasSignal(moduleId, 1)
            );
            Assert.assertFalse(parentProbe.msgAvailable());
            Assert.assertFalse(akkaEventProbe.msgAvailable());
            Assert.assertEquals(
                stagingAreaMap.keySet(),
                tracesSet(":in:x", ":in:y", "/a:in:ax", "/a:in:ay", "/b:in:bx", ":out:q")
            );

            // Verify that messages have been sent to the child interpreters
            Assert.assertEquals(
                IntStream.range(0, 2)
                    .mapToObj(
                        id -> moduleInterpretation.getTestProbeForChild("a").expectMsgClass(InPortHasSignal.class)
                    )
                    .mapToInt(InPortHasSignal::getInPortId)
                    .collect(BitSet::new, BitSet::set, BitSet::or),
                bitSetOf(0, 1)
            );
            Assert.assertFalse(moduleInterpretation.getTestProbeForChild("a").msgAvailable());
            moduleInterpretation.getTestProbeForChild("b").expectMsg(new InPortHasSignal(0));
            Assert.assertFalse(moduleInterpretation.getTestProbeForChild("b").msgAvailable());
            Assert.assertFalse(moduleInterpretation.getTestProbeForChild("c").msgAvailable());

            // Verify that we see a warning if the InPortHasSignal message message is sent about an unexpected port
            // that was not included in the set of requested in-ports
            actorRef.receive(new InPortHasSignal(1));
            LogEvent unexpectedMsgLogEvent = akkaEventProbe.expectMsgClass(LogEvent.class);
            Assert.assertEquals(unexpectedMsgLogEvent.logSource(), actorRef.path().toString());
            String unexpectedMsg = (String) unexpectedMsgLogEvent.message();
            Assert.assertTrue(unexpectedMsg.contains("unexpected"));
            Assert.assertTrue(unexpectedMsg.contains(PROXY_CONTEXT.getModule().getInPorts().get(1).toString()));
        }
    }

    /**
     * Verifies that the actor does not terminate before {@link InPortHasSignal} messages have been received for all
     * recomputed in-ports.
     */
    @Test
    public void inPortHasSignalAsynchronousActions() {
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("inPortHasSignal", PROXY_CONTEXT).build()) {
            MapStagingArea stagingArea = moduleInterpretation.getStagingArea();
            put(stagingArea, ":in:y");

            int moduleId = 3;
            Props props = moduleInterpretation.toCompositeModuleInterpreterProps(3, bitSetOf(0), bitSetOf(1));
            ActorSystem actorSystem = moduleInterpretation.getActorSystem();
            TestActorRef<CompositeModuleInterpreterActor> actorRef
                = TestActorRef.create(actorSystem, props, moduleInterpretation.getSupervisor());

            // Empty the executor queue, and subsequently verify that the in-port value has been copied and that the
            // parent was informed about the new out-port value.
            CallingThreadExecutor asyncTaskExecutor = moduleInterpretation.getAsyncTaskExecutor();
            asyncTaskExecutor.executeAll();
            TestProbe parentProbe = moduleInterpretation.getParentProbe();
            Assert.assertEquals(
                parentProbe.expectMsgClass(Object.class),
                new SubmoduleOutPortHasSignal(moduleId, 1)
            );

            // Verify actor is still alive, because not all expected InPortHasSignal messages have been received yet
            parentProbe.watch(actorRef);
            Assert.assertFalse(parentProbe.msgAvailable());

            // Verify that the actor terminates properly once the last asynchronous action is finished
            actorRef.receive(new InPortHasSignal(0));
            parentProbe.expectMsgClass(Terminated.class);
            Assert.assertFalse(parentProbe.msgAvailable());
        }
    }

    /**
     * Verifies correct handling of {@link SubmoduleOutPortHasSignal} messages.
     */
    @Test
    public void submoduleOutPortHasSignal() {
        try (
            ModuleInterpretation moduleInterpretation = testCaseBuilder("submoduleOutPortHasSignal", PROXY_CONTEXT)
                .build()
        ) {
            MapStagingArea stagingArea = moduleInterpretation.getStagingArea();
            Map<ExecutionTrace, ObjectNode> stagingAreaMap = stagingArea.toUnmodifiableMap();
            put(stagingArea, ":in:x", ":in:y");

            Props props = moduleInterpretation.toCompositeModuleInterpreterProps(3, bitSetOf(), bitSetOf(0, 1));
            ActorSystem actorSystem = moduleInterpretation.getActorSystem();
            TestActorRef<CompositeModuleInterpreterActor> actorRef = TestActorRef.create(
                moduleInterpretation.getActorSystem(), props, moduleInterpretation.getSupervisor());

            TestProbe akkaEventProbe = new TestProbe(actorSystem);
            actorSystem.eventStream().subscribe(akkaEventProbe.ref(), Object.class);

            // Empty the executor queue, and subsequently verify that the appropriate actions happened as a result of
            // the message and starting the interpretation.
            CallingThreadExecutor asyncTaskExecutor = moduleInterpretation.getAsyncTaskExecutor();
            asyncTaskExecutor.executeAll();

            Assert.assertEquals(
                stagingAreaMap.keySet(),
                tracesSet(":in:x", ":in:y", "/a:in:ax", "/a:in:ay", "/b:in:bx", ":out:q")
            );
            TestProbe aProbe = moduleInterpretation.getTestProbeForChild("a");
            aProbe.expectMsgClass(InPortHasSignal.class);
            aProbe.expectMsgClass(InPortHasSignal.class);
            Assert.assertFalse(aProbe.msgAvailable());

            int aId = submodule(moduleInterpretation, "a").getIndex();
            put(stagingArea, "/a:out:ap");
            actorRef.receive(new SubmoduleOutPortHasSignal(aId, 0));

            // Verify warning if the SubmoduleOutPortHasSignal message mentions a port that already provided a value
            Assert.assertFalse(akkaEventProbe.msgAvailable());
            actorRef.receive(new SubmoduleOutPortHasSignal(aId, 0));
            LogEvent redundantMsgLogEvent = akkaEventProbe.expectMsgClass(LogEvent.class);
            Assert.assertEquals(redundantMsgLogEvent.logSource(), actorRef.path().toString());
            String redundantMsg = (String) redundantMsgLogEvent.message();
            Assert.assertTrue(redundantMsg.contains("redundant"));
            RuntimeParentModule parentModule = (RuntimeParentModule) moduleInterpretation.getResolvedModule();
            RuntimeModule submodule = Objects.requireNonNull(
                parentModule.getEnclosedElement(RuntimeModule.class, SimpleName.identifier("a")));
            Assert.assertTrue(redundantMsg.contains(submodule.getOutPorts().get(0).toString()));

            // Verify warning if the SubmoduleOutPortHasSignal message mentions a port that is not needed
            Assert.assertFalse(akkaEventProbe.msgAvailable());
            actorRef.receive(new SubmoduleOutPortHasSignal(aId, 1));
            LogEvent unexpectedMsgLogEvent = akkaEventProbe.expectMsgClass(LogEvent.class);
            Assert.assertEquals(unexpectedMsgLogEvent.logSource(), actorRef.path().toString());
            String unexpectedMsg = (String) unexpectedMsgLogEvent.message();
            Assert.assertTrue(unexpectedMsg.contains("unexpected"));
            Assert.assertTrue(unexpectedMsg.contains(submodule.getOutPorts().get(1).toString()));

            // Verify that all successors receive values as events are processed. Note that the value of out-port a#ap
            // will no longer be needed, so submodule a will have no needed out-ports any more and cleaning will happen.
            asyncTaskExecutor.executeAll();
            Assert.assertEquals(
                stagingAreaMap.keySet(),
                tracesSet(":in:x", ":in:y", "/b:in:bx", "/b:in:by", ":out:q")
            );
        }
    }

    @Test
    public void endOfInterpretation() {
        try (
            ModuleInterpretation moduleInterpretation = testCaseBuilder("endOfInterpretation", PROXY_CONTEXT)
                .setCleaning(false)
                .build()
        ) {
            int moduleId = 3;
            MapStagingArea stagingArea = moduleInterpretation.getStagingArea();
            Map<ExecutionTrace, ObjectNode> stagingAreaMap = stagingArea.toUnmodifiableMap();
            put(stagingArea, "/b:in:bx", "/b:in:by", ":out:q");

            Props props = moduleInterpretation.toCompositeModuleInterpreterProps(moduleId, bitSetOf(), bitSetOf(0, 1));
            ActorSystem actorSystem = moduleInterpretation.getActorSystem();
            TestActorRef<CompositeModuleInterpreterActor> actorRef = TestActorRef.create(
                moduleInterpretation.getActorSystem(), props, moduleInterpretation.getSupervisor());

            TestProbe akkaEventProbe = new TestProbe(actorSystem);
            actorSystem.eventStream().subscribe(akkaEventProbe.ref(), Object.class);

            // Empty the executor queue, and subsequently verify that the appropriate actions happened as a result of
            // the message and starting the interpretation.
            CallingThreadExecutor asyncTaskExecutor = moduleInterpretation.getAsyncTaskExecutor();
            asyncTaskExecutor.executeAll();
            Assert.assertEquals(
                stagingAreaMap.keySet(),
                tracesSet("/b:in:bx", "/b:in:by", ":out:q")
            );
            TestProbe parentProbe = moduleInterpretation.getParentProbe();
            parentProbe.expectMsg(new SubmoduleOutPortHasSignal(moduleId, 1));
            Assert.assertFalse(parentProbe.msgAvailable());

            // Verify that an unexpected Terminated message is ignored
            actorRef.receive(new Terminated(moduleInterpretation.getSupervisor(), true, true));
            LogEvent unexpectedMsgLogEvent = akkaEventProbe.expectMsgClass(LogEvent.class);
            Assert.assertEquals(unexpectedMsgLogEvent.logSource(), actorRef.path().toString());
            String unexpectedMsg = (String) unexpectedMsgLogEvent.message();
            Assert.assertTrue(unexpectedMsg.contains("unexpected") && unexpectedMsg.contains("unknown"));

            // Verify that tge second SubmoduleOutPortHasSignal message is sent properly when submodule b produces a
            // value
            int bId = submodule(moduleInterpretation, "b").getIndex();
            put(stagingArea, "/b:out:bp");
            actorRef.receive(new SubmoduleOutPortHasSignal(bId, 0));
            asyncTaskExecutor.executeAll();
            parentProbe.expectMsg(new SubmoduleOutPortHasSignal(moduleId, 0));
            Assert.assertFalse(parentProbe.msgAvailable());

            // Verify that the actor terminates properly after the last submodule interpreter has finished.
            ActorRef childActor = actor(actorSystem, actorRef.path().child("b"));
            childActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            parentProbe.watch(actorRef);
            parentProbe.expectMsgClass(Terminated.class);
            Assert.assertFalse(parentProbe.msgAvailable());
        }
    }

    private static final String EXPECTED_MSG = "This is an expected exception!";

    private static final InvokeRequest THROW_EXCEPTION_MESSAGE
        = new InvokeRequest(() -> { throw new IllegalStateException(EXPECTED_MSG); });

    @Test
    public void childException() {
        try (ModuleInterpretation moduleInterpretation = testCaseBuilder("childException", PROXY_CONTEXT).build()) {
            MapStagingArea stagingArea = moduleInterpretation.getStagingArea();
            put(stagingArea, "/b:in:bx", "/b:in:by", ":out:q");

            Props props = moduleInterpretation.toCompositeModuleInterpreterProps(3, bitSetOf(), bitSetOf(0, 1));
            ActorSystem actorSystem = moduleInterpretation.getActorSystem();
            TestActorRef<CompositeModuleInterpreterActor> actorRef = TestActorRef.create(
                moduleInterpretation.getActorSystem(), props, moduleInterpretation.getSupervisor(), "interpreter");

            // Empty the executor queue, and subsequently verify that the appropriate actions happened as a result of
            // the message and starting the interpretation.
            CallingThreadExecutor asyncTaskExecutor = moduleInterpretation.getAsyncTaskExecutor();
            asyncTaskExecutor.executeAll();
            TestProbe parentProbe = moduleInterpretation.getParentProbe();
            parentProbe.expectMsgClass(SubmoduleOutPortHasSignal.class);
            Assert.assertFalse(parentProbe.msgAvailable());

            ActorRef childActor = actor(actorSystem, actorRef.path().child("b"));
            childActor.tell(THROW_EXCEPTION_MESSAGE, ActorRef.noSender());
            try {
                asyncTaskExecutor.executeAll();
                Assert.fail();
            } catch (UncaughtThrowableException exception) {
                InterpreterException interpreterException = (InterpreterException) exception.getCause();
                Assert.assertEquals(
                    interpreterException.getExecutionTrace(),
                    CompositeModuleContext.TEST_MODULE_TRACE.resolveExecutionTrace(ExecutionTrace.valueOf("/b"))
                );
                IllegalStateException childException = (IllegalStateException) interpreterException.getCause();
                Assert.assertEquals(childException.getMessage(), EXPECTED_MSG);
            }

            // The Akka manual says: "the watching actor will receive a Terminated message even if the watched actor has
            // already been terminated at the time of registration."
            parentProbe.watch(actorRef);
            parentProbe.expectMsgClass(Terminated.class);
            Assert.assertFalse(parentProbe.msgAvailable());
        }
    }

    /**
     * Akka logger that just swallows the log message. It is used in tests where a {@link akka.event.LoggingAdapter}
     * methods are expected to be called, but the results are not supposed to appear in stdout.
     */
    @SuppressWarnings("unused")
    public static class SwallowingLogger extends UntypedActor {
        @Override
        public void onReceive(Object message) {
            if (message instanceof Logging.InitializeLogger) {
                getSender().tell(Logging.loggerInitialized(), getSelf());
            }
        }
    }
}
