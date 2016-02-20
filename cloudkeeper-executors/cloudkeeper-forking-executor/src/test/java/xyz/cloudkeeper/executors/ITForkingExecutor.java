package xyz.cloudkeeper.executors;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.examples.modules.BinarySum;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.simple.SimpleInstanceProvider;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ITForkingExecutor {
    private static final long AWAIT_DURATION_MILLIS = 30_000;

    private Path tempDir;
    private ExecutorService executorService;
    private ForkingExecutor forkingExecutor;

    @BeforeClass
    public void setup() throws IOException, URISyntaxException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        executorService = Executors.newFixedThreadPool(2);
        forkingExecutor = new ForkingExecutor(
            executorService,
            DummyCommandProvider.INSTANCE,
            new SimpleInstanceProvider.Builder(executorService).build()
        );
    }

    public void tearDown() throws IOException {
        executorService.shutdownNow();
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
    }

    enum DummyCommandProvider implements CommandProvider {
        INSTANCE;

        @Override
        public List<String> getCommand(RuntimeAnnotatedExecutionTrace executionTrace) {
            // In order to be able to attach a debugger to the forked Java process, pass
            // "-agentlib:jdwp=transport=dt_socket,quiet=y,server=y,suspend=y,address=5005"
            // as additional argument to DummyProcess#command(String...).
            return DummyProcess.command();
        }
    }

    @Test
    public void testForking() throws Exception {
        RuntimeStateProvider runtimeStateProvider
            = StagingAreas.runtimeStateProviderForDSLModule(BinarySum.class, tempDir, executorService);
        SimpleModuleExecutorResult result
            = forkingExecutor.submit(runtimeStateProvider).get(AWAIT_DURATION_MILLIS, TimeUnit.MILLISECONDS);

        Assert.assertNotNull(result.getExecutionException());
        Assert.assertEquals(result.getExecutionException().getMessage(), DummyProcess.EXECUTION_EXCEPTION_MSG);
    }
}
