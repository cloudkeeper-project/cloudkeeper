package xyz.cloudkeeper.drm;

import akka.dispatch.ExecutionContexts;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import xyz.cloudkeeper.examples.modules.BinarySum;
import xyz.cloudkeeper.executors.CommandProvider;
import xyz.cloudkeeper.executors.DummyProcess;
import xyz.cloudkeeper.executors.StagingAreas;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tests submitting a DRMAA job that runs {@link DummyProcess#main(String[])}, which reads a
 * {@link RuntimeStateProvider} from stdin and writes a {@link SimpleModuleExecutorResult} back to stdout.
 *
 * <p>In order to run this integration test, the DRMAA environment needs to be set up properly. For instance, when using
 * Sun/Oracle/Open Grid Engine, the following steps are necessary:
 * <ul><li>
 *     Define system property {@code org.ggf.drmaa.SessionFactory} (in the case of Grid Engine, this would be
 *     {@code com.sun.grid.drmaa.SessionFactoryImpl}). If the system property is not set, a {@link SkipException} will
 *     be thrown in {@link #setup()}.
 * </li><li>
 *     Define environment variable {@code SGE_ROOT} so that it contains the Grid Engine root directory.
 * </li><li>
 *     Ensure that the directory with the native DRMAA libraries is contained in system property
 *     {@code java.library.path}. This is typically (shell script notation)
 *     {@code ${SGE_ROOT}/lib/$(${SGE_ROOT}/util/arch)}. On Mac OS X, for instance, this is
 *     {@code ${SGE_ROOT}/lib/darwin-x64}.
 *     One way to achieve this is to add this directory to the {@code LD_LIBRARY_PATH} (or {@code DYLD_LIBRARY_PATH} on
 *     Mac OS X) environment variable.
 * </li><li>
 *     Add {@code ${SGE_ROOT}/lib/drmaa.jar} to the classpath.
 * </li></ul>
 *
 * <p>With Maven, use the following command-line options (example again for Grid Engine):
 * <ul><li>
 *     {@code -Dmaven.test.additionalClasspath=${SGE_ROOT}/lib/drmaa.jar} ({@code SGE_ROOT} needs to be substituted by
 *     the shell, of course)
 * </li><li>
 *     {@code -DsystemPropertiesFile=/path/to/properties/file} (which contains a line such as
 *     {@code org.ggf.drmaa.SessionFactory = com.sun.grid.drmaa.SessionFactoryImpl})
 * </li></ul>
 */
public class ITDrmaaSimpleModuleExecutor {
    private static final Duration AWAIT_DURATION = Duration.create(30, TimeUnit.DAYS);

    private Session drmaaSession;
    private Path tempDir;
    private DrmaaSimpleModuleExecutor drmaaExecutor;
    private ExecutionContext executionContext;
    private ScheduledExecutorService executorService;

    @BeforeClass
    public void setup() throws DrmaaException, IOException {
        if (System.getProperty("org.ggf.drmaa.SessionFactory") == null) {
            throw new SkipException(
                "Could not find system property org.ggf.drmaa.SessionFactory."
            );
        }

        executorService = Executors.newScheduledThreadPool(4);
        executionContext = ExecutionContexts.fromExecutorService(executorService);

        drmaaSession = SessionFactory.getFactory().getSession();
        drmaaSession.init("");

        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        drmaaExecutor = new DrmaaSimpleModuleExecutor.Builder(drmaaSession, tempDir, DummyCommandProvider.INSTANCE,
                executionContext, executorService)
            .build();
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

    @AfterClass
    public void tearDown() throws DrmaaException, IOException {
        drmaaSession.exit();
        executorService.shutdownNow();
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
    }

    @Test
    public void testDrmaaSubmission() throws Exception {
        RuntimeStateProvider runtimeStateProvider
            = StagingAreas.runtimeStateProviderForDSLModule(BinarySum.class, tempDir, executionContext);
        SimpleModuleExecutorResult result = Await.result(
            drmaaExecutor.submit(runtimeStateProvider, null),
            AWAIT_DURATION
        );
        Assert.assertEquals(result.getExecutionException().get().getMessage(), DummyProcess.EXECUTION_EXCEPTION_MSG);

        // Wait just a little bit so that tearDown() and clean-up by the DRMAA executor do not interfere with their
        // cleaning.
        Thread.sleep(100);
    }
}
