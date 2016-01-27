package com.svbio.cloudkeeper.executors;

import akka.dispatch.ExecutionContexts;
import com.svbio.cloudkeeper.dsl.Module;
import com.svbio.cloudkeeper.examples.modules.Decrease;
import com.svbio.cloudkeeper.filesystem.FileStagingArea;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.simple.CharacterStreamCommunication.Splitter;
import com.svbio.cloudkeeper.simple.DSLRuntimeContextFactory;
import com.svbio.cloudkeeper.simple.LocalSimpleModuleExecutor;
import com.svbio.cloudkeeper.simple.PrefetchingModuleConnectorProvider;
import com.svbio.cloudkeeper.simple.SimpleInstanceProvider;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

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
