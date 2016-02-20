package xyz.cloudkeeper.model.api;

import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Factory of runtime contexts.
 *
 * <p>A repository-context factory loads and links CloudKeeper plug-in declarations so that the contained modules can be
 * interpreted and executed. <em>Loading</em> is the process of finding the source of bundles with particular URIs and
 * creating the {@link xyz.cloudkeeper.model.bare.element.BareBundle} instances from that representation.
 * <em>Linking</em> is the process of taking bundle instances and combining them into the runtime state of CloudKeeper,
 * which is a {@link RuntimeRepository} instance. The linking process includes resolving symbolic references
 * (that is, names) into Java object references, verifying integrity, etc.
 *
 * <p>In general, the number of bundles in the repository contained in a returned runtime context does not have to match
 * the number of bundle identifiers given to a factory instance: For instance, the loading process may support the
 * notion of dependencies, which would be included in the repository as additional bundles. Likewise, the loading
 * process may eliminate duplicate bundles. Also note that the order in which bundle identifiers are passed, may be
 * significant.
 *
 * <p>This interface does not specify how duplicate bundles (or plug-in declarations) are handled. It is not even
 * required that a repository factory support more than singleton lists of bundle identifiers. Moreover, this interface
 * does not prescribe a particular caching policy across different invocations of this method. Implementations may cache
 * results between calls to this method, or they may be entirely stateless and always return fresh objects.
 */
public interface RuntimeContextFactory {
    /**
     * Returns a new future that will be completed with the (new) runtime context resulting from loading, linking, and
     * verifying the bundles identified by the given identifiers.
     *
     * <p>Callers are responsible for closing the {@link RuntimeRepository} instance that the returned future will be
     * completed with.
     *
     * <p>Assuming that {@code runtimeContext} is the {@link RuntimeContext} instance that the returned future is
     * completed with, it is guaranteed that calling this method with the result of
     * {@code runtimeContext.getRepository().getBundles().stream().map(RuntimeBundle::getBundleIdentifier).collect(Collectors.toList())}
     * will provide an equivalent runtime context.
     *
     * @param bundleIdentifiers list of bundle identifiers
     * @return future that will be completed with the repository on success or a {@link RuntimeStateProvisionException}
     *     on failure
     */
    CompletableFuture<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers);
}
