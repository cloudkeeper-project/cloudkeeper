package xyz.cloudkeeper.simple;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;
import xyz.cloudkeeper.contracts.ModuleConnectorProviderContract;
import xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.staging.MapStagingArea;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrefetchingModuleConnectorProviderTest {
    private static final long WAIT_DURATION_MILLIS = 1000;

    @Nullable private Path tempDir;
    @Nullable private ExecutorService executorService;

    public void setup() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        executorService = Executors.newFixedThreadPool(1);
    }

    @AfterSuite
    public void tearDown() throws IOException {
        assert tempDir != null && executorService != null;
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
        executorService.shutdownNow();
    }

    @Factory
    public Object[] contractTests() throws IOException {
        setup();
        assert tempDir != null && executorService != null;

        ModuleConnectorProvider moduleConnectorProvider = new PrefetchingModuleConnectorProvider(tempDir);
        return new Object[] {
            new ModuleConnectorProviderContract(
                moduleConnectorProvider,
                (identifier, runtimeContext, executionTrace) -> new MapStagingArea(runtimeContext, executionTrace),
                WAIT_DURATION_MILLIS
            )
        };
    }
}
