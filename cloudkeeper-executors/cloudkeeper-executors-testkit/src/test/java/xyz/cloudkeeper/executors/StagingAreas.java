package xyz.cloudkeeper.executors;

import scala.concurrent.ExecutionContext;
import xyz.cloudkeeper.dsl.Module;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.filesystem.FileStagingArea;
import xyz.cloudkeeper.linker.Linker;
import xyz.cloudkeeper.linker.LinkerOptions;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.bare.element.BareBundle;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.RuntimeRepository;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.simple.DSLExecutableProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class StagingAreas {
    private StagingAreas() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    private static final LinkerOptions LINKER_OPTIONS = new LinkerOptions.Builder()
        .setClassProvider(name -> Optional.of(Class.forName(name.getBinaryName().toString())))
        .setExecutableProvider(DSLExecutableProvider.getDefault())
        .build();

    public static <T extends Module<T>> RuntimeStateProvider runtimeStateProviderForDSLModule(Class<T> moduleClazz,
            Path basePath, ExecutionContext executionContext) throws ClassNotFoundException, LinkerException {
        ModuleFactory moduleFactory = ModuleFactory.getDefault();
        T module = moduleFactory.create(moduleClazz);
        BareBundle bundle = moduleFactory.createBundle(module);
        RuntimeRepository repository = Linker.createRepository(Collections.singletonList(bundle), LINKER_OPTIONS);
        RuntimeAnnotatedExecutionTrace absoluteTrace = Linker.createAnnotatedExecutionTrace(
            ExecutionTrace.empty(), module, Collections.<BareOverride>emptyList(), repository, LINKER_OPTIONS);
        StagingArea stagingArea;
        try (RuntimeContext runtimeContext = new RuntimeContextImpl(repository)) {
            stagingArea = new FileStagingArea.Builder(runtimeContext, absoluteTrace, basePath, executionContext)
                .build();
            return RuntimeStateProvider.of(runtimeContext, stagingArea);
        } catch (IOException exception) {
            throw new AssertionError("Non-sensical exception", exception);
        }
    }

    private static final class RuntimeContextImpl implements RuntimeContext {
        private final RuntimeRepository repository;

        private RuntimeContextImpl(RuntimeRepository repository) {
            this.repository = repository;
        }

        @Override
        public void close() { }

        @Override
        public RuntimeRepository getRepository() {
            return repository;
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }

        @Override
        public RuntimeAnnotatedExecutionTrace newAnnotatedExecutionTrace(BareExecutionTrace absoluteTrace,
                BareModule bareModule, List<? extends BareOverride> overrides) throws LinkerException {
            return Linker.createAnnotatedExecutionTrace(absoluteTrace, bareModule, overrides, repository,
                LINKER_OPTIONS);
        }
    }
}
