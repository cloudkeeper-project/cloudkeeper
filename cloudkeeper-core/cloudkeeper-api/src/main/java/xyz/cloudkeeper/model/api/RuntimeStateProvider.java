package xyz.cloudkeeper.model.api;

import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.InstanceProvisionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.staging.StagingAreaProvider;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.beans.element.module.MutableModule;
import xyz.cloudkeeper.model.beans.execution.MutableOverride;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.RuntimeBundle;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * CloudKeeper runtime-state provider.
 *
 * <p>The CloudKeeper runtime state comprises:
 * <ul><li>
 *     the <em>runtime context</em> of type {@link RuntimeContext}, which itself contains both the repository of
 *     CloudKeeper plug-in declarations and a Java class loader,
 * </li><li>
 *     the current call stack of type {@link RuntimeAnnotatedExecutionTrace}, and
 * </li><li>
 *     the staging area of type {@link StagingArea}.
 * </li></ul>
 *
 * <p>This provider class is capable of reconstructing all of the above, potentially across machine boundaries (given
 * some additional context in the form of additional providers).
 *
 * <p>This class has a single static factory method {@link #of(RuntimeContext, StagingArea)}, which constructs instances
 * <em>backed</em> by a given runtime context and staging area. When runtime-state provider instances are serialized,
 * only a "recipe" is kept instead that describes how the runtime state can be reconstructed. Whether a provider
 * instance is backed by an already existing runtime context and staging area, or whether it is backed by just by a
 * recipe, should be largely irrelevant to client code.
 *
 * <p>However, it is important to note that the {@link RuntimeContext} instance returned by
 * {@link #provideRuntimeContext(InstanceProvider)} will not delegate {@link RuntimeContext#close()} calls to the
 * runtime context that was originally passed to {@link #of(RuntimeContext, StagingArea)}. In contrast, if the
 * runtime-state provider instance was created during deserialization, the runtime context returned by
 * {@link #provideRuntimeContext(InstanceProvider)} will delegate {@link RuntimeContext#close()} calls to the
 * {@link RuntimeContext} instance returned by {@link RuntimeContextFactory#newRuntimeContext(List)}.
 */
public abstract class RuntimeStateProvider implements Serializable {
    private static final long serialVersionUID = -2270342162805564819L;

    /**
     * Private constructor to restrict subclassing to nested classes.
     */
    private RuntimeStateProvider() { }

    /**
     * Creates a new runtime-state provider backed by the given runtime context and staging area.
     *
     * <p>The returned runtime-state provider will be backed by the given runtime-context and staging-area instances.
     * That is, {@link #provideRuntimeContext(InstanceProvider)} will return a runtime context that delegates
     * method calls to {@code runtimeContext}. Methods {@link #provideExecutionTrace(RuntimeContext)} and
     * {@link #provideStagingArea(RuntimeContext, InstanceProvider)} will return
     * {@code stagingArea.getAnnotatedExecutionTrace()} and {@code stagingArea}, respectively.
     *
     * <p>The runtime context returned by {@link #provideRuntimeContext(InstanceProvider)} will <em>not</em> delegate
     * invocations of {@link RuntimeContext#close()} to {@code runtimeContext}. It is the caller's responsibility that
     * the {@link RuntimeContext} passed to this method is eventually closed properly (and only after the runtime-state
     * provider instance returned by this method is no longer in use).
     *
     * @param runtimeContext runtime context
     * @param stagingArea staging area
     * @return the new runtime-state provider
     */
    public static RuntimeStateProvider of(RuntimeContext runtimeContext, StagingArea stagingArea) {
        Objects.requireNonNull(runtimeContext);
        Objects.requireNonNull(stagingArea);
        return new InstanceBacked(runtimeContext, stagingArea);
    }

    @Override
    public String toString() {
        return String.format("Runtime-state provider '%s'", getExecutionTrace());
    }

    /**
     * Returns the execution trace represented by this runtime-state provider.
     *
     * @return the execution trace represented by this runtime-state provider
     */
    public abstract ExecutionTrace getExecutionTrace();

    /**
     * Returns the runtime context, given a runtime-context provider.
     *
     * <p>If this runtime state was created using {@link #of(RuntimeContext, StagingArea)}, then the returned future
     * will already be completed with only a lightweight wrapper around the original {@link RuntimeContext}. This
     * lightweight wrapper will not forward calls to {@link RuntimeContext#close()}.
     *
     * <p>Nonetheless, callers should not make assumptions on how this runtime state was created (using
     * {@link #of(RuntimeContext, StagingArea)} or by means of deserialization, see class-level documentation). Hence,
     * callers are <em>always</em> expected to eventually call {@link RuntimeContext#close()} on the instance that the
     * returned future will be completed with.
     *
     * @param instanceProvider instance provider that can provide (at least) a {@link RuntimeContextFactory} and
     *     {@link Executor} instance
     * @return future that will normally be completed with the runtime context, or exceptionally with a
     *     {@link RuntimeStateProvisionException}
     */
    public abstract CompletableFuture<RuntimeContext> provideRuntimeContext(InstanceProvider instanceProvider);

    final void requireValidRuntimeContext(RuntimeContext runtimeContext) {
        if (!(runtimeContext instanceof RuntimeContextImpl)
                || ((RuntimeContextImpl) runtimeContext).creator != this) {
            throw new IllegalArgumentException(
                "Expected runtime context that was previously returned by provideRuntimeContext()."
            );
        }
    }

    /**
     * Returns the absolute annotated execution trace, given a runtime context.
     *
     * <p>While this method is similar to
     * {@link RuntimeContext#newAnnotatedExecutionTrace(BareExecutionTrace, BareModule, List)}, this method may provide
     * additional caching. That is, the returned execution trace is not necessarily a fresh instance.
     *
     * @param runtimeContext runtime context, which consists of the CloudKeeper plug-in repository and the Java class
     *     loader
     * @return absolute annotated execution trace
     * @throws LinkerException if linking fails because of inconsistent or incomplete input
     * @throws IllegalArgumentException if the given runtime context was not previously obtained by calling
     *     {@link #provideRuntimeContext(InstanceProvider)} on this object
     */
    public abstract RuntimeAnnotatedExecutionTrace provideExecutionTrace(RuntimeContext runtimeContext)
        throws LinkerException;

    /**
     * Returns the staging area, given a runtime context and instance provider.
     *
     * <p>While this method is similar to
     * {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, InstanceProvider)},
     * this method may provide additional caching. That is, the returned staging area is not necessarily a fresh
     * instance.
     *
     * <p>Note that this method provides no guarantees that the execution trace of the returned staging area
     * (as available with {@link StagingArea#getAnnotatedExecutionTrace()}) is the same instance as the execution trace
     * returned by {@link #provideExecutionTrace(RuntimeContext)}.
     *
     * @param runtimeContext runtime context, which consists of the CloudKeeper plug-in repository and the Java class
     *     loader
     * @param instanceProvider instance provider to be used as context when creating a new staging area
     * @return the staging area
     * @throws LinkerException if linking fails because of inconsistent or incomplete input
     * @throws InstanceProvisionException if a {@link StagingArea} instance cannot be provided
     */
    public abstract StagingArea provideStagingArea(RuntimeContext runtimeContext, InstanceProvider instanceProvider)
        throws LinkerException, InstanceProvisionException;

    private static final class InstanceBacked extends RuntimeStateProvider {
        private final RuntimeContext runtimeContext;
        private final StagingArea stagingArea;

        private InstanceBacked(RuntimeContext runtimeContext, StagingArea stagingArea) {
            this.runtimeContext = runtimeContext;
            this.stagingArea = stagingArea;
        }

        @Override
        public ExecutionTrace getExecutionTrace() {
            return ExecutionTrace.copyOf(stagingArea.getAnnotatedExecutionTrace());
        }

        private Object writeReplace() {
            List<URI> bundleIdentifiers = runtimeContext.getRepository().getBundles()
                .stream()
                .map(RuntimeBundle::getBundleIdentifier)
                .collect(Collectors.toList());
            RuntimeAnnotatedExecutionTrace executionTrace = stagingArea.getAnnotatedExecutionTrace();
            return new DescriptorBacked(bundleIdentifiers, executionTrace, executionTrace.getModule(),
                executionTrace.getOverrides(), stagingArea.getStagingAreaProvider());
        }

        @Override
        public CompletableFuture<RuntimeContext> provideRuntimeContext(InstanceProvider instanceProvider) {
            return CompletableFuture.completedFuture(new RuntimeContextImpl(this, runtimeContext, true));
        }

        @Override
        public RuntimeAnnotatedExecutionTrace provideExecutionTrace(RuntimeContext runtimeContext)
                throws LinkerException {
            requireValidRuntimeContext(runtimeContext);
            return stagingArea.getAnnotatedExecutionTrace();
        }

        @Override
        public StagingArea provideStagingArea(RuntimeContext runtimeContext, InstanceProvider instanceProvider) {
            requireValidRuntimeContext(runtimeContext);
            return stagingArea;
        }
    }

    private static final class DescriptorBacked extends RuntimeStateProvider {
        private static final long serialVersionUID = 3333070734487397298L;

        private final ArrayList<URI> bundleIdentifiers;
        private final ExecutionTrace executionTrace;
        private final MutableModule<?> module;
        private final ArrayList<MutableOverride> overrides;
        private final StagingAreaProvider stagingAreaProvider;

        private DescriptorBacked(List<URI> bundleIdentifiers, BareExecutionTrace executionTrace, BareModule module,
                List<? extends BareOverride> overrides, StagingAreaProvider stagingAreaProvider) {
            this.bundleIdentifiers = new ArrayList<>(bundleIdentifiers);
            this.executionTrace = ExecutionTrace.copyOf(executionTrace);
            // Without a parent module, a module must be anonymous
            this.module = Objects.requireNonNull(MutableModule.copyOfModule(module))
                .setSimpleName((SimpleName) null);
            this.overrides = overrides.stream()
                .map(MutableOverride::copyOf)
                .collect(Collectors.toCollection(ArrayList::new));
            this.stagingAreaProvider = stagingAreaProvider;
        }

        @Override
        public ExecutionTrace getExecutionTrace() {
            return executionTrace;
        }

        @Override
        public CompletableFuture<RuntimeContext> provideRuntimeContext(InstanceProvider instanceProvider) {
            RuntimeContextFactory runtimeContextFactory;
            try {
                runtimeContextFactory = instanceProvider.getInstance(RuntimeContextFactory.class);
            } catch (InstanceProvisionException exception) {
                return Futures.completedExceptionally(exception);
            }

            return Futures.unwrapCompletionException(
                runtimeContextFactory
                    .newRuntimeContext(bundleIdentifiers)
                    .thenApply(runtimeContext -> new RuntimeContextImpl(this, runtimeContext, false))
            );
        }

        @Override
        public RuntimeAnnotatedExecutionTrace provideExecutionTrace(RuntimeContext runtimeContext)
                throws LinkerException {
            requireValidRuntimeContext(runtimeContext);
            return runtimeContext.newAnnotatedExecutionTrace(executionTrace, module, overrides);
        }

        @Override
        public StagingArea provideStagingArea(RuntimeContext runtimeContext, InstanceProvider instanceProvider)
                throws LinkerException, InstanceProvisionException {
            requireValidRuntimeContext(runtimeContext);
            return stagingAreaProvider.provideStaging(runtimeContext, provideExecutionTrace(runtimeContext),
                instanceProvider);
        }
    }

    private static final class RuntimeContextImpl implements RuntimeContext {
        private final RuntimeStateProvider creator;
        private final RuntimeContext delegate;
        private final boolean isIgnoreClose;

        private RuntimeContextImpl(RuntimeStateProvider creator, RuntimeContext delegate, boolean isIgnoreClose) {
            this.creator = creator;
            this.delegate = delegate;
            this.isIgnoreClose = isIgnoreClose;
        }

        @Override
        public void close() throws IOException {
            if (!isIgnoreClose) {
                delegate.close();
            }
        }

        @Override
        public RuntimeRepository getRepository() {
            return delegate.getRepository();
        }

        @Override
        public ClassLoader getClassLoader() {
            return delegate.getClassLoader();
        }

        @Override
        public RuntimeAnnotatedExecutionTrace newAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace,
                BareModule bareModule, List<? extends BareOverride> overrides) throws LinkerException {
            return delegate.newAnnotatedExecutionTrace(absoluteTrace, bareModule, overrides);
        }
    }
}
