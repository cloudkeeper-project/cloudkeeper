package com.svbio.cloudkeeper.examples.repositories;

import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.linker.ExecutableProvider;
import com.svbio.cloudkeeper.model.api.Executable;
import com.svbio.cloudkeeper.model.api.ExecutionException;
import com.svbio.cloudkeeper.model.api.ModuleConnector;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvisionException;
import com.svbio.cloudkeeper.model.api.UserException;
import com.svbio.cloudkeeper.model.immutable.element.Name;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of {@link ExecutableProvider} that uses a {@link ModuleFactory} in order to load classes based on the
 * CloudKeeper DSL.
 */
public class TestKitExecutableProvider implements ExecutableProvider {
    private static final TestKitExecutableProvider DEFAULT_INSTANCE = new TestKitExecutableProvider(ModuleFactory.getDefault());

    private final ModuleFactory moduleFactory;

    public TestKitExecutableProvider(ModuleFactory moduleFactory) {
        this.moduleFactory = Objects.requireNonNull(moduleFactory);
    }

    /**
     * Returns the default Java connector that uses the default module factory returned by
     * {@link ModuleFactory#getDefault()}.
     *
     * <p>The returned Java connector is equivalent (barring caches) to a Java connector created with
     * {@link #TestKitExecutableProvider(ModuleFactory)}, where {@link ModuleFactory#getDefault()} is is passed as argument.
     */
    public static TestKitExecutableProvider getDefault() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public Optional<Executable> provideExecutable(Name name) throws RuntimeStateProvisionException {
        try {
            Class<?> untypedClass = moduleFactory.loadClass(name);

            if (!(SimpleModule.class.isAssignableFrom(untypedClass))) {
                throw new RuntimeStateProvisionException(String.format(
                    "Cannot instantiate simple-module declaration class '%s' because %s does not derive from %s.",
                    name, untypedClass, SimpleModule.class
                ));
            }

            return Optional.of(createExecutable(untypedClass));
        } catch (ClassNotFoundException exception) {
            throw new RuntimeStateProvisionException(String.format("Could not load Java class '%s'.", name), exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends SimpleModule<T>> ExecutableImpl<T> createExecutable(Class<?> untypedClass) {
        Class<T> typedClass = (Class<T>) untypedClass;
        return new ExecutableImpl<>(moduleFactory, typedClass);
    }

    static final class ExecutableImpl<T extends SimpleModule<T>> implements Executable {
        private final ModuleFactory moduleFactory;
        private final Class<T> moduleClass;

        ExecutableImpl(ModuleFactory moduleFactory, Class<T> moduleClass) {
            this.moduleFactory = moduleFactory;
            this.moduleClass = moduleClass;
        }

        @Override
        public void run(ModuleConnector moduleConnector) throws ExecutionException {
            SimpleModule<T> simpleModule;
            try {
                simpleModule = moduleFactory.createWithModuleConnector(moduleClass, moduleConnector);
            } catch (RuntimeException exception) {
                throw new ExecutionException(String.format(
                    "Failed to instantiate simple module from CloudKeeper DSL %s.", moduleClass
                ), exception);
            }

            try {
                simpleModule.run();
            } catch (Exception exception) {
                throw new UserException(exception);
            }
        }
    }
}
