package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.dispatch.ExecutionContexts;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.testkit.TestProbe;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.Assert;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import xyz.cloudkeeper.interpreter.DependencyGraph.HasValue;
import xyz.cloudkeeper.interpreter.event.Event;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclaration;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;
import xyz.cloudkeeper.staging.MapStagingArea;
import xyz.cloudkeeper.testkit.CallingThreadExecutor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ModuleInterpretation implements AutoCloseable {
    static final long DEFAULT_TIMEOUT_MILLIS = 1000;
    static final FiniteDuration DEFAULT_DURATION = Duration.create(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    private final CallingThreadExecutor asyncTaskExecutor = new CallingThreadExecutor();
    private final ActorSystem actorSystem;
    private final LocalInterpreterProperties localInterpreterProperties;
    private final TestProbe eventProbe;
    private final TestProbe administratorProbe;
    private final TestProbe executorProbe;
    private final TestProbe parentProbe;
    private final Map<String, TestProbe> childProbes;
    private final TestActorRef<ForwardingActor> supervisor;
    private final InterpreterPropsProvider interpreterPropsProvider;
    private final MapStagingArea stagingArea;

    public static final class Builder {
        private final String name;
        private final RuntimeRepository repository;
        private final RuntimeAnnotatedExecutionTrace executionTrace;
        private boolean cleaning = true;
        private long executionId = 1001;
        @Nullable private Map<String, ?> config;

        Builder(String name, RuntimeRepository repository, RuntimeAnnotatedExecutionTrace executionTrace) {
            this.name = name;
            this.repository = repository;
            this.executionTrace = executionTrace;
        }

        public Builder setCleaning(boolean cleaning) {
            this.cleaning = cleaning;
            return this;
        }

        public Builder setExecutionId(long executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder setConfig(Map<String, ?> config) {
            this.config = config;
            return this;
        }

        ModuleInterpretation build() {
            return new ModuleInterpretation(this);
        }
    }

    private ModuleInterpretation(Builder builder) {
        Config config = builder.config != null
            ? ConfigFactory.parseMap(builder.config).withFallback(ConfigFactory.load())
            : ConfigFactory.load();
        actorSystem = ActorSystem.create(builder.name, config);
        eventProbe = new TestProbe(actorSystem);
        administratorProbe = new TestProbe(actorSystem);
        executorProbe = new TestProbe(actorSystem);
        parentProbe = new TestProbe(actorSystem);

        childProbes = new LinkedHashMap<>();
        InterpreterProperties interpreterProperties = new InterpreterProperties(
            builder.cleaning,
            administratorProbe.ref(),
            executorProbe.ref(),
            ImmutableList.of(new EventSubscription(eventProbe.ref()))
        );
        InterpreterEventBus eventBus = new InterpreterEventBus();
        interpreterProperties.getEventSubscriptions()
            .forEach(eventSubscription -> eventBus.subscribe(eventSubscription.getActorRef(), Event.class));

        RuntimeContext runtimeContext = mock(RuntimeContext.class);
        when(runtimeContext.getRepository()).thenReturn(builder.repository);

        stagingArea = new MapStagingArea(runtimeContext, builder.executionTrace);

        RuntimeModule module = resolveModule(stagingArea.getAnnotatedExecutionTrace().getModule());
        if (module instanceof RuntimeParentModule) {
            for (RuntimeModule childModule : ((RuntimeParentModule) module).getModules()) {
                @Nullable SimpleName moduleName = childModule.getSimpleName();
                assert moduleName != null;
                childProbes.put(moduleName.toString(), new TestProbe(actorSystem));
            }
        }

        supervisor = TestActorRef.create(
            actorSystem,
            Props.create(new ForwardingActor.Factory(parentProbe, asyncTaskExecutor)),
            "supervisor"
        );
        ExecutionContextExecutor asyncExecutionContext = ExecutionContexts.fromExecutor(asyncTaskExecutor);
        localInterpreterProperties = new LocalInterpreterProperties(
            interpreterProperties, builder.executionId, runtimeContext, asyncExecutionContext, eventBus);

        interpreterPropsProvider = (
            LocalInterpreterProperties ignoredInterpreterProperties,
            StagingArea childStagingArea,
            int ignoredModuleId,
            List<HasValue> ignoredInPortHasValueList,
            BitSet ignoredRecomputedInPorts,
            BitSet ignoredRequestedOutPorts
        ) -> {
            RuntimeModule childModule = childStagingArea.getAnnotatedExecutionTrace().getModule();
            @Nullable TestProbe childProbe = childProbes.get(childModule.getSimpleName().toString());
            assert childProbe != null
                : String.format("Could not find %s for %s.", TestProbe.class.getSimpleName(), module);
            return Props.create(new ForwardingActor.Factory(childProbe, asyncTaskExecutor))
                .withDispatcher("akka.test.calling-thread-dispatcher");
        };
    }

    private static RuntimeModule resolveModule(RuntimeModule module) {
        if (module instanceof RuntimeProxyModule) {
            RuntimeModuleDeclaration declaration = ((RuntimeProxyModule) module).getDeclaration();
            if (declaration instanceof RuntimeCompositeModuleDeclaration) {
                return ((RuntimeCompositeModuleDeclaration) declaration).getTemplate();
            }
        }
        return module;
    }

    @Override
    public void close() {
        JavaTestKit.shutdownActorSystem(actorSystem);
    }

    CallingThreadExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }

    ActorSystem getActorSystem() {
        return actorSystem;
    }

    LocalInterpreterProperties getLocalInterpreterProperties() {
        return localInterpreterProperties;
    }

    ActorRef getSupervisor() {
        return supervisor;
    }

    MapStagingArea getStagingArea() {
        return stagingArea;
    }

    RuntimeModule getResolvedModule() {
        return resolveModule(stagingArea.getAnnotatedExecutionTrace().getModule());
    }

    TestProbe getEventProbe() {
        return eventProbe;
    }

    TestProbe getAdministratorProbe() {
        return administratorProbe;
    }

    TestProbe getExecutorProbe() {
        return executorProbe;
    }

    TestProbe getParentProbe() {
        return parentProbe;
    }

    TestProbe getTestProbeForChild(String childModuleName) {
        @Nullable TestProbe testProbe = childProbes.get(childModuleName);
        if (testProbe == null) {
            throw new IllegalArgumentException(String.format(
                "Expected name of child module, but got '%s'. No %s could be found.",
                childModuleName, TestProbe.class.getSimpleName()
            ));
        }
        return testProbe;
    }

    static BitSet bitSetOf(int... elements) {
        return Arrays.stream(elements).collect(BitSet::new, BitSet::set, BitSet::or);
    }

    static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            Assert.fail("Future did not complete normally.", exception);
            throw new AssertionError("unreachable", exception);
        }
    }

    static <T> T await(Future<T> future) {
        try {
            return Await.result(future, DEFAULT_DURATION);
        } catch (Exception exception) {
            Assert.fail("Future did not complete normally.", exception);
            throw new AssertionError("unreachable", exception);
        }
    }

    public Props toSimpleModuleInterpreterProps(int moduleId, BitSet recomputedInPorts, BitSet requestedOutPorts) {
        return Props.create(new SimpleModuleInterpreterActor.Factory(
            localInterpreterProperties,
            stagingArea,
            moduleId,
            recomputedInPorts,
            requestedOutPorts
        ));
    }

    public Props toCompositeModuleInterpreterProps(int moduleId, BitSet recomputedInPorts, BitSet requestedOutPorts) {
        return Props.create(new CompositeModuleInterpreterActor.Factory(
            localInterpreterProperties,
            stagingArea,
            interpreterPropsProvider,
            moduleId,
            ImmutableList.copyOf(
                Collections.nCopies(
                    stagingArea.getAnnotatedExecutionTrace().getModule().getInPorts().size(),
                    HasValue.UNKNOWN
                )
            ),
            recomputedInPorts,
            requestedOutPorts
        ));
    }

    private static final class ForwardingActor extends UntypedActor {
        private final TestProbe receivingProbe;
        private final CallingThreadExecutor asyncTaskExecutor;

        private static final class Factory implements Creator<ForwardingActor> {
            private static final long serialVersionUID = 314355822603349450L;

            private final TestProbe receivingProbe;
            private final CallingThreadExecutor asyncTaskExecutor;

            private Factory(TestProbe receivingProbe, CallingThreadExecutor asyncTaskExecutor) {
                this.receivingProbe = receivingProbe;
                this.asyncTaskExecutor = asyncTaskExecutor;
            }

            private void readObject(ObjectInputStream stream) throws IOException {
                throw new NotSerializableException(getClass().getName());
            }

            private void writeObject(ObjectOutputStream stream) throws IOException {
                throw new NotSerializableException(getClass().getName());
            }

            @Override
            public ForwardingActor create() throws Exception {
                return new ForwardingActor(this);
            }
        }

        private ForwardingActor(Factory factory) {
            receivingProbe = factory.receivingProbe;
            asyncTaskExecutor = factory.asyncTaskExecutor;
        }

        private SupervisorStrategy.Directive supervisorDirective(Throwable throwable) {
            // We cannot just re-throw the exception here because it would be caught by the actor system (which would
            // just restart the actor). We therefore schedule a deferred exception directly in the
            // CallingThreadExecutor.
            asyncTaskExecutor.execute(() -> { throw new UncaughtThrowableException(throwable); });
            return SupervisorStrategy.stop();
        }

        private final SupervisorStrategy supervisorStrategy
            = new AllForOneStrategy(0, Duration.Inf(), this::supervisorDirective, false);

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return supervisorStrategy;
        }

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof InvokeRequest) {
                ((InvokeRequest) message).invoke();
            } else {
                receivingProbe.ref().forward(message, getContext());
            }
        }
    }
}
