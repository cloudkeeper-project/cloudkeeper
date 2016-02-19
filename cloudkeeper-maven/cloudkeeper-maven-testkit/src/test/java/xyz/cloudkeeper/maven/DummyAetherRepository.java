package xyz.cloudkeeper.maven;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.linker.LinkerOptions;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.beans.element.MutableBundle;
import xyz.cloudkeeper.model.beans.element.MutablePackage;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.Version;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.util.BuildInformation;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class DummyAetherRepository {
    public static final String GROUP_ID = "testing-only";
    public static final Version VERSION = Version.valueOf("1.0.0-SNAPSHOT");
    public static final String TYPE = "ckbundle";
    private static final String MODEL_VERSION = "4.0.0";

    /**
     * Type indicating that {@link org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory} should be used
     * to manage a {@link LocalRepository}.
     */
    private static final String SIMPLE_REPOSITORY_MANAGER = "simple";

    private final Path tempDir;
    private final JAXBContext jaxbContext;
    private final RepositorySystem repositorySystem;
    private final DefaultRepositorySystemSession repositorySystemSession;
    private final LocalRepository localRepository;

    public DummyAetherRepository(Path baseTempDir) throws IOException {
        tempDir = Files.createTempDirectory(baseTempDir, getClass().getSimpleName());
        try {
            jaxbContext = JAXBContext.newInstance(MutableBundle.class);
        } catch (JAXBException exception) {
            throw new IllegalStateException("Failed to create JAXB context.", exception);
        }

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        repositorySystem = locator.getService(RepositorySystem.class);
        repositorySystemSession = MavenRepositorySystemUtils.newSession();
        localRepository
            = new LocalRepository(Files.createTempDirectory(tempDir, "maven-repo").toFile(), SIMPLE_REPOSITORY_MANAGER);
        repositorySystemSession.setLocalRepositoryManager(
            repositorySystem.newLocalRepositoryManager(repositorySystemSession, localRepository)
        );
    }

    public DefaultRepositorySystemSession getRepositorySystemSession() {
        return repositorySystemSession;
    }

    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    public LocalRepository getLocalRepository() {
        return localRepository;
    }

    private static void copy(Class<?> clazz, JarOutputStream jarOutputStream) throws IOException {
        String internalName = clazz.getName().replace('.', '/') + ".class";
        JarEntry jarEntry = new JarEntry(internalName);
        jarOutputStream.putNextEntry(jarEntry);
        try (InputStream inputStream = clazz.getClassLoader().getResourceAsStream(internalName)) {
            byte[] buffer = new byte[2048];
            while (true) {
                int read = inputStream.read(buffer);
                if (read == -1) {
                    break;
                }
                jarOutputStream.write(buffer, 0, read);
            }
        }
        jarOutputStream.closeEntry();
        for (Class<?> declaredClass: clazz.getDeclaredClasses()) {
            copy(declaredClass, jarOutputStream);
        }
    }

    private static void createDummyJar(Path jarFile, List<? extends Class<?>> declarationClasses) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile))) {
            for (Class<?> declarationClass: declarationClasses) {
                copy(declarationClass, jarOutputStream);
            }
        }
    }

    private enum CloudKeeperBundleFilter implements DependencyFilter {
        INSTANCE;

        @Override
        public boolean accept(@Nullable DependencyNode node, @Nullable List<DependencyNode> parents) {
            assert node != null && parents != null : "Violation of precondition.";
            @Nullable Artifact artifact = node.getArtifact();
            return artifact != null && Bundles.ARTIFACT_TYPE.equals(artifact.getExtension());
        }
    }

    private List<Artifact> collectDependencyArtifacts(List<Dependency> dependencies)
            throws RepositoryException {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setDependencies(dependencies);
        DependencyNode node = repositorySystem
            .collectDependencies(repositorySystemSession, collectRequest)
            .getRoot();

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setRoot(node);
        // setFilter() allows null arguments.
        dependencyRequest.setFilter(
            AndDependencyFilter.newInstance(
                new ScopeDependencyFilter(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME), null),
                CloudKeeperBundleFilter.INSTANCE
            )
        );
        repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

        PreorderNodeListGenerator nodeListGenerator = new PreorderNodeListGenerator();
        node.accept(nodeListGenerator);
        return nodeListGenerator.getArtifacts(false);
    }

    private List<MutableBundle> loadDependencyDeclarations(List<String> dependencies)
            throws JAXBException, RepositoryException, XMLStreamException {
        List<Dependency> aetherGraphDependencies = new ArrayList<>(dependencies.size());
        for (String dependency: dependencies) {
            aetherGraphDependencies.add(
                new Dependency(new DefaultArtifact(GROUP_ID, dependency, "ckbundle", VERSION.toString()), "compile")
            );
        }
        List<Artifact> bundleArtifacts = collectDependencyArtifacts(aetherGraphDependencies);

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        List<MutableBundle> bundles = new ArrayList<>(bundleArtifacts.size());
        for (Artifact bundleArtifact: bundleArtifacts) {
            MutableBundle bundle = Bundles.loadBundle(jaxbContext, xmlInputFactory, bundleArtifact);
            bundles.add(bundle);
        }
        return bundles;
    }

    private static org.apache.maven.model.Dependency createMavenDependency(String artifactId, String type) {
        org.apache.maven.model.Dependency mavenDependency = new org.apache.maven.model.Dependency();
        mavenDependency.setGroupId(GROUP_ID);
        mavenDependency.setArtifactId(artifactId);
        mavenDependency.setVersion(VERSION.toString());
        mavenDependency.setType(type);
        return mavenDependency;
    }

    private static void generatePOM(Path pomFile, String groupId, String artifactId, List<String> dependencies)
            throws IOException {
        Model pom = new Model();
        pom.setModelVersion(MODEL_VERSION);

        pom.setGroupId(groupId);
        pom.setArtifactId(artifactId);
        pom.setVersion(VERSION.toString());
        pom.setPackaging("jar");
        pom.setName(artifactId + ": fabricated POM for testing");

        for (String dependency: dependencies) {
            pom.addDependency(createMavenDependency(dependency, TYPE));
            pom.addDependency(createMavenDependency(dependency, "jar"));
        }

        MavenXpp3Writer writer = new MavenXpp3Writer();
        try (OutputStream outputStream = Files.newOutputStream(pomFile)) {
            writer.write(outputStream, pom);
        }
    }

    private static MutablePackage getOrCreatePackage(Package javaPackage, Map<Name, MutablePackage> packageMap) {
        Name qualifiedName = Name.qualifiedName(javaPackage.getName());
        @Nullable MutablePackage thePackage = packageMap.get(qualifiedName);
        if (thePackage == null) {
            thePackage = MutablePackage.fromPackage(javaPackage);
            packageMap.put(qualifiedName, thePackage);
        }
        return thePackage;
    }

    /**
     * Installs an Aether artifact representing a CloudKeeper bundle with the given declarations.
     *
     * <p>The group id of artifacts installed with this method is always {@link #GROUP_ID}. Likewise, the version is
     * always {@link #VERSION}.
     *
     * @param artifactId artifact id for new Aether artifact
     * @param declarationClasses DSL classes of plugin declarations that the new bundle will consist of
     * @param dependencies Artifact ids of CloudKeeper bundle dependencies (previously installed with this
     *     method). Group id and version are always assumed to be {@link #GROUP_ID} and {@link #VERSION}, respectively.
     * @throws IOException if an I/O exception occurs
     * @throws LinkerException if the new CloudKeeper bundle cannot be linked with the specified dependencies
     * @throws RepositoryException if an Aether operation (such as retrieving dependencies or installing the new
     *     artifact) fails
     */
    public void installBundle(String artifactId, List<? extends Class<?>> declarationClasses,
            String... dependencies) throws IOException, LinkerException, RepositoryException {
        Objects.requireNonNull(declarationClasses);
        Objects.requireNonNull(artifactId);
        Objects.requireNonNull(dependencies);

        List<String> dependenciesList = Arrays.asList(dependencies);
        Map<Name, MutablePackage> packageMap = new LinkedHashMap<>();
        ModuleFactory moduleFactory = ModuleFactory.getDefault();
        for (Class<?> declarationClass: declarationClasses) {
            getOrCreatePackage(declarationClass.getPackage(), packageMap)
                .getDeclarations()
                .add(moduleFactory.loadDeclaration(declarationClass));
        }
        MutableBundle unverifiedBundle = new MutableBundle()
            .setBundleIdentifier(
                Bundles.bundleIdentifierFromMaven(GROUP_ID, artifactId, VERSION))
            .setCreationTime(new Date())
            .setCloudKeeperVersion(BuildInformation.PROJECT_VERSION)
            .setPackages(new ArrayList<>(packageMap.values()));
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            // Verify that bundle can be linked
            List<MutableBundle> dependencyBundles = loadDependencyDeclarations(dependenciesList);
            List<MutableBundle> bundles = new ArrayList<>(1 + dependencyBundles.size());
            bundles.add(unverifiedBundle);
            bundles.addAll(dependencyBundles);
            LinkerOptions linkerOptions = new LinkerOptions.Builder()
                .setMarshalValues(true)
                .build();
            RuntimeRepository repository = Linker.createRepository(bundles, linkerOptions);
            MutableBundle verifiedBundle = MutableBundle.copyOf(repository.getBundles().get(0));

            Path bundleFile = tempDir.resolve(artifactId + '-' + VERSION + ".ckbundle");
            marshaller.marshal(verifiedBundle, bundleFile.toFile());

            Path pomFile = tempDir.resolve(artifactId + '-' + VERSION + ".pom");
            generatePOM(pomFile, GROUP_ID, artifactId, dependenciesList);
            Artifact pomArtifact = new DefaultArtifact(
                GROUP_ID, artifactId, "", "pom", VERSION.toString(), null, pomFile.toFile()
            );
            Artifact bundleArtifact = new SubArtifact(pomArtifact, "", "ckbundle", bundleFile.toFile());

            installBundleWithJar(pomArtifact, bundleArtifact, declarationClasses);
        } catch (JAXBException | XMLStreamException exception) {
            throw new IllegalStateException("Unexpected exception!", exception);
        }
    }

    public void installBundleWithJar(Artifact pomArtifact, Artifact bundleArtifact,
            List<? extends Class<?>> containedClasses) throws IOException, RepositoryException {
        Path jarFile = tempDir.resolve(pomArtifact.getArtifactId() + '-' + pomArtifact.getVersion() + ".jar");
        createDummyJar(jarFile, containedClasses);

        Artifact jarArtifact = new SubArtifact(pomArtifact, "", "jar", jarFile.toFile());
        InstallRequest installRequest = new InstallRequest()
            .addArtifact(pomArtifact)
            .addArtifact(bundleArtifact)
            .addArtifact(jarArtifact);
        repositorySystem.install(repositorySystemSession, installRequest);
    }
}
