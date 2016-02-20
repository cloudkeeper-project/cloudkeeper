package xyz.cloudkeeper.maven;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import xyz.cloudkeeper.linker.ClassProvider;
import xyz.cloudkeeper.linker.ExecutableProvider;
import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.linker.LinkerOptions;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeContextFactory;
import xyz.cloudkeeper.model.api.RuntimeStateProvisionException;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.beans.element.MutableBundle;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.util.BuildInformation;
import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maven repository factory.
 *
 * <p>This repository factory takes {@link URI}s of form {@code <scheme>:<group-id>:<artifact-id>:<type>:<version>},
 * where {@code <scheme>} is {@link Bundles#URI_SCHEME}, {@code <artifact-id>} is {@link Bundles#ARTIFACT_TYPE}, and
 * {@code <group-id>}, {@code <artifact-id>}, {@code <version>} are Maven group-artifact-version coordinates. Note that
 * {@link URI#getSchemeSpecificPart()} always contains coordinates as they are expected by the
 * {@link DefaultArtifact#DefaultArtifact(String)} constructor.
 *
 * <p>All bundle artifacts specified by the URIs, and the artifacts of all transitive dependencies are resolved. Each
 * CloudKeeper bundle artifact is an XML file that contains a serialized {@link MutableBundle}. This runtime-context
 * factory optionally resolves all JAR files and their transitive dependencies corresponding to the bundle artifacts.
 * These JAR files are used to create a {@link URLClassLoader}, which is used as the new repository's class loader.
 *
 * <p>Memory-consistency guarantees: Instances of this class contain a {@link RepositorySystem} instance from the
 * Eclipse Aether project. Unfortunately, this class is mutable. This class does guarantee that all invocations of
 * {@link RepositorySystem} methods <em>happen-after</em> the respective call to {@link #newRuntimeContext(List)}.
 */
public abstract class MavenRuntimeContextFactory implements RuntimeContextFactory {
    private static final String JAR_TYPE = "jar";

    /**
     * The JAXB context is supposed to be thread-safe.
     *
     * @see <a href="https://jaxb.java.net/guide/Performance_and_thread_safety.html">https://jaxb.java.net/guide/Performance_and_thread_safety.html</a>
     */
    private final JAXBContext jaxbContext;
    private final Executor executor;
    private final AetherConnector aetherConnector;
    private final Function<ClassLoader, ClassProvider> classProviderProvider;
    private final Function<ClassLoader, ExecutableProvider> executableProviderProvider;

    /**
     * This class is used to create Maven bundle loaders.
     */
    public static final class Builder {
        private final Executor executor;
        private final RepositorySystem repositorySystem;
        private final RepositorySystemSession repositorySystemSession;
        private ImmutableList<RemoteRepository> remoteRepositories = ImmutableList.of();
        @Nullable private ClassLoader classLoader = null;
        private Function<ClassLoader, ClassProvider> classProviderProvider = actualClassLoader
            -> (name -> Optional.of(Class.forName(name.getBinaryName().toString(), true, actualClassLoader)));
        private Function<ClassLoader, ExecutableProvider> executableProviderProvider
            = actualClassLoader -> (name -> Optional.empty());
        @Nullable private JAXBContext jaxbContext;

        /**
         * Constructs a new builder.
         *
         * @param executor executor for running asynchronous tasks during the loading process
         * @param repositorySystem Eclipse Aether repository system. This instance will be used, possibly concurrently,
         *     for all bundle-loading tasks.
         * @param repositorySystemSession Eclipse Aether session that provides settings and components that control the
         *     repository system ({@link RepositorySystemSession} instances are supposed to be immutable).
         */
        public Builder(Executor executor, RepositorySystem repositorySystem,
                RepositorySystemSession repositorySystemSession) {
            this.executor = Objects.requireNonNull(executor);
            this.repositorySystem = Objects.requireNonNull(repositorySystem);
            this.repositorySystemSession = Objects.requireNonNull(repositorySystemSession);
        }

        /**
         * Sets the JAXB context of this builder, which must be able to unmarshal {@link MutableBundle} and all
         * transitively referenced classes.
         *
         * <p>By default, if this method is not called, a new JAXB context will be created as
         * {@code JAXBContext.newInstance(MutableBundle.class)}.
         *
         * @param jaxbContext JAXB context
         * @return this builder
         */
        public Builder setJaxbContext(JAXBContext jaxbContext) {
            this.jaxbContext = Objects.requireNonNull(jaxbContext);
            return this;
        }

        /**
         * Sets the remote repositories of this builder. These will be used for resolving Maven artifacts.
         *
         * <p>By default, if this method is not called, no remote repositories will used.
         *
         * @param remoteRepositories remote repositories
         * @return this builder
         */
        public Builder setRemoteRepositories(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = ImmutableList.copyOf(Objects.requireNonNull(remoteRepositories));
            return this;
        }

        /**
         * Sets the class loader to use for linking and for new {@link RuntimeContext} instances.
         *
         * <p>If set to {@code null}, invocations of {@link #newRuntimeContext(List)} will construct a new
         * {@link URLClassLoader} as follows: Each Maven bundle identifier passed to {@link #newRuntimeContext(List)} is
         * converted to Maven coordinates of type jar, by simply replacing type {@code ckbundle} with {@code jar}. All
         * transitive dependencies are then resolved, and a classpath is built from all jar artifacts. The
         * {@link URLClassLoader} is created with this classpath, and it is closed by {@link RuntimeContext#close()}.
         *
         * <p>By default, the class loader is {@code null}, and the previous paragraph applies.
         *
         * @param classLoader class loader to use for linking and for new {@link RuntimeContext} instances, or
         *     {@code null} in order to construct a new {@link URLClassLoader} for each new {@link RuntimeContext}
         * @return this builder
         */
        public Builder setClassLoader(@Nullable ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Sets the function that will be used to map a new runtime context's actual class loader to a
         * {@link ClassProvider}.
         *
         * <p>The given function will be invoked as a callback from within
         * {@link MavenRuntimeContextFactory#newRuntimeContext(List)}.
         *
         * <p>By default, a {@link ClassProvider} provider will be used that, given {@code classLoader}, returns an
         * equivalent of {@code name -> Class.forName(name.getBinaryName().toString(), true, classLoader)}.
         *
         * @param classProviderProvider Function that returns a non-null {@link ClassProvider}, given the class loader
         *     specified with {@link #setClassLoader(ClassLoader)} or (if the specified class loader was {@code null})
         *     given the {@link URLClassLoader} specifically created for the new runtime context. The given function can
         *     be seen as the result of currying a function with arguments of types {@link ClassLoader} and
         *     {@link Name}.
         * @return this builder
         */
        public Builder setClassProviderProvider(Function<ClassLoader, ClassProvider> classProviderProvider) {
            Objects.requireNonNull(classProviderProvider);
            this.classProviderProvider = classProviderProvider;
            return this;
        }

        /**
         * Sets the function that will be used to map a new runtime context's actual class loader to an
         * {@link ExecutableProvider}.
         *
         * <p>The given function will be invoked as a callback from within
         * {@link MavenRuntimeContextFactory#newRuntimeContext(List)}.
         *
         * <p>By default, an {@link ExecutableProvider} provider will be used that, given {@code classLoader}, returns
         * an equivalent of {@code name -> Optional.empty()}.
         *
         * @param executableProviderProvider Function that returns a non-null {@link ExecutableProvider}, given the
         *     class loader specified with {@link #setClassLoader(ClassLoader)} or (if the specified class loader was
         *     {@code null}) given the {@link URLClassLoader} specifically created for the new runtime context. The
         *     given function can be seen as the result of currying a function with arguments of types
         *     {@link ClassLoader} and {@link Name}.
         * @return this builder
         */
        public Builder setExecutableProviderProvider(
                Function<ClassLoader, ExecutableProvider> executableProviderProvider) {
            Objects.requireNonNull(executableProviderProvider);
            this.executableProviderProvider = executableProviderProvider;
            return this;
        }

        /**
         * Returns a newly constructed Maven bundle loader, using the current properties of this builder.
         */
        public MavenRuntimeContextFactory build() {
            boolean createClassLoader = classLoader == null;
            return createClassLoader
                ? new DynamicClassLoaderRuntimeContextFactory(this)
                : new StaticClassLoaderRuntimeContextFactory(this);
        }
    }

    private MavenRuntimeContextFactory(Builder builder) {
        try {
            jaxbContext = builder.jaxbContext != null
                ? builder.jaxbContext
                : JAXBContext.newInstance(MutableBundle.class);
        } catch (JAXBException exception) {
            throw new IllegalStateException(String.format(
                "Failed to build %s, because %s could not be instantiated.",
                MavenRuntimeContextFactory.class.getSimpleName(), JAXBContext.class.getSimpleName()
            ), exception);
        }

        executor = builder.executor;
        aetherConnector = new AetherConnector(
            builder.repositorySystem, builder.repositorySystemSession, builder.remoteRepositories);
        classProviderProvider = builder.classProviderProvider;
        executableProviderProvider = builder.executableProviderProvider;
    }

    abstract boolean shouldIncludeDependencyNode(DependencyNode node, List<DependencyNode> parents);

    abstract Stream<Artifact> artifactStreamFromURI(URI bundleIdentifier);

    private static final class URLClassLoaderHolder implements Closeable {
        private final URLClassLoader classLoader;
        private boolean keepOpen = false;

        private URLClassLoaderHolder(URLClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        private URLClassLoader get() {
            return classLoader;
        }

        private URLClassLoader retain() {
            keepOpen = true;
            return classLoader;
        }

        @Override
        public void close() throws IOException {
            if (!keepOpen) {
                classLoader.close();
            }
        }
    }

    final LinkerOptions linkerOptions(ClassLoader classLoader) {
        return new LinkerOptions.Builder()
            .setClassProvider(classProviderProvider.apply(classLoader))
            .setExecutableProvider(executableProviderProvider.apply(classLoader))
            .build();
    }

    abstract RuntimeContext runtimeContext(List<Artifact> bundleArtifacts, List<MutableBundle> bundles)
        throws IOException, LinkerException;

    /**
     * {@inheritDoc}
     *
     * <p>The bundle corresponding to the first bundle identifier will be used as the root dependency when creating
     * the {@link org.eclipse.aether.collection.CollectRequest} that will be passed to the Eclipse Aether library. That
     * is, the first bundle will (among other things) determine the dependency management performed by Aether
     * ({@code <dependencyManagement>} sections in a Maven POM file).
     */
    @Override
    public CompletableFuture<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers) {
        Objects.requireNonNull(bundleIdentifiers);
        final ImmutableList<URI> localBundleIdentifiers = ImmutableList.copyOf(bundleIdentifiers);

        CompletionStage<RuntimeContext> completionStage = Futures.supplyAsync(() -> {
            List<Artifact> unresolvedArtifacts = localBundleIdentifiers
                .stream()
                .flatMap(this::artifactStreamFromURI)
                .collect(Collectors.toList());

            List<Artifact> bundleArtifacts = aetherConnector.resolveArtifacts(
                AndDependencyFilter.newInstance(
                    new ScopeDependencyFilter(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME), null),
                    this::shouldIncludeDependencyNode
                ),
                unresolvedArtifacts,
                JavaScopes.RUNTIME
            );

            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            List<MutableBundle> bundles = new ArrayList<>(bundleArtifacts.size());
            for (Artifact bundleArtifact : bundleArtifacts) {
                if (Bundles.ARTIFACT_TYPE.equals(bundleArtifact.getExtension())) {
                    bundles.add(Bundles.loadBundle(jaxbContext, xmlInputFactory, bundleArtifact));
                }
            }

            return runtimeContext(bundleArtifacts, bundles);
        }, executor);
        return Futures.translateException(
            completionStage,
            throwable -> new RuntimeStateProvisionException(String.format(
                "Failed to provide runtime state for bundles %s.", bundleIdentifiers
            ), Futures.unwrapCompletionException(throwable))
        );
    }

    private abstract static class AbstractRuntimeContext implements RuntimeContext {
        private final RuntimeRepository repository;
        private final LinkerOptions linkerOptions;

        private AbstractRuntimeContext(RuntimeRepository repository, LinkerOptions linkerOptions) {
            this.repository = repository;
            this.linkerOptions = linkerOptions;
        }

        @Override
        public final RuntimeRepository getRepository() {
            return repository;
        }
        @Override
        public final RuntimeAnnotatedExecutionTrace newAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace,
                BareModule bareModule, List<? extends BareOverride> overrides) throws LinkerException {
            return Linker.createAnnotatedExecutionTrace(absoluteTrace, bareModule, overrides, repository,
                linkerOptions);
        }
    }

    private static final class ExecutableRuntimeContext extends AbstractRuntimeContext {
        private final URLClassLoader classLoader;

        private ExecutableRuntimeContext(RuntimeRepository repository, LinkerOptions linkerOptions,
                URLClassLoader classLoader) {
            super(repository, linkerOptions);
            this.classLoader = classLoader;
        }

        @Override
        public void close() throws IOException {
            classLoader.close();
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }
    }

    private static final class DynamicClassLoaderRuntimeContextFactory extends MavenRuntimeContextFactory {
        private DynamicClassLoaderRuntimeContextFactory(Builder builder) {
            super(builder);
        }

        @Override
        Stream<Artifact> artifactStreamFromURI(URI bundleIdentifier) {
            Artifact unresolvedArtifact = Bundles.unresolvedArtifactFromURI(bundleIdentifier);
            return Stream.of(
                unresolvedArtifact,
                new DefaultArtifact(unresolvedArtifact.getGroupId(),
                    unresolvedArtifact.getArtifactId(), null, JAR_TYPE, unresolvedArtifact.getVersion(),
                    unresolvedArtifact.getProperties(), (File) null)
            );
        }

        @Override
        boolean shouldIncludeDependencyNode(DependencyNode node, List<DependencyNode> parents) {
            @Nullable Artifact artifact = node.getArtifact();
            return artifact != null
                && (Bundles.ARTIFACT_TYPE.equals(artifact.getExtension()) || JAR_TYPE.equals(artifact.getExtension()))
                && !artifact.getGroupId().equals(BuildInformation.PROJECT_GROUP_ID);
        }

        private static URLClassLoaderHolder classLoader(List<Artifact> bundleArtifacts) throws MalformedURLException {
            URL[] urls = new URL[bundleArtifacts.size()];
            int i = 0;
            for (Artifact artifact: bundleArtifacts) {
                if (JAR_TYPE.equals(artifact.getExtension())) {
                    urls[i] = artifact.getFile().toURI().toURL();
                    ++i;
                }
            }

            // Array urls may be too large if bundleArtifacts contained non-jar artifacts
            return new URLClassLoaderHolder(URLClassLoader.newInstance(Arrays.copyOf(urls, i)));
        }

        @Override
        ExecutableRuntimeContext runtimeContext(List<Artifact> bundleArtifacts, List<MutableBundle> bundles)
                throws IOException, LinkerException {
            try (URLClassLoaderHolder holder = classLoader(bundleArtifacts)) {
                URLClassLoader classLoader = holder.get();
                LinkerOptions linkerOptions = linkerOptions(classLoader);
                RuntimeRepository repository = Linker.createRepository(bundles, linkerOptions);
                ExecutableRuntimeContext runtimeContext
                    = new ExecutableRuntimeContext(repository, linkerOptions, classLoader);

                // No exception occurred, so retain class loader.
                holder.retain();
                return runtimeContext;
            }
        }
    }

    private static final class NonExecutableRuntimeContext extends AbstractRuntimeContext {
        private NonExecutableRuntimeContext(RuntimeRepository repository, LinkerOptions linkerOptions) {
            super(repository, linkerOptions);
        }

        @Override
        public void close() { }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }

    private static class StaticClassLoaderRuntimeContextFactory extends MavenRuntimeContextFactory {
        private final ClassLoader classLoader;

        StaticClassLoaderRuntimeContextFactory(Builder builder) {
            super(builder);
            assert builder.classLoader != null;
            classLoader = builder.classLoader;
        }

        @Override
        Stream<Artifact> artifactStreamFromURI(URI bundleIdentifier) {
            return Stream.of(Bundles.unresolvedArtifactFromURI(bundleIdentifier));
        }

        @Override
        boolean shouldIncludeDependencyNode(DependencyNode node, List<DependencyNode> parents) {
            @Nullable Artifact artifact = node.getArtifact();
            return artifact != null && Bundles.ARTIFACT_TYPE.equals(artifact.getExtension());
        }

        @Override
        NonExecutableRuntimeContext runtimeContext(List<Artifact> bundleArtifacts, List<MutableBundle> bundles)
                throws LinkerException {
            LinkerOptions linkerOptions = linkerOptions(classLoader);
            RuntimeRepository repository = Linker.createRepository(bundles, linkerOptions);
            return new NonExecutableRuntimeContext(repository, linkerOptions);
        }
    }
}
