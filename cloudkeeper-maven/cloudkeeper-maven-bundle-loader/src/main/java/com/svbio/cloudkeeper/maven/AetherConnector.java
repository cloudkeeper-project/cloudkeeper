package com.svbio.cloudkeeper.maven;

import com.svbio.cloudkeeper.model.util.ImmutableList;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Light-weight wrapper around Eclipse Aether that provides simple high-level methods for standard Aether tasks.
 *
 * <p>This class is not meant to be extended or used outside of the CloudKeeper Aether/Maven component. See the remarks
 * at {@link com.svbio.cloudkeeper.maven}.
 */
final class AetherConnector {
    /**
     * A repository system is not guaranteed to be immutable.
     *
     * @see <a href="https://github.com/eclipse/aether-core/blob/aether-0.9.0.M2/aether-impl/src/main/java/org/eclipse/aether/internal/impl/DefaultRepositorySystem.java">DefaultRepositorySystem.java</a>
     */
    private final RepositorySystem repositorySystem;

    /**
     * Settings and components that control the repository system. {@link RepositorySystemSession} objects are supposed
     * to be immutable ("session object itself is supposed to be immutable").
     */
    private final RepositorySystemSession repositorySystemSession;
    private final ImmutableList<RemoteRepository> remoteRepositories;

    /**
     * Constructor.
     *
     * @param repositorySystem Eclipse Aether repository system. This instance will be used, possibly concurrently,
     *     for all module-execution tasks.
     * @param repositorySystemSession Eclipse Aether session that provides settings and components that control the
     *     repository system ({@link RepositorySystemSession} instances are supposed to be immutable).
     * @param remoteRepositories remote Eclipse Aether repositories that will be queried
     * @throws NullPointerException if an argument is {@code null}
     */
    AetherConnector(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession,
            List<RemoteRepository> remoteRepositories) {
        this.repositorySystem = Objects.requireNonNull(repositorySystem);
        this.repositorySystemSession = Objects.requireNonNull(repositorySystemSession);
        this.remoteRepositories = ImmutableList.copyOf(Objects.requireNonNull(remoteRepositories));
    }

    /**
     * Returns a filtered list of artifacts that the given (unresolved) artifact has as dependencies.
     *
     * <p>The first artifact is converted into the root dependency that is passed to
     * {@link CollectRequest#CollectRequest(Dependency, List, List)}. The root dependency is used, among other things,
     * to determine the managed dependencies. In other words, {@code unresolvedArtifacts} should not be treated as set,
     * the order (and in particular, which element comes first) is significant.
     *
     * @param dependencyFilter filter for nodes in the dependency graph; may be null
     * @param unresolvedArtifacts the artifacts that should be resolved, including their version-managed and merged
     *     transitive dependencies
     * @param scope Scope of root node in the dependency graph. If {@code dependencyFilter} filters on the scope, then
     *     this should match the requested scope. Otherwise, this parameter will not have an effect. May be
     *     {@code null}.
     * @return List of artifacts that the given (unresolved) artifact has as dependencies. The list only contains the
     *     artifacts of {@link DependencyNode} instances for that {@code dependencyFilter} returns {@code true}.
     *     Note that the returned list is inclusive of {@code unresolvedArtifact}.
     * @throws NullPointerException if {@code unresolvedArtifact} is {@code null}
     * @throws RepositoryException if one or more artifacts cannot be resolved
     */
    public final List<Artifact> resolveArtifacts(@Nullable DependencyFilter dependencyFilter,
            List<Artifact> unresolvedArtifacts, @Nullable String scope) throws RepositoryException {
        List<Dependency> dependencies = unresolvedArtifacts.stream()
            .map(unresolved -> new Dependency(unresolved, scope))
            .collect(Collectors.toList());

        if (dependencies.isEmpty()) {
            return Collections.emptyList();
        }

        CollectRequest collectRequest
            = new CollectRequest(dependencies.get(0), dependencies.subList(1, dependencies.size()), remoteRepositories);
        DependencyNode node = repositorySystem
            .collectDependencies(repositorySystemSession, collectRequest)
            .getRoot();

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setRoot(node);
        // setFilter() allows null arguments.
        dependencyRequest.setFilter(dependencyFilter);
        repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

        PreorderNodeListGenerator nodeListGenerator = new PreorderNodeListGenerator();
        node.accept(nodeListGenerator);
        return nodeListGenerator.getArtifacts(false);
    }
}
