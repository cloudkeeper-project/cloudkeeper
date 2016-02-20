package xyz.cloudkeeper.executors;

import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.simple.CharacterStreamCommunication;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * This class consists of static methods for executing a simple-module encoded in an input stream and writing the result
 * to an output stream.
 */
public final class ForkedExecutors {
    private ForkedExecutors() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Deserializes a {@link RuntimeStateProvider} instance from the given input stream, runs the encoded simple module,
     * and then serializes a {@link SimpleModuleExecutorResult} instance to the given output stream.
     *
     * <p>Any execution exception is always transformed into an "immunized" execution exception (with
     * {@link ExecutionException#toImmunizedException()}), which does not contain (transitive) references to any class
     * that may not be available in the JVM that deserializes the {@link SimpleModuleExecutorResult}. Yet, an
     * "immunized" exception is indistinguishable in printed stack traces.
     *
     * <p>The output {@link SimpleModuleExecutorResult} is written to the output stream using
     * {@link CharacterStreamCommunication#writeObject(Serializable, Appendable, String)}. Accordingly, it is acceptable
     * for {@link SimpleModuleExecutor#submit(RuntimeStateProvider)} to also write to the same {@link OutputStream}.
     *
     * <p>Note that this method returns only after execution of the simple module has finished.
     *
     * @param inputStream the input stream, will not be closed
     * @param outputStream the output stream, will not be closed
     * @throws IOException if an I/O error occurs
     */
    public static void run(SimpleModuleExecutor simpleModuleExecutor, InputStream inputStream,
            OutputStream outputStream) throws IOException {
        String boundary = UUID.randomUUID().toString();
        try (PrintWriter printWriter
                = new PrintWriter(new OutputStreamWriter(new NonClosingOutputStream(outputStream)))) {
            CharacterStreamCommunication.writeBoundary(boundary, printWriter);
        }

        SimpleModuleExecutorResult.Builder resultBuilder
            = new SimpleModuleExecutorResult.Builder(Name.qualifiedName(ForkedExecutors.class.getName()))
                .addProperty(SimpleModuleExecutor.SUBMISSION_TIME_MILLIS, System.currentTimeMillis());
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new NonClosingInputStream(inputStream))) {
            RuntimeStateProvider runtimeStateProvider = (RuntimeStateProvider) objectInputStream.readObject();

            SimpleModuleExecutorResult executorResult = simpleModuleExecutor.submit(runtimeStateProvider).get();
            @Nullable ExecutionException exception = executorResult.getExecutionException();
            resultBuilder.addExecutionResult(executorResult);
            if (exception != null) {
                resultBuilder.setException(exception.toImmunizedException());
            }
        } catch (IOException | ClassNotFoundException exception) {
            resultBuilder.setException(
                new ExecutionException("Forked simple-module executor failed to read and deserialize input.", exception)
            );
        } catch (InterruptedException | java.util.concurrent.ExecutionException exception) {
            ExecutionException executionException = new ExecutionException(
                "Unexpected exception in forked simple-module executor.", exception);
            resultBuilder.setException(executionException.toImmunizedException());
        }
        resultBuilder.addProperty(SimpleModuleExecutor.COMPLETION_TIME_MILLIS, System.currentTimeMillis());

        try (PrintWriter printWriter
                = new PrintWriter(new OutputStreamWriter(new NonClosingOutputStream(outputStream)))) {
            CharacterStreamCommunication.writeObject(resultBuilder.build(), printWriter, boundary);
        }
    }

    /**
     * Wrapper around some other already existing input stream (the <i>underlying</i> input stream), which it uses as
     * its basic source of data.
     *
     * <p>This class never closes the underlying input stream. It is intended for wrapping already existing input
     * streams that are guaranteed to be closed elsewhere. Example use:
     *
     * <p>{@code
     * try (ObjectInputStream objectInputStream = new ObjectInputStream(new NonClosingInputStream(existingStream))) {
     *     // ...
     * }
     * }
     */
    private static final class NonClosingInputStream extends FilterInputStream {
        private NonClosingInputStream(InputStream in) {
            super(in);
        }

        /**
         * According to the name of this class, this method does nothing.
         */
        @Override
        public void close() { }
    }

    /**
     * Wrapper around some other already existing output stream (the <i>underlying</i> output stream), which it uses as
     * its basic sink of data.
     *
     * <p>This class never closes the underlying output stream. It is intended for wrapping already existing output
     * streams that are guaranteed to be closed elsewhere. Example use:
     *
     * <p>{@code
     * try (ObjectOutputStream objectOutputStream
     *         = new ObjectOutputStream(new NonClosingOutputStream(existingStream))) {
     *     // ...
     * }
     * }
     */
    private static final class NonClosingOutputStream extends FilterOutputStream {
        private NonClosingOutputStream(OutputStream out) {
            super(out);
        }

        /**
         * According to the name of this class, this method only flushes the underlying output stream, but does not
         * close it.
         *
         * @throws IOException if the call to {@link #flush()} fails
         */
        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
