package com.svbio.cloudkeeper.simple;

import akka.dispatch.ExecutionContexts;
import com.svbio.cloudkeeper.contracts.ModuleExecutorContract;
import com.svbio.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.staging.MapStagingArea;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleExecutorTest {
    private ExecutorService executorService;
    private Path tempDir;
    private LocalSimpleModuleExecutor simpleExecutor;

    public void setup() throws IOException {
        executorService = Executors.newFixedThreadPool(1);
        ExecutionContext executionContext = ExecutionContexts.fromExecutorService(executorService);
        tempDir = Files.createTempDirectory(getClass().getSimpleName());

        ModuleConnectorProvider connectorProvider = new PrefetchingModuleConnectorProvider(tempDir, executionContext);
        simpleExecutor = new LocalSimpleModuleExecutor.Builder(executionContext, connectorProvider)
            .build();
    }

    @AfterSuite
    public void tearDown() throws IOException {
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
        executorService.shutdownNow();
    }

    @Factory
    public Object[] contractTests() throws IOException {
        setup();

        FiniteDuration awaitDuration = Duration.create(1, TimeUnit.SECONDS);
        return new Object[] {
            new ModuleExecutorContract(
                simpleExecutor,
                (identifier, runtimeContext, executionTrace) -> new MapStagingArea(runtimeContext, executionTrace),
                awaitDuration
            )
        };
    }
}
