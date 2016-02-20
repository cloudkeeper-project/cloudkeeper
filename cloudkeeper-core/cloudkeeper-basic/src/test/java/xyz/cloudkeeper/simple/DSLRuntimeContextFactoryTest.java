package xyz.cloudkeeper.simple;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.Module;
import xyz.cloudkeeper.examples.modules.BinarySum;
import xyz.cloudkeeper.examples.modules.Fibonacci;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DSLRuntimeContextFactoryTest {
    @Nullable private ExecutorService executorService;

    @BeforeClass
    public void setup() {
        executorService = Executors.newFixedThreadPool(1);
    }

    @AfterClass
    public void tearDown() {
        assert executorService != null;
        executorService.shutdown();
    }

    @Test
    public void loadTest() throws Exception {
        assert executorService != null;

        DSLRuntimeContextFactory runtimeContextFactory = new DSLRuntimeContextFactory.Builder(executorService).build();
        URI bundleIdentifier = new URI(Module.URI_SCHEME, Fibonacci.class.getName(), null);
        CompletableFuture<RuntimeContext> future = runtimeContextFactory.newRuntimeContext(
            Collections.singletonList(bundleIdentifier)
        );

        try (RuntimeContext runtimeContext = future.get(5, TimeUnit.SECONDS)) {
            RuntimeRepository repository = runtimeContext.getRepository();

            Assert.assertEquals(repository.getBundles().size(), 1);
            Assert.assertEquals(repository.getBundles().get(0).getBundleIdentifier(), bundleIdentifier);

            RuntimeSimpleModuleDeclaration binarySumDeclaration = repository.getElement(
                RuntimeSimpleModuleDeclaration.class, Name.qualifiedName(BinarySum.class.getName()));
            Assert.assertNotNull(binarySumDeclaration);
            Assert.assertEquals(binarySumDeclaration.getPorts().size(), 3);
        }
    }
}
