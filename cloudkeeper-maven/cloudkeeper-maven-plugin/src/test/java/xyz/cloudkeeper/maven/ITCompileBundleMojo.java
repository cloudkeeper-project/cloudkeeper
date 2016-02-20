package xyz.cloudkeeper.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Assert;
import xyz.cloudkeeper.examples.modules.BinarySum;
import xyz.cloudkeeper.examples.modules.Decrease;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.NotFoundException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.beans.element.MutableBundle;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.Version;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModuleDeclaration;
import xyz.cloudkeeper.model.util.BuildInformation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for {@link CompileBundleMojo}.
 *
 * <p>Note that system property {@code basedir} should be set to the base directory of this project
 * (cloudkeeper-maven-plugin). This is because {@link #setupAndRunMojo(Path, String)} calls methods
 * {@link #lookupMojo(String, File)}, which in turn calls {@link #getBasedir()}.
 *
 * <p>IntelliJ users may have make sure that there is only one asm version on the classpath (the Maven testing harness
 * transitively depends on 3.3.1, and this is the only version that should be present).
 */
public class ITCompileBundleMojo extends AbstractMojoTestCase {
    private static final long RUNTIME_CONTEXT_DURATION_MS = 1000;

    private Path tempDir;
    private Unmarshaller unmarshaller;
    private ExecutorService executorService;

    private Date startDate;
    private DummyAetherRepository aetherRepository;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        JAXBContext jaxbContext = JAXBContext.newInstance(MutableBundle.class);
        unmarshaller = jaxbContext.createUnmarshaller();
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        executorService = Executors.newFixedThreadPool(1);

        startDate = new Date();
        aetherRepository = new DummyAetherRepository(tempDir);
        aetherRepository.installBundle("decrease", Collections.singletonList(Decrease.class));
        aetherRepository.installBundle("binarysum", Collections.singletonList(BinarySum.class), "decrease");
    }

    @Override
    public void tearDown() throws Exception {
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
        executorService.shutdownNow();
        super.tearDown();
    }

    private CompileBundleMojo setupAndRunMojo(Path buildDirectory, String pomResourceName) throws Exception {
        File pomFile = new File(getClass().getResource(pomResourceName).toURI());
        CompileBundleMojo mojo = (CompileBundleMojo) lookupMojo("compile", pomFile);
        RepositorySystem mojoRepositorySystem = mojo.getRepositorySystem();
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(
            mojoRepositorySystem.newLocalRepositoryManager(session, aetherRepository.getLocalRepository())
        );
        mojo.setRepositorySystemSession(session);
        mojo.setBuildDirectory(buildDirectory);
        mojo.execute();
        return mojo;
    }

    private static Artifact mavenToAetherArtifact(org.apache.maven.artifact.Artifact mavenArtifact) {
        return new DefaultArtifact(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(),
            mavenArtifact.getClassifier(), mavenArtifact.getType(), mavenArtifact.getVersion(), null,
            mavenArtifact.getFile());
    }

    public void testSuccessfulCompile() throws Exception {
        Path buildDirectory = Files.createDirectory(tempDir.resolve("successful-build"));
        CompileBundleMojo mojo = setupAndRunMojo(buildDirectory, "successful.pom.xml");
        Path bundleFile = buildDirectory.resolve(mojo.getBundleFileName());
        MutableBundle bundle = (MutableBundle) unmarshaller.unmarshal(bundleFile.toFile());

        // Verify mojo execution
        Assert.assertTrue(
            startDate.compareTo(bundle.getCreationTime()) <= 0
            && bundle.getCreationTime().compareTo(new Date()) <= 0
        );
        Assert.assertNull(bundle.getBundleIdentifier());
        Assert.assertEquals(bundle.getCloudKeeperVersion(), BuildInformation.PROJECT_VERSION);
        Assert.assertEquals(1, bundle.getPackages().size());
        Assert.assertEquals(1, bundle.getPackages().get(0).getDeclarations().size());
        Assert.assertEquals(SumMinusTwo.class.getSimpleName(),
            bundle.getPackages().get(0).getDeclarations().get(0).getSimpleName().toString());

        // Now install artifacts into temporary Maven repository
        MavenProject project = mojo.getProject();
        Artifact pomArtifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(), null, "pom",
            project.getVersion(), null, new File(getClass().getResource("successful.pom.xml").toURI()));
        Assert.assertEquals(project.getAttachedArtifacts().size(), 1);
        Artifact bundleArtifact = mavenToAetherArtifact(project.getAttachedArtifacts().get(0));
        aetherRepository.installBundleWithJar(pomArtifact, bundleArtifact, Collections.emptyList());

        // Load the bundle with a bundle URI
        MavenRuntimeContextFactory runtimeContextFactory = new MavenRuntimeContextFactory.Builder(
                executorService, aetherRepository.getRepositorySystem(), aetherRepository.getRepositorySystemSession())
            .build();
        URI bundleIdentifier = Bundles.bundleIdentifierFromMaven(
            project.getGroupId(), project.getArtifactId(), Version.valueOf(project.getVersion()));
        try (
            RuntimeContext runtimeContext = runtimeContextFactory
                .newRuntimeContext(Collections.singletonList(bundleIdentifier))
                .get(RUNTIME_CONTEXT_DURATION_MS, TimeUnit.MILLISECONDS)
        ) {
            // Sanity check: Linking needs to succeed
            RuntimeModuleDeclaration binarySumDeclaration = runtimeContext.getRepository().getElement(
                RuntimeModuleDeclaration.class, Name.qualifiedName(BinarySum.class.getName()));
            // The bundle identifier is preserved. Therefore, the artifact-id in the bundle identifier is "binarysum",
            // and not "testing-only"
            Assert.assertTrue(
                binarySumDeclaration.getPackage().getBundleIdentifier().toString().contains("testing-only:binarysum"));
        }
    }

    public void testFailedCompile() throws Exception {
        Path buildDirectory = Files.createDirectory(tempDir.resolve("failed-build"));
        try {
            setupAndRunMojo(buildDirectory, "failure.pom.xml");
            Assert.fail("Expected " + LinkerException.class.getSimpleName());
        } catch (MojoFailureException exception) {
            Assert.assertTrue(exception.getCause() instanceof NotFoundException);
            Assert.assertTrue(exception.getCause().getMessage().contains(BinarySum.class.getName()));
        }
    }
}
