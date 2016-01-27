package com.svbio.cloudkeeper.executors;

import akka.japi.Option;
import com.svbio.cloudkeeper.filesystem.FileStagingArea;
import com.svbio.cloudkeeper.linker.Linker;
import com.svbio.cloudkeeper.model.api.ExecutionException;
import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import com.svbio.cloudkeeper.simple.CharacterStreamCommunication;
import com.svbio.cloudkeeper.simple.LocalSimpleModuleExecutor;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public final class DummyProcess {
    public static final String EXECUTION_EXCEPTION_MSG = "All is good. This is an expected exception.";

    private DummyProcess() { }

    /**
     * Returns the command that can be passed to {@link ProcessBuilder} in order to run {@link #main(String[])} in a
     * fresh JVM.
     */
    public static ImmutableList<String> command(String... additionalArguments) {
        return JVMs.command(
            DummyProcess.class,
            Arrays.asList(
                DummyProcess.class,
                BareModule.class, // cloudkeeper-model
                FileStagingArea.Builder.class, // cloudkeeper-file-staging
                LocalSimpleModuleExecutor.class, // cloudkeeper-basic
                StagingAreaProvider.class, // cloudkeeper-api
                Linker.class, // cloudkeeper-linker
                Option.class, // akka-actor
                Future.class, // scala-library
                LoggerFactory.class // slf4j-api
            ),
            additionalArguments
        );
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.printf("[%s, %s]: ", DummyProcess.class.getName(), new Date());
            throwable.printStackTrace(System.err);
        });

        try (ObjectInputStream inputStream = new ObjectInputStream(System.in)) {
            String boundary = UUID.randomUUID().toString();
            CharacterStreamCommunication.writeBoundary(boundary, System.out);
            RuntimeStateProvider runtimeStateProvider = (RuntimeStateProvider) inputStream.readObject();

            Objects.requireNonNull(runtimeStateProvider, "Expected: runtimeStateProvider != null");

            long currentTimeMillis = System.currentTimeMillis();
            System.out.printf("[%s, %s]: %s\n", DummyProcess.class.getName(), new Date(), EXECUTION_EXCEPTION_MSG);
            CharacterStreamCommunication.writeObject(
                new SimpleModuleExecutorResult.Builder(Name.qualifiedName(DummyProcess.class.getName()))
                    .setException(new ExecutionException(EXECUTION_EXCEPTION_MSG))
                    .addProperty(SimpleModuleExecutor.SUBMISSION_TIME_MILLIS, currentTimeMillis - 1000)
                    .addProperty(SimpleModuleExecutor.COMPLETION_TIME_MILLIS, currentTimeMillis)
                    .build(),
                System.out,
                boundary
            );
        }
    }
}
