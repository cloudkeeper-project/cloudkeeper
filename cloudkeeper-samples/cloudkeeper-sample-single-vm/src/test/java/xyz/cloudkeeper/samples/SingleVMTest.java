package xyz.cloudkeeper.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;
import xyz.cloudkeeper.model.api.CloudKeeperEnvironment;
import xyz.cloudkeeper.model.api.WorkflowExecution;
import xyz.cloudkeeper.model.util.ByteSequences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Example program that creates simple CloudKeeper environments and then runs modules within these environments.
 */
public final class SingleVMTest {
    private static final Logger LOG = LoggerFactory.getLogger(SingleVMTest.class);

    private SingleVMTest() { }

    private static void runFileManipulationExample(FileBasedCloudKeeperEnvironment cloudKeeperEnvironment)
        throws IOException, InterruptedException, ExecutionException {

        Path file = cloudKeeperEnvironment.getTemporaryDirectory().resolve("HelloWorld.txt");
        Files.copy(SingleVMTest.class.getResourceAsStream("HelloWorld.txt"), file);

        Future<FileManipulationResult> future = new FileManipulationFuture(cloudKeeperEnvironment, file, "-- ");
        FileManipulationResult result = future.get();

        Path prependedFile = cloudKeeperEnvironment.getTemporaryDirectory().resolve("prepended.txt");
        ByteSequences.copy(result.getByteSequence(), prependedFile);

        LOG.info(
            "Result: Path to new file: {}, Number of lines in new file: {}, Size of new file: {}",
            prependedFile, result.getNumberOfLines(), result.getSizeOfPrependedFile()
        );
    }

    private static void runFaultyModule(CloudKeeperEnvironment cloudKeeperEnvironment) {
        DivideModule divideModule = ModuleFactory.getDefault().create(DivideModule.class)
            .numerator().fromValue(4)
            .denominator().fromValue(0);
        WorkflowExecution execution
            = divideModule.newPreconfiguredWorkflowExecutionBuilder(cloudKeeperEnvironment).start();
        try {
            execution.toCompletableFuture().get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException exception) {
            LOG.debug("An expected exception occurred!", exception);
        }
    }

    @Test
    public void testInSingleVM() throws IOException, InterruptedException, ExecutionException {
        try (FileBasedCloudKeeperEnvironment cloudKeeperEnvironment = new FileBasedCloudKeeperEnvironment()) {
            runFileManipulationExample(cloudKeeperEnvironment);
        }

        try (InMemoryCloudKeeperEnvironment cloudKeeperEnvironment = new InMemoryCloudKeeperEnvironment()) {
            runFaultyModule(cloudKeeperEnvironment);
        }
    }

    @SimpleModulePlugin("Divides a number by another number.")
    public abstract static class DivideModule extends SimpleModule<DivideModule> {
        public abstract InPort<Integer> numerator();
        public abstract InPort<Integer> denominator();
        public abstract OutPort<Integer> result();

        @Override
        public void run() {
            result().set(numerator().get() / denominator().get());
        }
    }
}
