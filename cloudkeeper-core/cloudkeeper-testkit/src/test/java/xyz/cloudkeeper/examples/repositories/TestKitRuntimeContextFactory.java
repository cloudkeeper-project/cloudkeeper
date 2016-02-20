package xyz.cloudkeeper.examples.repositories;

import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.linker.LinkerOptions;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeContextFactory;
import xyz.cloudkeeper.model.api.RuntimeStateProvisionException;
import xyz.cloudkeeper.model.bare.element.BareBundle;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import net.florianschoppmann.java.futures.Futures;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class TestKitRuntimeContextFactory implements RuntimeContextFactory {
    private static final String TESTKIT_SCHEME = "x-testkit";

    static URI bundleIdentifier(Class<? extends TestKitBundleProvider> definingClass) {
        return URI.create(TESTKIT_SCHEME + ':' + definingClass.getName());
    }

    @Override
    public CompletableFuture<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers) {
        Objects.requireNonNull(bundleIdentifiers);

        List<BareBundle> bundles = new ArrayList<>(bundleIdentifiers.size());
        for (URI bundleIdentifier: bundleIdentifiers) {
            if (!TESTKIT_SCHEME.equals(bundleIdentifier.getScheme())) {
                return Futures.completedExceptionally(new RuntimeStateProvisionException(String.format(
                    "Expected list of bundle identifiers (URI) with scheme '%s', but list contains '%s'.",
                    TESTKIT_SCHEME, bundleIdentifier
                )));
            }

            @Nullable final String providerClassName = bundleIdentifier.getSchemeSpecificPart();
            if (providerClassName == null || providerClassName.isEmpty()) {
                return Futures.completedExceptionally(new RuntimeStateProvisionException(String.format(
                    "Expected bundle identifier (URI) that contains a class name in the schema-specific part, but "
                        + "bundle identifier is '%s'.", bundleIdentifier
                )));
            }

            try {
                Class<?> providerClass = Class.forName(providerClassName);
                if (!TestKitBundleProvider.class.isAssignableFrom(providerClass)) {
                    return Futures.completedExceptionally(new RuntimeStateProvisionException(String.format(
                        "Expected bundle identifier (URI) that contains the name of a class implementing %s, but got "
                            + "'%s'.",
                        TestKitBundleProvider.class, bundleIdentifier
                    )));
                }
                Constructor<?> constructor = providerClass.getConstructor();
                TestKitBundleProvider testKitBundleProvider = (TestKitBundleProvider) constructor.newInstance();
                bundles.add(testKitBundleProvider.get());
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                    | InvocationTargetException exception) {
                return Futures.completedExceptionally(
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
            return CompletableFuture.completedFuture(new RuntimeContextImpl(repository, linkerOptions));
        } catch (LinkerException exception) {
            return Futures.completedExceptionally(exception);
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
