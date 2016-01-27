package com.svbio.cloudkeeper.examples.repositories;

import akka.dispatch.Futures;
import com.svbio.cloudkeeper.linker.Linker;
import com.svbio.cloudkeeper.linker.LinkerOptions;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.RuntimeContextFactory;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvisionException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TestKitRuntimeContextFactory implements RuntimeContextFactory {
    private static final String TESTKIT_SCHEME = "x-testkit";

    static URI bundleIdentifier(Class<? extends TestKitBundleProvider> definingClass) {
        return URI.create(TESTKIT_SCHEME + ':' + definingClass.getName());
    }

    @Override
    public Future<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers) {
        Objects.requireNonNull(bundleIdentifiers);

        List<BareBundle> bundles = new ArrayList<>(bundleIdentifiers.size());
        for (URI bundleIdentifier: bundleIdentifiers) {
            if (!TESTKIT_SCHEME.equals(bundleIdentifier.getScheme())) {
                return Futures.failed(new RuntimeStateProvisionException(String.format(
                    "Expected list of bundle identifiers (URI) with scheme '%s', but list contains '%s'.",
                    TESTKIT_SCHEME, bundleIdentifier
                )));
            }

            @Nullable final String providerClassName = bundleIdentifier.getSchemeSpecificPart();
            if (providerClassName == null || providerClassName.isEmpty()) {
                return Futures.failed(new RuntimeStateProvisionException(String.format(
                    "Expected bundle identifier (URI) that contains a class name in the schema-specific part, but bundle "
                        + "identifier is '%s'.", bundleIdentifier
                )));
            }

            try {
                Class<?> providerClass = Class.forName(providerClassName);
                if (!TestKitBundleProvider.class.isAssignableFrom(providerClass)) {
                    return Futures.failed(new RuntimeStateProvisionException(String.format(
                        "Expected bundle identifier (URI) that contains the name of a class implementing %s, but got '%s'.",
                        TestKitBundleProvider.class, bundleIdentifier
                    )));
                }
                Constructor<?> constructor = providerClass.getConstructor();
                TestKitBundleProvider testKitBundleProvider = (TestKitBundleProvider) constructor.newInstance();
                bundles.add(testKitBundleProvider.get());
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                    | InvocationTargetException exception) {
                return Futures.failed(
                    new RuntimeStateProvisionException(String.format(
                        "Could not instantiate class %s.", providerClassName
                    ), exception)
                );
            }
        }

        LinkerOptions linkerOptions = new LinkerOptions.Builder()
            .setClassProvider(name -> Optional.of(Class.forName(name.getBinaryName().toString())))
            .setExecutableProvider(TestKitExecutableProvider.getDefault())
            .build();
        try {
            RuntimeRepository repository = Linker.createRepository(bundles, linkerOptions);
            return Futures.successful(new RuntimeContextImpl(repository, linkerOptions));
        } catch (LinkerException exception) {
            return Futures.failed(exception);
        }
    }

    private static final class RuntimeContextImpl implements RuntimeContext {
        private final RuntimeRepository repository;
        private final LinkerOptions linkerOptions;

        private RuntimeContextImpl(RuntimeRepository repository, LinkerOptions linkerOptions) {
            this.repository = repository;
            this.linkerOptions = linkerOptions;
        }

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
            return Linker.createAnnotatedExecutionTrace(absoluteTrace, bareModule, overrides, repository,
                linkerOptions);
        }
    }
}
