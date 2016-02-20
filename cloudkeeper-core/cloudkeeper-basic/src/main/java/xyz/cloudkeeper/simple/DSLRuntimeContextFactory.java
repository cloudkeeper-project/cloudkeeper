package xyz.cloudkeeper.simple;

import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.dsl.Module;
import xyz.cloudkeeper.dsl.ModuleFactory;
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
import xyz.cloudkeeper.model.beans.SystemBundle;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class DSLRuntimeContextFactory implements RuntimeContextFactory {
    private final ClassLoader classLoader;
    private final ModuleFactory moduleFactory;
    private final DSLExecutableProvider executableProvider;
    private final Executor executor;

    private DSLRuntimeContextFactory(Builder builder) {
        classLoader = builder.classLoader;
        moduleFactory = builder.moduleFactory;
        executableProvider = new DSLExecutableProvider(moduleFactory);
        executor = builder.executor;
    }

    public static final class Builder {
        private final Executor executor;
        private ClassLoader classLoader = SystemBundle.class.getClassLoader();
        private ModuleFactory moduleFactory = ModuleFactory.getDefault();

        public Builder(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
        }

        public Builder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder setModuleFactory(ModuleFactory moduleFactory) {
            this.moduleFactory = Objects.requireNonNull(moduleFactory);
            return this;
        }

        public DSLRuntimeContextFactory build() {
            return new DSLRuntimeContextFactory(this);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Module<T>> Module<?> createModule(Class<?> clazz) {
        return moduleFactory.create((Class<T>) clazz);
    }

    private static String moduleClassName(List<URI> bundleIdentifiers) throws RuntimeStateProvisionException {
        Objects.requireNonNull(bundleIdentifiers);
        if (bundleIdentifiers.size() != 1) {
            throw new RuntimeStateProvisionException(String.format(
                "Expected a list with exactly one bundle identifier (URI), but got %s.", bundleIdentifiers
            ));
        }

        URI bundleIdentifier =  bundleIdentifiers.get(0);

        if (!Module.URI_SCHEME.equals(bundleIdentifier.getScheme())) {
            throw new RuntimeStateProvisionException(String.format(
                "Expected bundle identifier (URI) with scheme '%s', but bundle identifier is '%s'.",
                Module.URI_SCHEME, bundleIdentifier
            ));
        }

        final String moduleClassName = bundleIdentifier.getSchemeSpecificPart();
        if (moduleClassName.isEmpty()) {
            throw new RuntimeStateProvisionException(String.format(
                "Expected bundle identifier (URI) that contains a class name in the schema-specific part, but bundle "
                    + "identifier is '%s'.", bundleIdentifier
            ));
        }

        return moduleClassName;
    }

    @Override
    public CompletableFuture<RuntimeContext> newRuntimeContext(List<URI> bundleIdentifiers) {
        final String moduleClassName;
        try {
            moduleClassName = moduleClassName(bundleIdentifiers);
        } catch (RuntimeStateProvisionException exception) {
            return Futures.completedExceptionally(exception);
        }

        CompletionStage<RuntimeContext> completionStage = Futures.supplyAsync(() -> {
            Class<?> moduleClass = moduleFactory.loadClass(Name.qualifiedName(moduleClassName));
            Module<?> module = createModule(moduleClass);
            BareBundle bundle = moduleFactory.createBundle(module);
            LinkerOptions linkerOptions = new LinkerOptions.Builder()
                .setClassProvider(
                    name -> Optional.of(Class.forName(name.getBinaryName().toString(), true, classLoader))
                )
                .setExecutableProvider(executableProvider)
                .build();
            RuntimeRepository repository
                = Linker.createRepository(Collections.singletonList(bundle), linkerOptions);
            return new RuntimeContextImpl(repository, classLoader);
        }, executor);
        return Futures.translateException(
            completionStage,
            throwable -> new RuntimeStateProvisionException(String.format(
                "Failed to provide runtime context for DSL module '%s' and its dependencies.", moduleClassName
            ), Futures.unwrapCompletionException(throwable))
        );
    }

    private static final class RuntimeContextImpl implements RuntimeContext {
        private final RuntimeRepository repository;
        private final ClassLoader classLoader;

        private RuntimeContextImpl(RuntimeRepository repository, ClassLoader classLoader) {
            this.repository = repository;
            this.classLoader = classLoader;
        }

        @Override
        public void close() { }

        @Override
        public RuntimeRepository getRepository() {
            return repository;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public RuntimeAnnotatedExecutionTrace newAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace,
                BareModule bareModule, List<? extends BareOverride> overrides) throws LinkerException {
            LinkerOptions linkerOptions = new LinkerOptions.Builder()
                .setUnmarshalClassLoader(classLoader)
                .build();
            return Linker.createAnnotatedExecutionTrace(absoluteTrace, bareModule, overrides, repository,
                linkerOptions);
        }
    }
}
