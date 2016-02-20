package xyz.cloudkeeper.simple;

import net.florianschoppmann.java.futures.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.cloudkeeper.model.api.ConnectorException;
import xyz.cloudkeeper.model.api.executor.ExtendedModuleConnector;
import xyz.cloudkeeper.model.api.executor.IncompleteOutputsException;
import xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimePort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class PrefetchingModuleConnectorProvider implements ModuleConnectorProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path workspaceBasePath;

    /**
     * Constructs a new module-connector provider.
     *
     * @param workspaceBasePath base path within working directories for new module connectors will be created
     */
    public PrefetchingModuleConnectorProvider(Path workspaceBasePath) {
        this.workspaceBasePath = Objects.requireNonNull(workspaceBasePath);
    }

    @Override
    public CompletableFuture<ExtendedModuleConnector> provideModuleConnector(final StagingArea stagingArea) {
        RuntimeProxyModule module = (RuntimeProxyModule) stagingArea.getAnnotatedExecutionTrace().getModule();
        List<? extends RuntimeInPort> inPorts = module.getInPorts();
        List<CompletableFuture<Object>> futures = new ArrayList<>(inPorts.size());
        for (RuntimeInPort inPort: module.getInPorts()) {
            ExecutionTrace inPortTrace = ExecutionTrace.empty().resolveInPort(inPort.getSimpleName());
            futures.add(stagingArea.getObject(inPortTrace));
        }
        final Object[] inputValues = new Object[inPorts.size()];
        return Futures.unwrapCompletionException(
            Futures
                .collect(futures)
                .thenApply(inPortValues -> {
                    int i = 0;
                    for (Object inPortValue: inPortValues) {
                        inputValues[i] = inPortValue;
                        ++i;
                    }
                    return new PrefetchingModuleConnector(stagingArea, inputValues);
                })
        );
    }

    final class PrefetchingModuleConnector implements ExtendedModuleConnector {
        private final StagingArea stagingArea;
        private final RuntimeProxyModule module;
        private final Object[] inputValues;
        private final Object monitor = new Object();

        private final AtomicReferenceArray<Object> outputValues;

        @Nullable private Path workingDirectory;


        PrefetchingModuleConnector(StagingArea stagingArea, Object[] inputValues) {
            this.stagingArea = stagingArea;
            module = (RuntimeProxyModule) stagingArea.getAnnotatedExecutionTrace().getModule();
            this.inputValues = inputValues;

            outputValues = new AtomicReferenceArray<>(module.getOutPorts().size());
        }

        @Override
        public RuntimeAnnotatedExecutionTrace getExecutionTrace() {
            return stagingArea.getAnnotatedExecutionTrace();
        }

        @Override
        public Path getWorkingDirectory() {
            synchronized (monitor) {
                if (workingDirectory == null) {
                    try {
                        workingDirectory = Files.createTempDirectory(
                            workspaceBasePath,
                            module.getDeclaration().getQualifiedName().toSimpleName().toString()
                        );
                    } catch (IOException exception) {
                        throw new ConnectorException(String.format(
                            "Failed to create working directory within %s.", workspaceBasePath
                        ), exception);
                    }
                }
                return workingDirectory;
            }
        }

        @Override
        public void close() {
            // Synchronization is not an issue time-wise, but we need to create a happens-before relationship with
            // getWorkingDirectory() for memory consistency
            synchronized (monitor) {
                if (workingDirectory != null) {
                    try {
                        Files.walkFileTree(workingDirectory, RecursiveDeleteVisitor.getInstance());
                    } catch (IOException exception) {
                        log.warn(String.format("Could not remove working directory %s.", workingDirectory), exception);
                    }
                }
            }
        }

        @Override
        public Object getInput(SimpleName inPortName) {
            @Nullable RuntimePort inPort = module.getEnclosedElement(RuntimePort.class, inPortName);
            if (!(inPort instanceof RuntimeInPort)) {
                throw new ConnectorException(String.format("Expected name of in-port, but got '%s'.", inPortName));
            }
            return inputValues[((RuntimeInPort) inPort).getInIndex()];
        }

        @Override
        public void setOutput(SimpleName outPortName, Object value) {
            Objects.requireNonNull(value);
            @Nullable RuntimePort outPort = module.getEnclosedElement(RuntimePort.class, outPortName);
            if (!(outPort instanceof RuntimeOutPort)) {
                throw new ConnectorException(String.format("Expected name of out-port, but got '%s'.", outPortName));
            }
            outputValues.set(((RuntimeOutPort) outPort).getOutIndex(), value);
        }

        @Override
        public CompletableFuture<Void> commit() {
            List<? extends RuntimeOutPort> outPorts = module.getOutPorts();
            CompletableFuture<?>[] futures = new CompletableFuture<?>[outPorts.size()];
            int i = 0;
            for (RuntimeOutPort outPort: outPorts) {
                ExecutionTrace outPortTrace = ExecutionTrace.empty().resolveOutPort(outPort.getSimpleName());
                @Nullable Object outputValue = outputValues.get(i);
                CompletableFuture<?> future;
                if (outputValue == null) {
                    future = Futures.completedExceptionally(new IncompleteOutputsException(String.format(
                        "No value for %s.", outPort
                    )));
                } else {
                    future = stagingArea.putObject(outPortTrace, outputValue);
                }
                futures[i] = future;
                ++i;
            }
            return Futures.unwrapCompletionException(CompletableFuture.allOf(futures));
        }
    }
}
