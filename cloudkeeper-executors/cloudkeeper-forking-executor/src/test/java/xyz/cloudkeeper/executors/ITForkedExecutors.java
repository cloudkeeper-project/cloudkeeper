package xyz.cloudkeeper.executors;

import akka.dispatch.ExecutionContexts;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import xyz.cloudkeeper.dsl.Module;
import xyz.cloudkeeper.examples.modules.Decrease;
import xyz.cloudkeeper.filesystem.FileStagingArea;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.beans.element.module.MutableProxyModule;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.simple.CharacterStreamCommunication.Splitter;
import xyz.cloudkeeper.simple.DSLRuntimeContextFactory;
import xyz.cloudkeeper.simple.LocalSimpleModuleExecutor;
import xyz.cloudkeeper.simple.PrefetchingModuleConnectorProvider;
import xyz.cloudkeeper.simple.SimpleInstanceProvider;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ITForkedExecutors {
    private static final Duration AWAIT_DURATION = Duration.create(1, TimeUnit.SECONDS);

    private Path tempDir;
    private ExecutorService executorService;
    private ExecutionContext executionContext;

    @BeforeClass
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getName());
        executorService = Executors.newCachedThreadPool();
        executionContext = ExecutionContexts.fromExecutorService(executorService);
    }

    @AfterClass
    public void tearDown() throws IOException {
        executorService.shutdownNow();
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
    }

    private static SimpleModuleExecutorResult run(SimpleModuleExecutor simpleModuleExecutor,
            RuntimeStateProvider runtimeStateProvider) throws IOException {
        byte[] serializedStagingArea;
        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(runtimeStateProvider);
            serializedStagingArea = byteArrayOutputStream.toByteArray();
        }

        byte[] serializedExecutionResult;
        try (
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedStagingArea);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            ForkedExecutors.run(simpleModuleExecutor, byteArrayInputStream, byteArrayOutputStream);
            serializedExecutionResult = byteArrayOutputStream.toByteArray();
        }

        try (
            Splitter<SimpleModuleExecutorResult> splitter = new Splitter<>(
                SimpleModuleExecutorResult.class,
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(serializedExecutionResult)))
            )
        ) {
            splitter.consumeAll();
            return splitter.getResult();
        }
    }

    @Test
    public void testExecution() throws Exception {
        DSLRuntimeContextFactory runtimeContextFactory = new DSLRuntimeContextFactory.Builder(executionContext).build();
        URI bundleURI = new URI(Module.URI_SCHEME, Decrease.class.getName(), null);
        try (RuntimeContext runtimeContext = Await.result(
                runtimeContextFactory.newRuntimeContext(Collections.singletonList(bundleURI)), AWAIT_DURATION)) {
            RuntimeAnnotatedExecutionTrace rootTrace = runtimeContext.newAnnotatedExecutionTrace(
                ExecutionTrace.empty(),
                new MutableProxyModule().setDeclaration(Decrease.class.getName()),
                Collections.<BareOverride>emptyList()
            );

            Path stagingAreaBasePath = Files.createDirectory(tempDir.resolve("staging-area"));
            StagingArea stagingArea = new FileStagingArea.Builder(runtimeContext, rootTrace,
                    stagingAreaBasePath, executionContext)
                .build();
            Await.result(
                stagingArea.putObject(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("num")), 5),
                AWAIT_DURATION
            );

            RuntimeStateProvider runtimeStateProvider = RuntimeStateProvider.of(runtimeContext, stagingArea);

            InstanceProvider instanceProvider = new SimpleInstanceProvider.Builder(executionContext)
                .setRuntimeContextFactory(runtimeContextFactory)
                .build();
            ModuleConnectorProvider moduleConnectorProvider
                = new PrefetchingModuleConnectorProvider(tempDir, executionContext);
            SimpleModuleExecutor simpleModuleExecutor
                    = new LocalSimpleModuleExecutor.Builder(executionContext, moduleConnectorProvider)
                .setInstanceProvider(instanceProvider)
                .build();

            // Finally, run the simple module
            SimpleModuleExecutorResult result = run(simpleModuleExecutor, runtimeStateProvider);

            if (result.getExecutionException().isDefined()) {
                Assert.fail("Module execution failed.", result.getExecutionException().get());
            }

            // Sanity check that the correct out-port value was produced
            Assert.assertEquals(
                Await.result(
                    stagingArea.getObject(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("result"))),
                    AWAIT_DURATION
                ),
                4
            );
        }
    }
}
