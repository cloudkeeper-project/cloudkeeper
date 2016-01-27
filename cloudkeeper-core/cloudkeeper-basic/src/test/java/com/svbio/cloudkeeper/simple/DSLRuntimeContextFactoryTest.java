package com.svbio.cloudkeeper.simple;

import akka.dispatch.ExecutionContexts;
import com.svbio.cloudkeeper.dsl.Module;
import com.svbio.cloudkeeper.examples.modules.BinarySum;
import com.svbio.cloudkeeper.examples.modules.Fibonacci;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DSLRuntimeContextFactoryTest {
    private ExecutorService executorService;
    private DSLRuntimeContextFactory runtimeContextFactory;

    @BeforeClass
    public void setup() {
        executorService = Executors.newFixedThreadPool(1);
        ExecutionContext executionContext = ExecutionContexts.fromExecutorService(executorService);
        runtimeContextFactory = new DSLRuntimeContextFactory.Builder(executionContext)
            .build();
    }

    @AfterClass
    public void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void loadTest() throws Exception {
        URI bundleIdentifier = new URI(Module.URI_SCHEME, Fibonacci.class.getName(), null);
        Future<RuntimeContext> future = runtimeContextFactory.newRuntimeContext(
            Collections.singletonList(bundleIdentifier)
        );

        try (RuntimeContext runtimeContext = Await.result(future, Duration.create(5, TimeUnit.SECONDS))) {
            RuntimeRepository repository = runtimeContext.getRepository();

            Assert.assertEquals(repository.getBundles().size(), 1);
            Assert.assertEquals(repository.getBundles().get(0).getBundleIdentifier(), bundleIdentifier);

            RuntimeSimpleModuleDeclaration binarySumDeclaration
                = repository.getElement(RuntimeSimpleModuleDeclaration.class, Name.qualifiedName(BinarySum.class.getName()));
            Assert.assertNotNull(binarySumDeclaration);
            Assert.assertEquals(binarySumDeclaration.getPorts().size(), 3);
        }
    }
}
