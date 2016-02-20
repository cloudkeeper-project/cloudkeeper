package xyz.cloudkeeper.contracts;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.examples.modules.Fibonacci;
import xyz.cloudkeeper.model.api.ConnectorException;
import xyz.cloudkeeper.model.api.executor.ExtendedModuleConnector;
import xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.immutable.element.Index;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Contract test for module-connector provider.
 *
 * <p>This contract test verifies all fundamental functionality of a
 * {@link xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider} implementation.
 */
public final class ModuleConnectorProviderContract {
    private final ModuleConnectorProvider moduleConnectorProvider;
    private final StagingAreaContractProvider stagingAreaContractProvider;
    private final long awaitDurationMillis;
    private StagingAreaContractHelper helper;
    private StagingArea stagingArea;
    private ExtendedModuleConnector moduleConnector;

    public ModuleConnectorProviderContract(ModuleConnectorProvider moduleConnectorProvider,
            StagingAreaContractProvider stagingAreaContractProvider, long awaitDurationMillis) {
        this.moduleConnectorProvider = moduleConnectorProvider;
        this.stagingAreaContractProvider = stagingAreaContractProvider;
        this.awaitDurationMillis = awaitDurationMillis;
    }

    @BeforeClass
    public void setup() {
        helper = new StagingAreaContractHelper(stagingAreaContractProvider, Fibonacci.class);
    }

    private <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(awaitDurationMillis, TimeUnit.MILLISECONDS);
    }

    @Test
    public void provisionTest() throws Exception {
        stagingArea = helper.createStagingArea("submitTest")
            .resolveDescendant(
                ExecutionTrace.empty()
                    .resolveContent().resolveModule(SimpleName.identifier("loop"))
                    .resolveContent().resolveIteration(Index.index(0))
                    .resolveContent().resolveModule(SimpleName.identifier("sum"))
            );
        await(stagingArea.putObject(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("num1")), 4));
        await(stagingArea.putObject(ExecutionTrace.empty().resolveInPort(SimpleName.identifier("num2")), 6));

        moduleConnector = await(moduleConnectorProvider.provideModuleConnector(stagingArea));
    }

    @Test(dependsOnMethods = "provisionTest")
    public void getExecutionTraceTest() {
        Assert.assertSame(stagingArea.getAnnotatedExecutionTrace(), moduleConnector.getExecutionTrace());
    }

    @Test(dependsOnMethods = "provisionTest")
    public void getWorkingDirectoryTest() throws IOException {
        Path workingDirectory = moduleConnector.getWorkingDirectory();
        Files.createFile(workingDirectory.resolve("junk"));
        Files.delete(workingDirectory.resolve("junk"));
    }

    @Test(dependsOnMethods = "provisionTest")
    public void getInputTestInvalid() {
        try {
            moduleConnector.getInput(null);
            Assert.fail("Expected exception");
        } catch (NullPointerException ignored) { }

        try {
            moduleConnector.getInput(SimpleName.identifier("sum"));
            Assert.fail("Expected exception");
        } catch (ConnectorException exception) {
            Assert.assertTrue(exception.getMessage().contains("sum"));
        }

        try {
            moduleConnector.getInput(SimpleName.identifier("foo"));
            Assert.fail("Expected exception");
        } catch (ConnectorException exception) {
            Assert.assertTrue(exception.getMessage().contains("foo"));
        }
    }

    @Test(dependsOnMethods = "provisionTest")
    public void getInputTest() {
        Assert.assertEquals(moduleConnector.getInput(SimpleName.identifier("num1")), 4);
        Assert.assertEquals(moduleConnector.getInput(SimpleName.identifier("num2")), 6);
    }

    @Test(dependsOnMethods = "provisionTest")
    public void setOutputTestInvalid() {
        try {
            moduleConnector.setOutput(SimpleName.identifier("sum"), null);
            Assert.fail("Expected exception");
        } catch (NullPointerException ignored) { }

        try {
            moduleConnector.setOutput(null, 10);
            Assert.fail("Expected exception");
        } catch (NullPointerException ignored) { }

        try {
            moduleConnector.setOutput(SimpleName.identifier("num1"), 10);
            Assert.fail("Expected exception");
        } catch (ConnectorException exception) {
            Assert.assertTrue(exception.getMessage().contains("num1"));
        }

        try {
            moduleConnector.setOutput(SimpleName.identifier("foo"), 10);
            Assert.fail("Expected exception");
        } catch (ConnectorException exception) {
            Assert.assertTrue(exception.getMessage().contains("foo"));
        }
    }

    @Test(dependsOnMethods = "provisionTest")
    public void setOutputTest() {
        moduleConnector.setOutput(SimpleName.identifier("sum"), 10);
    }

    @Test(dependsOnMethods = "setOutputTest")
    public void commitTest() throws Exception {
        ExecutionTrace sumTrace = ExecutionTrace.empty().resolveOutPort(SimpleName.identifier("sum"));
        Assert.assertFalse(await(stagingArea.exists(sumTrace)));
        await(moduleConnector.commit());
        Assert.assertEquals(await(stagingArea.getObject(sumTrace)), 10);
    }

    @Test(dependsOnMethods = "commitTest")
    public void releaseTest() {
        moduleConnector.close();
    }
}
