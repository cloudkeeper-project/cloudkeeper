package xyz.cloudkeeper.maven;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import xyz.cloudkeeper.contracts.RuntimeContextProviderContract;
import xyz.cloudkeeper.examples.modules.BinarySum;
import xyz.cloudkeeper.examples.modules.Decrease;
import xyz.cloudkeeper.examples.modules.Fibonacci;
import xyz.cloudkeeper.examples.modules.GreaterOrEqual;
import xyz.cloudkeeper.linker.ExecutableProvider;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.simple.DSLExecutableProvider;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Basic contract-test instantiation. A more complete integration test is undertaken in project
 * "cloudkeeper-maven-plugin".
 */
public class ITMavenRuntimeContextProvider {
    private static final long TIMEOUT_MILLIS = 10_000;

    private ExecutorService executorService;
    private Path tempDir;
    private DummyAetherRepository aetherRepository;
    private boolean classLoaderVerified = false;

    public void setup() throws Exception {
        executorService = Executors.newFixedThreadPool(1);
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        aetherRepository = new DummyAetherRepository(tempDir);
        aetherRepository.installBundle("decrease", Collections.singletonList(Decrease.class));
        aetherRepository.installBundle("binarysum", Collections.singletonList(BinarySum.class), "decrease");
        aetherRepository.installBundle("fibonacci", Arrays.asList(GreaterOrEqual.class, Fibonacci.class), "binarysum");
    }

    @AfterSuite
    public void tearDown() throws IOException {
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
        executorService.shutdownNow();
    }

    @Factory
    public Object[] contractTests() throws Exception {
        setup();

        return new Object[] {
            new RuntimeContextProviderContract(
                () -> new MavenRuntimeContextFactory.Builder(
                    executorService,
                    aetherRepository.getRepositorySystem(),
                    aetherRepository.getRepositorySystemSession()
                ).build()
            )
        };
    }

    @Test
    public void provide() throws Exception {
        MavenRuntimeContextFactory runtimeContextFactory = new MavenRuntimeContextFactory.Builder(
                executorService,
                aetherRepository.getRepositorySystem(),
                aetherRepository.getRepositorySystemSession()
            )
            .setExecutableProviderProvider(this::getJavaConnector)
            .build();

        try (
            RuntimeContext runtimeContext
                = runtimeContextFactory.newRuntimeContext(
                    Collections.singletonList(Bundles.bundleIdentifierFromMaven(
                        DummyAetherRepository.GROUP_ID, "fibonacci", DummyAetherRepository.VERSION
                    ))
                )
                .get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        ) {
            Assert.assertEquals(runtimeContext.getRepository().getBundles().size(), 3);
        }

        Assert.assertTrue(classLoaderVerified, "Newly create URLClassLoader was not verified.");
    }

    /**
     * Verifies that the given {@link URLClassLoader} has indeed the correct classpaths.
     */
    private void verifyClassLoader(URLClassLoader classLoader) throws RepositoryException, URISyntaxException {
        AetherConnector aetherConnector = new AetherConnector(
            aetherRepository.getRepositorySystem(), aetherRepository.getRepositorySystemSession(),
            Collections.<RemoteRepository>emptyList()
        );
        List<Artifact> jarArtifacts = aetherConnector.resolveArtifacts(
            (node, parents) -> {
                @Nullable Artifact artifact = node.getArtifact();
                return artifact != null && "jar".equals(node.getArtifact().getExtension());
            },
            Collections.<Artifact>singletonList(
                new DefaultArtifact(DummyAetherRepository.GROUP_ID, "fibonacci", "jar",
                    DummyAetherRepository.VERSION.toString())
            ),
            null
        );
        Set<Path> jarFiles
            = jarArtifacts.stream().map(artifact -> artifact.getFile().toPath()).collect(Collectors.toSet());
        Assert.assertEquals(jarFiles.size(), 3);
        Set<Path> classLoaderFiles = new LinkedHashSet<>();
        for (URL url: classLoader.getURLs()) {
            classLoaderFiles.add(Paths.get(url.toURI()));
        }

        Assert.assertEquals(classLoaderFiles, jarFiles);
    }

    private ExecutableProvider getJavaConnector(ClassLoader classLoader) {
        try {
            verifyClassLoader((URLClassLoader) classLoader);
            classLoaderVerified = true;
        } catch (RepositoryException | URISyntaxException exception) {
            Assert.fail("Failed to collect classpaths because of an unexpected exception.", exception);
        }

        return DSLExecutableProvider.getDefault();
    }
}
