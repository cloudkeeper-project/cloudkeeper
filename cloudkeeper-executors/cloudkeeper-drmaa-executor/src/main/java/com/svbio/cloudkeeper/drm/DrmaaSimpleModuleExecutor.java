package com.svbio.cloudkeeper.drm;

import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.dispatch.Recover;
import akka.japi.Option;
import com.svbio.cloudkeeper.executors.CommandLines;
import com.svbio.cloudkeeper.executors.CommandProvider;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.api.ExecutionException;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvisionException;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import com.svbio.cloudkeeper.simple.CharacterStreamCommunication.Splitter;
import com.svbio.cloudkeeper.simple.SimpleInstanceProvider;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.ExitTimeoutException;
import org.ggf.drmaa.FileTransferMode;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.Promise;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Simple-module executor implementation that uses the Distributed Resource Management Application API v1 (DRMAA) to
 * execute simple modules.
 *
 * <p>This executor submits a new {@link JobTemplate} to the DRMAA {@link Session} configured at construction time. It
 * serializes the {@link RuntimeStateProvider} instance passed to {@link #submit(RuntimeStateProvider, Future)} into a
 * file that will be configured (with {@link JobTemplate#setInputPath(String)}) to provide the standard input for the
 * submitted job. The distributed resource manager schedules and executes the configured command so that the
 * standard-out stream of the job is written to the file configured with {@link JobTemplate#setOutputPath(String)}. This
 * executor subsequently deserializes the {@link SimpleModuleExecutorResult} from there. The standard-error stream of
 * the job will (temporarily) be written to a file configured with {@link JobTemplate#setErrorPath(String)}.
 * Subsequently, it will be logged to the standard-error stream of the current process.
 */
public final class DrmaaSimpleModuleExecutor implements SimpleModuleExecutor {
    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the timestamp (as returned by
     * {@link System#currentTimeMillis()}) when {@link Session#runJob(JobTemplate)}
     * was called.
     */
    public static final SimpleName DRMAA_SUBMISSION_TIME_MILLIS = SimpleName.identifier("drmaaSubmissionTimeMillis");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the DRMAA job id returned by
     * {@link Session#runJob(JobTemplate)}.
     */
    public static final SimpleName JOB_ID = SimpleName.identifier("jobId");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the DRMAA native arguments (returned by
     * {@link NativeSpecificationProvider#getNativeSpecification(RuntimeAnnotatedExecutionTrace)} and passed on to DRMAA
     * with {@link JobTemplate#setNativeSpecification(String)}).
     */
    public static final SimpleName NATIVE_ARGUMENTS = SimpleName.identifier("nativeArguments");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the DRMAA command line (returned by
     * {@link CommandLines#escape(java.util.List)} after
     * {@link CommandProvider#getCommand(RuntimeAnnotatedExecutionTrace)} and passed on to DRMAA with
     * {@link JobTemplate#setRemoteCommand(String)} and {@link JobTemplate#setArgs(java.util.List)}).
     */
    public static final SimpleName COMMAND_LINE = SimpleName.identifier("commandLine");

    /**
     * Name of property in {@link SimpleModuleExecutorResult} that contains the process exit value returned by
     * {@link JobInfo#getExitStatus()}. The type of this property is {@link Long}.
     */
    public static final SimpleName EXIT_VALUE = SimpleName.identifier("exitCode");

    private static final Pattern NON_JOB_NAME_CHARACTERS = Pattern.compile("[^A-Za-z0-9_]");
    private static final FileTransferMode TRANSFER_FILES = new FileTransferMode(true, true, true);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Session drmaaSession;
    private final Path jobIOBasePath;
    private final CommandProvider commandProvider;
    private final long waitTimeoutSeconds;
    private final long errorWaitMilliseconds;
    private final ExecutionContext shortLivedExecutionContext;
    private final ScheduledExecutorService longLivedExecutorService;
    private final InstanceProvider instanceProvider;
    private final NativeSpecificationProvider nativeSpecificationProvider;

    private final Map<String, AwaitedJob> awaitedJobMap = new LinkedHashMap<>();

    private boolean waitTaskIsRunning = false;

    private final WaitForJobMapper waitForJobMapper = new WaitForJobMapper();
    private final WaitForNextJobTask waitForNextJobTask = new WaitForNextJobTask();

    private DrmaaSimpleModuleExecutor(Session drmaaSession, Path jobIOBasePath, CommandProvider commandProvider,
            NativeSpecificationProvider nativeSpecificationProvider, InstanceProvider instanceProvider,
            long waitTimeoutSeconds, long errorWaitMilliseconds, ExecutionContext shortLivedExecutionContext,
            ScheduledExecutorService longLivedExecutorService) {
        this.drmaaSession = drmaaSession;
        this.jobIOBasePath = jobIOBasePath;
        this.commandProvider = commandProvider;
        this.nativeSpecificationProvider = nativeSpecificationProvider;
        this.instanceProvider = instanceProvider;
        this.waitTimeoutSeconds = waitTimeoutSeconds;
        this.errorWaitMilliseconds = errorWaitMilliseconds;
        this.shortLivedExecutionContext = shortLivedExecutionContext;
        this.longLivedExecutorService = longLivedExecutorService;
    }

    private enum EmptyNativeSpecificationProvider implements NativeSpecificationProvider {
        INSTANCE;

        @Override
        public String getNativeSpecification(RuntimeAnnotatedExecutionTrace trace) {
            return "";
        }
    }

    /**
     * This class is used to create DRMAA simple-module executors.
     */
    public static final class Builder {
        private final Session session;
        private final Path jobIOBasePath;
        private final CommandProvider commandProvider;
        private final ExecutionContext shortLivedExecutionContext;
        private final ScheduledExecutorService longLivedExecutorService;
        @Nullable private NativeSpecificationProvider nativeSpecificationProvider = null;
        @Nullable private InstanceProvider instanceProvider = null;
        private long waitTimeoutSeconds = TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES);
        private long errorWaitMilliseconds = TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);

        /**
         * Constructor.
         *
         * <p>Typically, {@code shortLivedExecutionContext} should not be the same executor service as
         * {@code longLivedExecutionContext}. Otherwise, a deadlock situation may arise when the
         * cancellation could only execute after the process has terminated.
         *
         * @param session DRMAA session instance. {@link Session#init(String)} must have been called before.
         * @param jobIOBasePath base path where job-specific directories will be created (that will contain stdin,
         *     stdout, and stderr of submitted jobs)
         * @param commandProvider provider of the command-line
         * @param shortLivedExecutionContext execution context that will be used to execute short-lived asynchronous
         *     tasks
         * @param longLivedExecutorService executor service that will be used to execute potentially long-lived tasks
         *     that make blocking calls to {@link Session#wait(String, long)}
         */
        public Builder(Session session, Path jobIOBasePath, CommandProvider commandProvider,
                ExecutionContext shortLivedExecutionContext, ScheduledExecutorService longLivedExecutorService) {
            this.session = Objects.requireNonNull(session);
            this.jobIOBasePath = Objects.requireNonNull(jobIOBasePath);
            this.commandProvider = Objects.requireNonNull(commandProvider);
            this.shortLivedExecutionContext = Objects.requireNonNull(shortLivedExecutionContext);
            this.longLivedExecutorService = Objects.requireNonNull(longLivedExecutorService);
        }

        /**
         * Sets the native-specification provider for this builder.
         *
         * <p>By default, an empty-specification provider will be used that always returns an empty string. The default
         * for this builder can be restored by passing null as argument.
         *
         * @param nativeSpecificationProvider native specification provider
         * @return this builder
         */
        public Builder setNativeSpecificationProvider(NativeSpecificationProvider nativeSpecificationProvider) {
            this.nativeSpecificationProvider = nativeSpecificationProvider;
            return this;
        }

        /**
         * Sets the instance provider for this builder.
         *
         * <p>By default, a {@link SimpleInstanceProvider} will be used (using the short-lived {@link ExecutionContext}
         * passed to {@link #Builder(Session, Path, CommandProvider, ExecutionContext, ScheduledExecutorService)}).
         *
         * @param instanceProvider instance provider
         * @return this builder
         */
        public Builder setInstanceProvider(InstanceProvider instanceProvider) {
            this.instanceProvider = instanceProvider;
            return this;
        }

        /**
         * Sets the timeout (in seconds) that will be passed to {@link Session#wait(String, long)}.
         *
         * <p>By default, if this method is not called, the timeout will be one minute.
         *
         * @param waitTimeoutSeconds timeout (in seconds) when waiting for finished DRMAA jobs
         * @return this builder
         */
        public Builder setWaitTimeoutSeconds(long waitTimeoutSeconds) {
            this.waitTimeoutSeconds = waitTimeoutSeconds;
            return this;
        }

        /**
         * Sets the delay (in milliseconds) before calling {@link Session#wait(String, long)} again after a failed
         * attempt.
         *
         * <p>By default, if this method is not called, the delay will be 10 seconds.
         *
         * @param errorWaitMilliseconds delay (in milliseconds)
         * @return this builder
         */
        public Builder setErrorWaitMilliseconds(long errorWaitMilliseconds) {
            this.errorWaitMilliseconds = errorWaitMilliseconds;
            return this;
        }

        public DrmaaSimpleModuleExecutor build() {
            NativeSpecificationProvider actualNativeProvider = nativeSpecificationProvider == null
                ? EmptyNativeSpecificationProvider.INSTANCE
                : nativeSpecificationProvider;
            InstanceProvider actualInstanceProvider = instanceProvider == null
                ? new SimpleInstanceProvider.Builder(shortLivedExecutionContext).build()
                : instanceProvider;
            return new DrmaaSimpleModuleExecutor(session, jobIOBasePath, commandProvider, actualNativeProvider,
                actualInstanceProvider, waitTimeoutSeconds, errorWaitMilliseconds, shortLivedExecutionContext,
                longLivedExecutorService);
        }
    }

    static final class SubmittedJob {
        private final RuntimeAnnotatedExecutionTrace trace;
        private final String drmaaId;

        SubmittedJob(RuntimeAnnotatedExecutionTrace trace, String drmaaId) {
            this.trace = trace;
            this.drmaaId = drmaaId;
        }

        @Override
        public String toString() {
            return String.format("submitted DRMAA job '%s' (trace: '%s')", drmaaId, trace);
        }
    }

    static final class FinishedJob {
        private final SubmittedJob submittedJob;
        private final Option<Integer> exitStatus;
        private final Option<String> terminatingSignal;
        private final boolean aborted;

        private FinishedJob(SubmittedJob submittedJob, Option<Integer> exitStatus, Option<String> terminatingSignal,
                boolean aborted) {
            this.submittedJob = submittedJob;
            this.exitStatus = exitStatus;
            this.terminatingSignal = terminatingSignal;
            this.aborted = aborted;
        }

        private static <T> String optionToString(Option<T> option) {
            return option.isDefined()
                ? option.get().toString()
                : "-";
        }

        @Override
        public String toString() {
            return String.format(
                "finished DRMAA job '%s' (exit status: %s, terminating signal: %s, aborted: %s, trace: '%s')",
                submittedJob.drmaaId, optionToString(exitStatus), optionToString(terminatingSignal), aborted,
                submittedJob.trace
            );
        }
    }

    static final class AwaitedJob {
        private final SubmittedJob submittedJob;
        private final Promise<FinishedJob> promise;

        AwaitedJob(SubmittedJob submittedJob, Promise<FinishedJob> promise) {
            this.submittedJob = submittedJob;
            this.promise = promise;
        }
    }

    private static Path stdinPath(Path ioPath) {
        return ioPath.resolve("stdin");
    }

    private static Path stdoutPath(Path ioPath) {
        return ioPath.resolve("stdout");
    }

    private static Path stderrPath(Path ioPath) {
        return ioPath.resolve("stderr");
    }

    static String getJobName(String moduleDeclarationName) {
        String normlized = Normalizer.normalize(moduleDeclarationName.replace('.', '_'), Normalizer.Form.NFD);
        return NON_JOB_NAME_CHARACTERS.matcher(normlized).replaceAll("");
    }

    private static final class Timing {
        private final long submissionTimeMillis;
        private volatile long drmaaSubmissionTimeMillis = 0;
        @Nullable private volatile Path ioPath = null;
        @Nullable private volatile ImmutableList<String> commandLine = null;
        @Nullable private volatile String nativeArguments = null;
        @Nullable private volatile SubmittedJob submittedJob = null;
        @Nullable private volatile FinishedJob finishedJob = null;

        private Timing(long submissionTimeMillis) {
            this.submissionTimeMillis = submissionTimeMillis;
        }
    }

    private final class WaitForJobMapper extends Mapper<SubmittedJob, Future<FinishedJob>> {
        @Override
        public Future<FinishedJob> apply(SubmittedJob submittedJob) {
            Promise<FinishedJob> promise = Futures.promise();
            synchronized (awaitedJobMap) {
                awaitedJobMap.put(submittedJob.drmaaId, new AwaitedJob(submittedJob, promise));
                if (!waitTaskIsRunning) {
                    longLivedExecutorService.execute(waitForNextJobTask);
                    waitTaskIsRunning = true;
                }
            }
            return promise.future();
        }
    }

    private static SimpleModuleExecutorResult.Builder resultBuilder(Timing timing) {
        SimpleModuleExecutorResult.Builder resultBuilder
                = new SimpleModuleExecutorResult.Builder(Name.qualifiedName(DrmaaSimpleModuleExecutor.class.getName()))
            .addProperty(SUBMISSION_TIME_MILLIS, timing.submissionTimeMillis);
        @Nullable ImmutableList<String> localCommandLine = timing.commandLine;
        if (localCommandLine != null) {
            resultBuilder.addProperty(COMMAND_LINE, CommandLines.escape(localCommandLine));
        }
        @Nullable String localNativeArguments = timing.nativeArguments;
        if (localNativeArguments != null) {
            resultBuilder.addProperty(NATIVE_ARGUMENTS, localNativeArguments);
        }
        long localDrmaaSubmissionTimeMillis = timing.drmaaSubmissionTimeMillis;
        if (localDrmaaSubmissionTimeMillis != 0) {
            resultBuilder.addProperty(DRMAA_SUBMISSION_TIME_MILLIS, localDrmaaSubmissionTimeMillis);
        }
        @Nullable SubmittedJob localSubmittedJob = timing.submittedJob;
        if (localSubmittedJob != null) {
            resultBuilder.addProperty(JOB_ID, localSubmittedJob.drmaaId);
        }
        @Nullable FinishedJob localFinishedJob = timing.finishedJob;
        if (localFinishedJob != null && localFinishedJob.exitStatus.isDefined()) {
            resultBuilder.addProperty(EXIT_VALUE, (long) localFinishedJob.exitStatus.get());
        }
        return resultBuilder;
    }

    private final class FinishedJobMapper extends Mapper<FinishedJob, SimpleModuleExecutorResult> {
        private final Timing timing;

        private FinishedJobMapper(Timing timing) {
            this.timing = timing;
        }

        private void logOutput(Path outputFile, String outputDescription, String drmaaId) {
            if (Files.exists(outputFile)) {
                boolean empty = true;
                try (BufferedReader reader = Files.newBufferedReader(outputFile, StandardCharsets.UTF_8)) {
                    @Nullable String line = reader.readLine();
                    while (line != null) {
                        if (empty && !line.isEmpty()) {
                            empty = false;
                            log.info("Content of {} of DRMAA job '{}' follows.", outputDescription, drmaaId);
                            log.info("--8<--");
                        }
                        log.info(line);
                        line = reader.readLine();
                    }
                    if (!empty) {
                        log.info("-->8--");
                        log.info("End of content of {} of DRMAA job '{}'.", outputDescription, drmaaId);
                    }
                } catch (IOException exception) {
                    log.warn(String.format(
                        "Exception while logging the content of %s of DRMAA job '%s' (path: %s).",
                        outputDescription, drmaaId, outputFile
                    ), exception);
                }
            }
        }

        @Override
        public SimpleModuleExecutorResult checkedApply(FinishedJob finishedJob)
                throws IOException, ClassNotFoundException, ExecutionException {
            timing.finishedJob = finishedJob;
            log.debug("{}", finishedJob);
            @Nullable Path ioPath = timing.ioPath;
            assert ioPath != null : "set to non-null value before job was started";
            logOutput(stderrPath(ioPath), "stderr", finishedJob.submittedJob.drmaaId);
            SimpleModuleExecutorResult.Builder resultBuilder = resultBuilder(timing);
            if (finishedJob.exitStatus.isDefined() && finishedJob.exitStatus.get() == 0) {
                try (Splitter<SimpleModuleExecutorResult> splitter = new Splitter<>(
                        SimpleModuleExecutorResult.class, Files.newBufferedReader(stdoutPath(ioPath)))) {
                    splitter.consumeAll();
                    resultBuilder.addExecutionResult(splitter.getResult());
                }
            } else {
                logOutput(stdoutPath(ioPath), "stdout", finishedJob.submittedJob.drmaaId);
                resultBuilder.setException(new ExecutionException(String.format(
                    "DRMAA job '%s' failed (exit status %s, terminating signal %s)",
                    finishedJob.submittedJob.drmaaId, finishedJob.exitStatus, finishedJob.terminatingSignal
                )));
            }
            return resultBuilder
                .addProperty(COMPLETION_TIME_MILLIS, System.currentTimeMillis())
                .build();
        }
    }

    private static final class RecoverWithTiming extends Recover<SimpleModuleExecutorResult> {
        private final Timing timing;

        private RecoverWithTiming(Timing timing) {
            this.timing = timing;
        }

        @Override
        public SimpleModuleExecutorResult recover(Throwable throwable) {
            return resultBuilder(timing)
                .setException(new ExecutionException(
                    "Exception while trying to execute simple module using DRMAA.", throwable
                ))
                .addProperty(COMPLETION_TIME_MILLIS, System.currentTimeMillis())
                .build();
        }
    }

    private final class Cleaner extends OnComplete<SimpleModuleExecutorResult> {
        private final Timing timing;

        private Cleaner(Timing timing) {
            this.timing = timing;
        }

        @Override
        public void onComplete(Throwable throwable, SimpleModuleExecutorResult result) {
            try {
                @Nullable Path ioPath = timing.ioPath;
                if (ioPath != null) {
                    Files.walkFileTree(ioPath, RecursiveDeleteVisitor.getInstance());
                }
            } catch (IOException exception) {
                log.warn(String.format("Ignoring I/O exception while trying to clean '%s'.", timing.ioPath), exception);
            }
        }
    }

    private SubmittedJob newSubmittedJob(RuntimeStateProvider runtimeStateProvider,
            RuntimeContext runtimeContext, Timing timing)
            throws IOException, DrmaaException, InstanceProvisionException, LinkerException {
        RuntimeAnnotatedExecutionTrace executionTrace = runtimeStateProvider.provideExecutionTrace(runtimeContext);
        RuntimeProxyModule module = (RuntimeProxyModule) executionTrace.getModule();
        String declarationName = module.getDeclaration().getQualifiedName().toString();
        Path ioPath = Files.createTempDirectory(jobIOBasePath, declarationName);
        timing.ioPath = ioPath;
        try (ObjectOutputStream objectOutputStream
                = new ObjectOutputStream(Files.newOutputStream(stdinPath(ioPath)))) {
            objectOutputStream.writeObject(runtimeStateProvider);
        }
        JobTemplate jobTemplate = drmaaSession.createJobTemplate();
        try {
            jobTemplate.setJobName(getJobName(declarationName));
            ImmutableList<String> command = ImmutableList.copyOf(commandProvider.getCommand(executionTrace));
            timing.commandLine = command;
            jobTemplate.setRemoteCommand(command.get(0));
            jobTemplate.setArgs(command.subList(1, command.size()));
            jobTemplate.setTransferFiles(TRANSFER_FILES);
            jobTemplate.setInputPath(":" + stdinPath(ioPath));
            jobTemplate.setOutputPath(":" + stdoutPath(ioPath));
            jobTemplate.setErrorPath(":" + stderrPath(ioPath));
            String nativeSpecification = nativeSpecificationProvider.getNativeSpecification(executionTrace);
            timing.nativeArguments = nativeSpecification;
            jobTemplate.setNativeSpecification(nativeSpecification);
            timing.drmaaSubmissionTimeMillis = System.currentTimeMillis();
            SubmittedJob submittedJob = new SubmittedJob(executionTrace, drmaaSession.runJob(jobTemplate));
            timing.submittedJob = submittedJob;
            log.debug("{}, command: {}, native: {}", submittedJob, command, nativeSpecification);
            return submittedJob;
        } finally {
            drmaaSession.deleteJobTemplate(jobTemplate);
        }
    }

    @Override
    public Future<SimpleModuleExecutorResult> submit(RuntimeStateProvider runtimeStateProvider,
            @Nullable Future<String> cancellationFuture) {
        Timing timing = new Timing(System.currentTimeMillis());
        return runtimeStateProvider
            .flatMapRuntimeContext(instanceProvider, runtimeContext -> {
                Future<SimpleModuleExecutorResult> future = Futures
                    .future(
                        () -> newSubmittedJob(runtimeStateProvider, runtimeContext, timing),
                        shortLivedExecutionContext
                    )
                    .flatMap(waitForJobMapper, shortLivedExecutionContext)
                    .map(new FinishedJobMapper(timing), shortLivedExecutionContext);
                future.onComplete(new Cleaner(timing), shortLivedExecutionContext);
                return future;
            }, shortLivedExecutionContext)
            .recover(new RecoverWithTiming(timing), shortLivedExecutionContext);
    }

    enum TaskState {
        BEFORE_WAIT,
        AFTER_WAIT,
        FINISHED
    }

    private final class WaitForNextJobTask implements Runnable {
        @Override
        public void run() {
            TaskState taskState = TaskState.BEFORE_WAIT;
            try {
                JobInfo jobInfo = drmaaSession.wait(Session.JOB_IDS_SESSION_ANY, waitTimeoutSeconds);
                taskState = TaskState.AFTER_WAIT;
                String jobId = jobInfo.getJobId();
                @Nullable AwaitedJob awaitedJob;
                synchronized (awaitedJobMap) {
                    awaitedJob = awaitedJobMap.remove(jobId);
                }
                if (awaitedJob == null) {
                    log.warn(String.format("Ignoring event that unknown DRMAA job '%s' finished.", jobId));
                } else {
                    Option<Integer> exitStatus = jobInfo.hasExited()
                        ? Option.some(jobInfo.getExitStatus())
                        : Option.<Integer>none();
                    Option<String> terminatingSignal = jobInfo.hasSignaled()
                        ? Option.some(jobInfo.getTerminatingSignal())
                        : Option.<String>none();
                    awaitedJob.promise.success(new FinishedJob(
                        awaitedJob.submittedJob, exitStatus, terminatingSignal, jobInfo.wasAborted()));
                }
                taskState = TaskState.FINISHED;
            } catch (ExitTimeoutException ignored) {
                taskState = TaskState.FINISHED;
            } catch (DrmaaException exception) {
                log.warn(String.format(
                    "A DRMAA exception occurred in a task that waited for a DRMAA job to finish. A new wait task will "
                        + "be started with a delay of %d ms. State: %s", errorWaitMilliseconds, taskState
                ), exception);
            } catch (RuntimeException exception) {
                log.error(String.format(
                    "An unexpected runtime exception occurred in a task that waited for a DRMAA job to finish. "
                        + "A new wait task will be started with a delay of %d ms, but there is likely a deeper "
                        + "problem. State: %s", errorWaitMilliseconds, taskState
                ), exception);
            } finally {
                synchronized (awaitedJobMap) {
                    assert waitTaskIsRunning;
                    if (awaitedJobMap.isEmpty()) {
                        waitTaskIsRunning = false;
                    } else {
                        if (taskState == TaskState.FINISHED) {
                            longLivedExecutorService.execute(this);
                        } else {
                            longLivedExecutorService.schedule(this, errorWaitMilliseconds, TimeUnit.MILLISECONDS);
                        }
                    }
                }
            }
        }
    }
}
