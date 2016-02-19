package xyz.cloudkeeper.simple;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import xyz.cloudkeeper.model.api.ConnectorException;
import xyz.cloudkeeper.model.api.executor.ExtendedModuleConnector;
import xyz.cloudkeeper.model.api.executor.IncompleteOutputsException;
import xyz.cloudkeeper.model.api.executor.ModuleConnectorProvider;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import xyz.cloudkeeper.model.api.util.ScalaFutures;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeInPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimePort;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class PrefetchingModuleConnectorProvider implements ModuleConnectorProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Path workspaceBasePath;
    private final ExecutionContext executionContext;

    /**
     * Constructs a new module-connector provider.
     *
     * @param workspaceBasePath base path within working directories for new module connectors will be created
     * @param executionContext execution context that will be used for tasks that complete new futures created in the
     *     provider or in created {@link ExtendedModuleConnector} instances
     */
    public PrefetchingModuleConnectorProvider(Path workspaceBasePath, ExecutionContext executionContext) {
        this.workspaceBasePath = Objects.requireNonNull(workspaceBasePath);
        this.executionContext = Objects.requireNonNull(executionContext);
    }

    @Override
    public Future<ExtendedModuleConnector> provideModuleConnector(final StagingArea stagingArea) {
        RuntimeProxyModule module = (RuntimeProxyModule) stagingArea.getAnnotatedExecutionTrace().getModule();
        List<? extends RuntimeInPort> inPorts = module.getInPorts();
        List<Future<Object>> futures = new ArrayList<>(inPorts.size());
        for (RuntimeInPort inPort: module.getInPorts()) {
            ExecutionTrace inPortTrace = ExecutionTrace.empty().resolveInPort(inPort.getSimpleName());
            futures.add(stagingArea.getObject(inPortTrace));
        }
        final Object[] inputValues = new Object[inPorts.size()];
        return ScalaFutures
            .createListFuture(futures, executionContext)
            .map(
                new Mapper<ImmutableList<Object>, ExtendedModuleConnector>() {
                    @Override
                    public ExtendedModuleConnector apply(ImmutableList<Object> inPortValues) {
                        int i = 0;
                        for (Object inPortValue: inPortValues) {
                            inputValues[i] = inPortValue;
                            ++i;
                        }
                        return new PrefetchingModuleConnector(stagingArea, inputValues);
                    }
                },
                executionContext
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
        public Future<Object> commit() {
            List<? extends RuntimeOutPort> outPorts = module.getOutPorts();
            List<Future<RuntimeExecutionTrace>> futures = new ArrayList<>(outPorts.size());
            int i = 0;
            for (RuntimeOutPort outPort: outPorts) {
                ExecutionTrace outPortTrace = ExecutionTrace.empty().resolveOutPort(outPort.getSimpleName());
                @Nullable Object outputValue = outputValues.get(i);
                Future<RuntimeExecutionTrace> future;
                if (outputValue == null) {
                    future = Futures.failed(new IncompleteOutputsException(String.format(
                        "No value for %s.", outPort
                    )));
                } else {
                    future = stagingArea.putObject(outPortTrace, outputValue);
                }
                futures.add(future);
                ++i;
            }

            // Unfortunately, returning Future<?> is not an option (because that makes map() and flatMap() impossible
            // to use). Since futures are immutable, the following is always safe, however.
            @SuppressWarnings("unchecked")
            Future<Object> typedFuture
                = (Future<Object>) (Future<?>) ScalaFutures.createListFuture(futures, executionContext);

            return typedFuture;
        }
    }
}
