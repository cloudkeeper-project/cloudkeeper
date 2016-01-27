package com.svbio.cloudkeeper.executors;

import akka.dispatch.OnComplete;
import akka.dispatch.Recover;
import com.svbio.cloudkeeper.model.api.ExecutionException;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import com.svbio.cloudkeeper.simple.CharacterStreamCommunication.Splitter;
import com.svbio.cloudkeeper.simple.SimpleInstanceProvider;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;

/**
 * Simple-module executor implementation that starts a separate JVM in order to execute simple modules.
 *
 * <p>This executor starts a new {@link Process} from the executable path configured at construction time. It serializes
 * the {@link RuntimeStateProvider} instance passed to {@link #submit(RuntimeStateProvider, Future)} and pipes the
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


    private final ExecutionContext longLivedExecutionContext;
    private final ExecutionContext shortLivedExecutionContext;
    private final CommandProvider commandProvider;
    private final InstanceProvider instanceProvider;

    private ForkingExecutor(ExecutionContext shortLivedExecutionContext, ExecutionContext longLivedExecutionContext,
            CommandProvider commandProvider, InstanceProvider instanceProvider) {
        this.longLivedExecutionContext = longLivedExecutionContext;
        this.shortLivedExecutionContext = shortLivedExecutionContext;
        this.commandProvider = commandProvider;
        this.instanceProvider = instanceProvider;
    }

    /**
     * This class is used to create forking simple-module executors.
     */
    public static final class Builder {
        private final ExecutionContext longLivedExecutionContext;
        private final ExecutionContext shortLivedExecutionContext;
        private final CommandProvider commandProvider;
        @Nullable private InstanceProvider instanceProvider;

        /**
         * Constructor.
         *
         * <p>If {@code longLivedExecutionContext} is bounded, then {@code shortLivedExecutionContext} should not be the
         * same execution context as {@code longLivedExecutionContext}. Otherwise, a deadlock situation may arise when
         * the cancellation could only execute after the process has terminated.
         *
         * <p>See {@link #submit(RuntimeStateProvider, Future)} for a description of the "interface" that any command
         * returned by the given command provider is expected to satisfy.
         *
         * @param shortLivedExecutionContext execution context for running short-lived tasks such as cancelling an
         *     execution
         * @param longLivedExecutionContext execution context for running long-lived tasks (reading the output from
         *     processes)
         * @param commandProvider provider of the commands that will be passed to
         *     {@link ProcessBuilder#ProcessBuilder(List)}
         * @throws NullPointerException if any argument is null
         */
        public Builder(ExecutionContext shortLivedExecutionContext, ExecutionContext longLivedExecutionContext,
                CommandProvider commandProvider) {
            this.longLivedExecutionContext = Objects.requireNonNull(longLivedExecutionContext);
            this.shortLivedExecutionContext = Objects.requireNonNull(shortLivedExecutionContext);
            this.commandProvider = Objects.requireNonNull(commandProvider);
        }

        /**
         * Sets the instance provider for this builder.
         *
         * <p>By default, a {@link SimpleInstanceProvider} will be used (using the
         * short-lived {@link ExecutionContext} passed to
         * {@link #Builder(ExecutionContext, ExecutionContext, CommandProvider)}).
         *
         * @param instanceProvider instance provider, may be null to set to the default
         * @return this builder
         */
        public Builder setInstanceProvider(@Nullable InstanceProvider instanceProvider) {
            this.instanceProvider = instanceProvider;
            return this;
        }

        public ForkingExecutor build() {
            InstanceProvider actualInstanceProvider = instanceProvider == null
                ? new SimpleInstanceProvider.Builder(shortLivedExecutionContext).build()
                : instanceProvider;
            return new ForkingExecutor(shortLivedExecutionContext, longLivedExecutionContext, commandProvider,
                actualInstanceProvider);
        }
    }

    private static final class RecoverWithSubmissionTime extends Recover<SimpleModuleExecutorResult> {
        private final IntermediateResults intermediateResults;

        private RecoverWithSubmissionTime(IntermediateResults intermediateResults) {
            this.intermediateResults = intermediateResults;
        }

        @Override
        public SimpleModuleExecutorResult recover(Throwable throwable) throws Throwable {
            return resultBuilder(intermediateResults)
                .setException(new ExecutionException(
                    "Exception while trying to execute simple module in a forked JVM.", throwable
                ))
                .addProperty(COMPLETION_TIME_MILLIS, System.currentTimeMillis())
                .build();
        }
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
     * {@inheritDoc}
     *
     * <p>This method uses {@link ProcessBuilder} to start a new {@link Process}. The serialized
     * {@link RuntimeStateProvider} instance will be provided to the process through standard-in, and a serialized
     * {@link SimpleModuleExecutorResult} will be read from its stdout.
     */
    @Override
    public Future<SimpleModuleExecutorResult> submit(RuntimeStateProvider runtimeStateProvider,
            @Nullable Future<String> cancellationFuture) {
        Objects.requireNonNull(runtimeStateProvider);
        IntermediateResults intermediateResults = new IntermediateResults(System.currentTimeMillis());
        return runtimeStateProvider
            .mapRuntimeContext(instanceProvider, runtimeContext -> {
                RuntimeAnnotatedExecutionTrace trace = runtimeStateProvider.provideExecutionTrace(runtimeContext);
                ImmutableList<String> commandLine = ImmutableList.copyOf(commandProvider.getCommand(trace));
                intermediateResults.commandLine = commandLine;
                final Process process = new ProcessBuilder(commandLine)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
                if (cancellationFuture != null) {
                    cancellationFuture.onComplete(new OnComplete<String>() {
                        @Override
                        public void onComplete(@Nullable Throwable throwable, @Nullable String reason) {
                            process.destroyForcibly();
                        }
                    }, shortLivedExecutionContext);
                }
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
            }, longLivedExecutionContext)
            .recover(new RecoverWithSubmissionTime(intermediateResults), shortLivedExecutionContext);
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
