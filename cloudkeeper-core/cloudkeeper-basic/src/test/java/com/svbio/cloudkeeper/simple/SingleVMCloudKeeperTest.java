package com.svbio.cloudkeeper.simple;

import akka.actor.ActorSystem;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.examples.modules.BinarySum;
import com.svbio.cloudkeeper.examples.modules.Fibonacci;
import com.svbio.cloudkeeper.examples.modules.PascalTriangle;
import com.svbio.cloudkeeper.examples.repositories.FibonacciRepository;
import com.svbio.cloudkeeper.examples.repositories.SimpleRepository;
import com.svbio.cloudkeeper.examples.repositories.TestKitRuntimeContextFactory;
import com.svbio.cloudkeeper.model.api.CloudKeeperEnvironment;
import com.svbio.cloudkeeper.model.api.WorkflowExecution;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SingleVMCloudKeeperTest {
    private static final long WAIT_SECONDS = 3600;
    private Path workspaceBasePath;
    private ActorSystem actorSystem;
    private SingleVMCloudKeeper cloudKeeper;

    @BeforeClass
    public void setup() throws IOException {
        workspaceBasePath = Files.createTempDirectory(getClass().getSimpleName());
        actorSystem = ActorSystem.create(getClass().getSimpleName());

        InstanceProvider instanceProvider = new SimpleInstanceProvider.Builder(actorSystem.dispatcher())
            .setRuntimeContextFactory(new TestKitRuntimeContextFactory())
            .build();

        cloudKeeper = new SingleVMCloudKeeper.Builder()
            .setActorSystem(actorSystem)
            .setWorkspaceBasePath(workspaceBasePath)
            .setInstanceProvider(instanceProvider)
            .build();
    }

    @AfterClass
    public void tearDown() throws Exception {
        cloudKeeper.shutdown().awaitTermination();
        Await.result(actorSystem.terminate(), Duration.Inf());
        Files.walkFileTree(workspaceBasePath, RecursiveDeleteVisitor.getInstance());
    }

    @Test
    public void testSimpleModule() throws Exception {
        SingleVMCloudKeeper simpleCloudKeeper = new SingleVMCloudKeeper.Builder().build();
        CloudKeeperEnvironment environment = simpleCloudKeeper.newCloudKeeperEnvironmentBuilder()
            .setCleaningRequested(false)
            .build();

        BinarySum binarySumModule = ModuleFactory.getDefault().create(BinarySum.class)
            .num1().fromValue(4)
            .num2().fromValue(6);

        WorkflowExecution workflowExecution = binarySumModule
            .newPreconfiguredWorkflowExecutionBuilder(environment)
            .start();
        int result = WorkflowExecutions.getOutputValue(
            workflowExecution, binarySumModule.sum(), WAIT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(result, 10);

        workflowExecution.toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
        simpleCloudKeeper.shutdown();
    }

    @Test
    public void testFibonacci() throws Exception {
        CloudKeeperEnvironment cloudKeeperEnvironment = cloudKeeper.newCloudKeeperEnvironmentBuilder()
            .setCleaningRequested(true)
            .build();
        Fibonacci fibonacciModule = ModuleFactory.getDefault().create(Fibonacci.class)
            .n().fromValue(5);
        WorkflowExecution workflowExecution = fibonacciModule
            .newPreconfiguredWorkflowExecutionBuilder(cloudKeeperEnvironment)
            .setBundleIdentifiers(Collections.singletonList(SimpleRepository.BUNDLE_ID))
            .start();
        int result = WorkflowExecutions.getOutputValue(
            workflowExecution, fibonacciModule.result(), WAIT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(result, 5);

        workflowExecution.toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testProxyFibonacci() throws Exception {
        CloudKeeperEnvironment cloudKeeperEnvironment = cloudKeeper.newCloudKeeperEnvironmentBuilder()
            .setCleaningRequested(true)
            .build();
        WorkflowExecution workflowExecution = cloudKeeperEnvironment
            .newWorkflowExecutionBuilder(new MutableProxyModule().setDeclaration(Fibonacci.class.getName()))
            .setBundleIdentifiers(Arrays.asList(SimpleRepository.BUNDLE_ID, FibonacciRepository.BUNDLE_ID))
            .setInputs(Collections.singletonMap(SimpleName.identifier("n"), (Object) 5))
            .start();
        int result = (Integer) workflowExecution.getOutput("result").get(WAIT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(result, 5);

        workflowExecution.toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testCompositeModule() throws Exception {
        CloudKeeperEnvironment cloudKeeperEnvironment = cloudKeeper.newCloudKeeperEnvironmentBuilder()
            .setCleaningRequested(true)
            .build();
        int n = 5;
        BareModule pascalTriangleModule = PascalTriangle.createCompositeModule(n);
        WorkflowExecution workflowExecution = cloudKeeperEnvironment.newWorkflowExecutionBuilder(pascalTriangleModule)
            .setBundleIdentifiers(Collections.singletonList(SimpleRepository.BUNDLE_ID))
            .start();

        for (int k = 0; k <= n; ++k) {
            Assert.assertEquals(
                (int) workflowExecution.getOutput("coef_" + k).get(WAIT_SECONDS, TimeUnit.SECONDS),
                binomial(n, k)
            );
        }
        workflowExecution.toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    private static int binomial(int n, int k) {
        // Crappy algorithm to compute binomial coefficients. OK for our purpose, where inputs are small.
        long result = 1;
        for (int i = n - k + 1; i <= n; ++i) {
            result *= i;
        }
        for (int i = 1; i <= k; ++i) {
            result /= i;
        }
        return (int) result;
    }
}
