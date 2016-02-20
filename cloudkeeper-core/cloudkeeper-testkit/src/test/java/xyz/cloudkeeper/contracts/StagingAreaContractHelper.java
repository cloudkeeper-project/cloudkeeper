package xyz.cloudkeeper.contracts;

import org.testng.Assert;
import xyz.cloudkeeper.dsl.Module;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.examples.repositories.TestKitExecutableProvider;
import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.linker.LinkerOptions;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeContextFactory;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.InstanceProvisionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.bare.element.BareBundle;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.RuntimeBundle;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Internal helper class that factors out code needed both by {@link StagingAreaContract} and
 * {@link RemoteStagingAreaContract}.
 */
final class StagingAreaContractHelper {
    private final StagingAreaContractProvider provider;
    private final LinkerOptions linkerOptions;
    private final RuntimeRepository repository;
    private final RuntimeAnnotatedExecutionTrace rootTrace;
    private final RuntimeContext runtimeContext;
    private final RuntimeContextFactory runtimeContextFactory;

    <T extends Module<T>> StagingAreaContractHelper(StagingAreaContractProvider provider, Class<T> moduleClass) {
        this.provider = provider;

        Module<T> module = ModuleFactory.getDefault().create(moduleClass);
        try {
            BareBundle bareBundle = ModuleFactory.getDefault().createBundle(module);
            linkerOptions = new LinkerOptions.Builder()
                .setExecutableProvider(TestKitExecutableProvider.getDefault())
                .build();
            repository = Linker.createRepository(Collections.singletonList(bareBundle), linkerOptions);
            rootTrace = Linker.createAnnotatedExecutionTrace(
                ExecutionTrace.empty(),
                module,
                Collections.<BareOverride>emptyList(),
                repository,
                linkerOptions
            );
        } catch (ClassNotFoundException | LinkerException exception) {
            Assert.fail("Exception while setting up test case.", exception);
            // The following line is necessary for the compiler.
            throw new AssertionError("This line should never be never reached.", exception);
        }

        runtimeContext = new RuntimeContextImpl();
        runtimeContextFactory = new RuntimeContextFactoryImpl();
    }

    StagingArea createStagingArea(String identifier) {
        return provider.getStagingArea(identifier, runtimeContext, rootTrace);
    }

    private final class RuntimeContextImpl implements RuntimeContext {
        @Override
        public void close() throws IOException { }

        @Override
        public RuntimeRepository getRepository() {
            return repository;
        }

        @Override
        public ClassLoader getClassLoader() {
            return ClassLoader.getSystemClassLoader();
        }

        @Override
        public RuntimeAnnotatedExecutionTrace newAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace,
                BareModule bareModule, List<? extends BareOverride> overrides) throws LinkerException {
            return Linker.createAnnotatedExecutionTrace(
                absoluteTrace, bareModule, overrides, repository, linkerOptions);
        }
    }

    RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    RuntimeRepository getRepository() {
        return repository;
    }

    InstanceProvider getInstanceProvider(InstanceProvider delegate) {
        return new InstanceProviderImpl(delegate);
    }


    private final class InstanceProviderImpl implements InstanceProvider {
        private final InstanceProvider delegate;

        private InstanceProviderImpl(InstanceProvider delegate) {
            this.delegate = delegate;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getInstance(Class<T> requestedClass) throws InstanceProvisionException {
            if (RuntimeContextFactory.class.equals(requestedClass)) {
                return (T) runtimeContextFactory;
            } else {
                return delegate.getInstance(requestedClass);
            }
        }
    }

    private class RuntimeContextFactoryImpl implements RuntimeContextFactory {
        @Override
        public CompletableFuture<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers) {
            RuntimeBundle fibonacciBundle = repository.getBundles().get(0);
            if (Collections.singletonList(fibonacciBundle.getBundleIdentifier()).equals(bundleIdentifiers)) {
                return CompletableFuture.completedFuture(runtimeContext);
            }

            Assert.fail(String.format(
                "Mock runtime state provider asked for bundle with unexpected identifiers %s.", bundleIdentifiers
            ));
            return null;
        }
    }
}
