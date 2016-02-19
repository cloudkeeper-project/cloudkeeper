package xyz.cloudkeeper.filesystem;

import akka.dispatch.ExecutionContexts;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Factory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import xyz.cloudkeeper.contracts.RemoteStagingAreaContract;
import xyz.cloudkeeper.contracts.StagingAreaContract;
import xyz.cloudkeeper.contracts.StagingAreaContractProvider;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.InstanceProvisionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.testkit.CallingThreadExecutor;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ITFileStagingArea {
    private final InstanceProvider instanceProvider = new InstanceProviderImpl();
    @Nullable private Path tempDir;
    private final CallingThreadExecutor executor = new CallingThreadExecutor();
    private final ExecutionContext executionContext = ExecutionContexts.fromExecutor(executor);

    @BeforeSuite
    public void setup() throws IOException, JAXBException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
    }

    @AfterSuite
    public void tearDown() throws IOException {
        assert tempDir != null;
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
    }

    @Factory
    public Object[] contractTests() {
        ProviderImpl stagingAreaProvider = new ProviderImpl();
        return new Object[] {
            new StagingAreaContract(stagingAreaProvider),
            new RemoteStagingAreaContract(stagingAreaProvider, instanceProvider)
        };
    }

    private class ProviderImpl implements StagingAreaContractProvider {
        @Override
        public StagingArea getStagingArea(String identifier, RuntimeContext runtimeContext,
                RuntimeAnnotatedExecutionTrace executionTrace) {
            assert tempDir != null;

            Path rootPath;
            try {
                rootPath = Files.createTempDirectory(tempDir, identifier);
            } catch (IOException exception) {
                Assert.fail(
                    String.format("Failed to create root directory for staging area '%s'.", identifier),
                    exception
                );
                // The following statement is only necessary to make the compiler happy.
                return null;
            }
            return new FileStagingArea.Builder(runtimeContext, executionTrace, rootPath, executionContext).build();
        }

        @Override
        public <T> T await(Future<T> future) throws Exception {
            executor.executeAll();
            assert future.isCompleted();
            return future.value().get().get();
        }
    }

    /**
     * This instance provider is a mock implementation that satisfies the requirements described in
     * {@link xyz.cloudkeeper.filesystem}.
     */
    private class InstanceProviderImpl implements InstanceProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getInstance(Class<T> requestedClass) throws InstanceProvisionException {
            if (ExecutionContext.class.equals(requestedClass)) {
                return (T) executionContext;
            } else {
                throw new InstanceProvisionException(String.format(
                    "Instance provider ask for unexpected %s.", requestedClass
                ));
            }
        }
    }
}
