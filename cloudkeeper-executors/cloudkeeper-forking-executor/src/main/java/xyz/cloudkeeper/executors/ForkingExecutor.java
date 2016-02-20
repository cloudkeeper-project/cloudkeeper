package xyz.cloudkeeper.executors;

import net.florianschoppmann.java.futures.Futures;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;
import xyz.cloudkeeper.simple.CharacterStreamCommunication.Splitter;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Simple-module executor implementation that starts a separate JVM in order to execute simple modules.
 *
 * <p>This executor starts a new {@link Process} using the command-line returned by the configured
 * {@link CommandProvider}. It serializes
 * the {@link RuntimeStateProvider} instance passed to {@link #submit(RuntimeStateProvider)} and pipes the
 * serialized representation to the new process. Subsequently it reads the {@link SimpleModuleExecutorResult}, which the
 * process is expected to write to standard out. The standard-error stream of the forked process is redirected to the
 * standard error of this process.
 */
public final class ForkingExecutor implements SimpleModuleExecutor {
    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the command line (returned by
     * {@link CommandLines#escape(List)} after
     * {@link CommandProvider#getCommand(RuntimeAnnotatedExecutionTrace)} and passed on to
     * {@link ProcessBuilder#ProcessBuilder(List)}).
     */
    public static final SimpleName COMMAND_LINE = SimpleName.identifier("commandLine");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the process exit code returned by
     * {@link Process#waitFor()}. The type of this property is {@link Long}.
     */
    public static final SimpleName EXIT_VALUE = SimpleName.identifier("exitCode");

    private final Executor executor;
    private final CommandProvider commandProvider;
    private final InstanceProvider instanceProvider;

    /**
     * Constructor.
     *
     * @param executor executor for running long-lived tasks (used for reading the output from and waiting for new
     *     {@link Process} instances)
     * @param commandProvider Provider of the commands that will be passed to
     *     {@link ProcessBuilder#ProcessBuilder(List)}. See {@link #submit(RuntimeStateProvider)} for a description of
     *     the "interface" that any command returned by the provider is expected to satisfy.
     * @param instanceProvider instance provider (used to pass to
     *     {@link RuntimeStateProvider#provideRuntimeContext(InstanceProvider)})
     */
    public ForkingExecutor(Executor executor, CommandProvider commandProvider, InstanceProvider instanceProvider) {
        this.executor = executor;
        this.commandProvider = commandProvider;
        this.instanceProvider = instanceProvider;
    }

    private static SimpleModuleExecutorResult.Builder resultBuilder(IntermediateResults intermediateResults) {
        SimpleModuleExecutorResult.Builder resultBuilder
            = new SimpleModuleExecutorResult.Builder(Name.qualifiedName(ForkingExecutor.class.getName()))
                .addProperty(SUBMISSION_TIME_MILLIS, intermediateResults.submissionTime);
        @Nullable ImmutableList<String> commandLine = intermediateResults.commandLine;
        if (commandLine != null) {
            resultBuilder.addProperty(COMMAND_LINE, CommandLines.escape(commandLine));
        }
        @Nullable Integer exitValue = intermediateResults.exitValue;
        if (exitValue != null) {
            resultBuilder.addProperty(EXIT_VALUE, (long) (int) exitValue);
        }
        return resultBuilder;
    }

    /**
     * Synchronously starts a new process, pipes the {@link RuntimeStateProvider} to it, and reads the result (and log)
     * from the process output.
     */
    private SimpleModuleExecutorResult launch(RuntimeStateProvider runtimeStateProvider, CompletableFuture<?> future,
            IntermediateResults intermediateResults, RuntimeContext runtimeContext)
            throws LinkerException, IOException, InterruptedException {
        RuntimeAnnotatedExecutionTrace trace = runtimeStateProvider.provideExecutionTrace(runtimeContext);
        ImmutableList<String> commandLine = ImmutableList.copyOf(commandProvider.getCommand(trace));
        intermediateResults.commandLine = commandLine;
        final Process process = new ProcessBuilder(commandLine)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
        future.whenComplete((result, failure) -> {
            if (failure instanceof CancellationException) {
                process.destroyForcibly();
            }
        });

        SimpleModuleExecutorResult moduleExecutionResult;
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(process.getOutputStream())) {
            objectOutputStream.writeObject(runtimeStateProvider);
        }
        // In a new try-with-resources block because we want to flush the output stream.
        try (
            Splitter<SimpleModuleExecutorResult> splitter = new Splitter<>(
                SimpleModuleExecutorResult.class,
                new BufferedReader(new InputStreamReader(process.getInputStream()))
            )
        ) {
            splitter.consumeAll();
            moduleExecutionResult = splitter.getResult();
        }
        intermediateResults.exitValue = process.waitFor();
        return resultBuilder(intermediateResults)
            .addExecutionResult(moduleExecutionResult)
            .addProperty(COMPLETION_TIME_MILLIS, System.currentTimeMillis())
            .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method uses {@link ProcessBuilder} to start a new {@link Process}. The serialized
     * {@link RuntimeStateProvider} instance will be provided to the process through standard-in, and a serialized
     * {@link SimpleModuleExecutorResult} will be read from its stdout.
     */
    @Override
    public CompletableFuture<SimpleModuleExecutorResult> submit(RuntimeStateProvider runtimeStateProvider) {
        Objects.requireNonNull(runtimeStateProvider);
        CompletableFuture<SimpleModuleExecutorResult> future = new CompletableFuture<>();
        IntermediateResults intermediateResults = new IntermediateResults(System.currentTimeMillis());

        // Asynchronously start a forked process and read the result.
        CompletionStage<SimpleModuleExecutorResult> resultStage = Futures.thenApplyWithResourceAsync(
            runtimeStateProvider.provideRuntimeContext(instanceProvider),
            runtimeContext -> launch(runtimeStateProvider, future, intermediateResults, runtimeContext),
            executor
        );
        CompletionStage<SimpleModuleExecutorResult> recoveryStage = resultStage.exceptionally(
            failure -> resultBuilder(intermediateResults)
                .setException(new ExecutionException(
                    "Exception while trying to execute simple module in a forked JVM.",
                    Futures.unwrapCompletionException(failure)
                ))
                .addProperty(COMPLETION_TIME_MILLIS, System.currentTimeMillis())
                .build()
        );
        Futures.completeWith(future, recoveryStage);
        return future;
    }

    private static final class IntermediateResults {
        private final long submissionTime;
        @Nullable private volatile ImmutableList<String> commandLine;
        @Nullable private volatile Integer exitValue;

        IntermediateResults(long submissionTime) {
            this.submissionTime = submissionTime;
        }
    }
}
