package xyz.cloudkeeper.executors;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
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

import javax.annotation.Nullable;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ITForkedExecutors {
    private static final long AWAIT_DURATION_MILLIS = 1000;

    @Nullable private Path tempDir;
    @Nullable private ExecutorService executorService;

    @BeforeClass
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getName());
        executorService = Executors.newCachedThreadPool();
    }

    @AfterClass
    public void tearDown() throws IOException {
        assert tempDir != null && executorService != null;
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

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(AWAIT_DURATION_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testExecution() throws Exception {
        assert executorService != null;

        DSLRuntimeContextFactory runtimeContextFactory = new DSLRuntimeContextFactory.Builder(executorService).build();
        URI bundleURI = new URI(Module.URI_SCHEME, Decrease.class.getName(), null);
        try (RuntimeContext runtimeContext
                 = await(runtimeContextFactory.newRuntimeContext(Collections.singletonList(bundleURI)))) {
            RuntimeAnnotatedExecutionTrace rootTrace = runtimeContext.newAnnotatedExecutionTrace(
                ExecutionTrace.empty(),
                new MutableProxyModule().setDeclaration(Decrease.class.getName()),
                Collections.<BareOverride>emptyList()
            );

            Path stagingAreaBasePath = Files.createDirectory(tempDir.resolve("staging-area"));
            StagingArea stagingArea = new FileStagingArea.Builder(runtimeContext, rootTrace,
                    stagingAreaBasePath, executorService)
                .build();
            await(stagingArea.putObject(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("num")), 5));

            RuntimeStateProvider runtimeStateProvider = RuntimeStateProvider.of(runtimeContext, stagingArea);

            InstanceProvider instanceProvider = new SimpleInstanceProvider.Builder(executorService)
                .setRuntimeContextFactory(runtimeContextFactory)
                .build();
            ModuleConnectorProvider moduleConnectorProvider = new PrefetchingModuleConnectorProvider(tempDir);
            SimpleModuleExecutor simpleModuleExecutor
                    = new LocalSimpleModuleExecutor.Builder(executorService, moduleConnectorProvider)
                .setInstanceProvider(instanceProvider)
                .build();

            // Finally, run the simple module
            SimpleModuleExecutorResult result = run(simpleModuleExecutor, runtimeStateProvider);
            Assert.assertNull(result.getExecutionException(), "Module execution failed.");

            // Sanity check that the correct out-port value was produced
            Assert.assertEquals(
                await(stagingArea.getObject(ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("result")))),
                4
            );
        }
    }
}
