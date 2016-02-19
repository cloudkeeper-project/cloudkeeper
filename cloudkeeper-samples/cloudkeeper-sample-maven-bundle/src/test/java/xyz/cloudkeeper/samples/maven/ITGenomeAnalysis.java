package xyz.cloudkeeper.samples.maven;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.model.api.CloudKeeperEnvironment;
import xyz.cloudkeeper.model.api.WorkflowExecution;
import xyz.cloudkeeper.simple.SingleVMCloudKeeper;
import xyz.cloudkeeper.simple.WorkflowExecutions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ITGenomeAnalysis {
    private SingleVMCloudKeeper cloudKeeper;
    private CloudKeeperEnvironment cloudKeeperEnvironment;

    @BeforeClass
    public void setup() {
        cloudKeeper = new SingleVMCloudKeeper.Builder().build();
        cloudKeeperEnvironment = cloudKeeper.newCloudKeeperEnvironmentBuilder().build();
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        cloudKeeper.shutdown().awaitTermination();
    }

    @Test
    public void test() throws Exception {
        List<String> reads = Arrays.asList("ACCTG", "CTAA", "GATCTTTACGAA", "CGAAT");
        GenomeAnalysisModule module = ModuleFactory.getDefault().create(GenomeAnalysisModule.class)
            .reads().fromValue(reads.stream().collect(Collectors.joining(System.lineSeparator())));
        WorkflowExecution execution = module
            .newPreconfiguredWorkflowExecutionBuilder(cloudKeeperEnvironment)
            .start();
        String report = WorkflowExecutions.getOutputValue(execution, module.report(), 1, TimeUnit.MINUTES);
        Assert.assertTrue(report.contains(
            String.format("read length is %.2f,", reads.stream().collect(Collectors.averagingInt(String::length)))
        ));
    }
}
