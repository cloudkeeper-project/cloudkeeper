package com.svbio.cloudkeeper.executors;

import akka.dispatch.ExecutionContexts;
import com.svbio.cloudkeeper.examples.modules.BinarySum;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ITForkingExecutor {
    private static final Duration AWAIT_DURATION = Duration.create(30, TimeUnit.SECONDS);

    private Path tempDir;
    private ExecutorService executorService;
    private ExecutionContext executionContext;
    private ForkingExecutor forkingExecutor;

    @BeforeClass
    public void setup() throws IOException, URISyntaxException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        executorService = Executors.newFixedThreadPool(2);
        executionContext = ExecutionContexts.fromExecutorService(executorService);
        forkingExecutor = new ForkingExecutor.Builder(executionContext, executionContext, DummyCommandProvider.INSTANCE)
            .build();
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
            = StagingAreas.runtimeStateProviderForDSLModule(BinarySum.class, tempDir, executionContext);
        SimpleModuleExecutorResult result = Await.result(
            forkingExecutor.submit(runtimeStateProvider, null),
            AWAIT_DURATION
        );
        Assert.assertEquals(result.getExecutionException().get().getMessage(), DummyProcess.EXECUTION_EXCEPTION_MSG);
    }
}
