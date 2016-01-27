package com.svbio.cloudkeeper.simple;

import akka.dispatch.ExecutionContexts;
import com.svbio.cloudkeeper.contracts.ModuleConnectorProviderContract;
import com.svbio.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.staging.MapStagingArea;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PrefetchingModuleConnectorProviderTest {
    private Path tempDir;
    private ExecutorService executorService;

    public void setup() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        executorService = Executors.newFixedThreadPool(1);
    }

    @AfterSuite
    public void tearDown() throws IOException {
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
        executorService.shutdownNow();
    }

    @Factory
    public Object[] contractTests() throws IOException {
        setup();

        ModuleConnectorProvider moduleConnectorProvider = new PrefetchingModuleConnectorProvider(
            tempDir,
            ExecutionContexts.fromExecutorService(executorService)
        );
        FiniteDuration awaitDuration = Duration.create(1, TimeUnit.SECONDS);
        return new Object[] {
            new ModuleConnectorProviderContract(
                moduleConnectorProvider,
                (identifier, runtimeContext, executionTrace) -> new MapStagingArea(runtimeContext, executionTrace),
                awaitDuration
            )
        };
    }
}
