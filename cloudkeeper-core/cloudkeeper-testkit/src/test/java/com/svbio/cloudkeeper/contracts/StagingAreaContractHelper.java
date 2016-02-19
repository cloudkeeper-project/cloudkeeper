package com.svbio.cloudkeeper.contracts;

import akka.dispatch.Futures;
import com.svbio.cloudkeeper.dsl.Module;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.examples.repositories.TestKitExecutableProvider;
import com.svbio.cloudkeeper.linker.Linker;
import com.svbio.cloudkeeper.linker.LinkerOptions;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.RuntimeContextFactory;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvisionException;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeBundle;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import org.testng.Assert;
import scala.concurrent.Future;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

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
        public Future<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers) {
            RuntimeBundle fibonacciBundle = repository.getBundles().get(0);
            if (Collections.singletonList(fibonacciBundle.getBundleIdentifier()).equals(bundleIdentifiers)) {
                return Futures.successful(runtimeContext);
            }

            Assert.fail(String.format(
                "Mock runtime state provider asked for bundle with unexpected identifiers %s.", bundleIdentifiers
            ));
            return null;
        }
    }
}
