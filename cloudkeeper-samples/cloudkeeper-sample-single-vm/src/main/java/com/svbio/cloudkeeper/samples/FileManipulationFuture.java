package com.svbio.cloudkeeper.samples;

import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.dsl.CompositeModule;
import com.svbio.cloudkeeper.dsl.CompositeModulePlugin;
import com.svbio.cloudkeeper.dsl.ExcludedSuperTypes;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.dsl.SimpleModulePlugin;
import com.svbio.cloudkeeper.dsl.TypePlugin;
import com.svbio.cloudkeeper.model.CloudKeeperSerialization;
import com.svbio.cloudkeeper.model.api.CloudKeeperEnvironment;
import com.svbio.cloudkeeper.model.api.WorkflowExecution;
import com.svbio.cloudkeeper.model.util.ByteSequences;
import com.svbio.cloudkeeper.simple.AwaitException;
import com.svbio.cloudkeeper.simple.WorkflowExecutions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asynchronous task that prepends each line of a file with a prefix.
 *
 * <p>Under the hood, this class implements the asynchronous task as a CloudKeeper workflow. This class is thread-safe.
 */
@TypePlugin("Asynchronous task that prepends each line of a file with a prefix")
@ExcludedSuperTypes(Future.class)
public final class FileManipulationFuture implements Future<FileManipulationResult> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final WorkflowExecution execution;
    private final FileManipulationModule module;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public FileManipulationFuture(CloudKeeperEnvironment cloudKeeperEnvironment, Path inputFile, String prefix) {
        module = ModuleFactory.getDefault().create(FileManipulationModule.class)
            .inPrefix().fromValue(prefix)
            .inText().fromValue(new PlainText(ByteSequences.fileBacked(inputFile)));
        execution = module.newPreconfiguredWorkflowExecutionBuilder(cloudKeeperEnvironment).start();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return cancelled.compareAndSet(false, true) && execution.cancel();
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        return !execution.isRunning();
    }

    @Override
    public FileManipulationResult get() throws InterruptedException, ExecutionException {
        int days = 0;
        do {
            try {
                return get(1, TimeUnit.DAYS);
            } catch (TimeoutException ignored) {
                ++days;
                log.info(String.format(
                    "Exceptionally long wait of %d day(s) in %s#get().", days, getClass().getSimpleName()
                ));
            }
        } while (true);
    }

    @Override
    public FileManipulationResult get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {

        try {
            ByteSequence byteSequence = WorkflowExecutions.getOutputValue(execution, module.outText(), timeout, unit);
            int numberOfLines = WorkflowExecutions.getOutputValue(execution, module.outNumLines(), timeout, unit);
            long sizeOfFile = WorkflowExecutions.getOutputValue(execution, module.outTextSize(), timeout, unit);
            return new FileManipulationResult(byteSequence, numberOfLines, sizeOfFile);
        } catch (AwaitException exception) {
            throw new ExecutionException(exception);
        }
    }

    @CloudKeeperSerialization(ByteSequenceMarshaler.class)
    @TypePlugin("Text file")
    public static class PlainText implements ByteSequence {
        private final ByteSequence byteSequence;

        public PlainText(ByteSequence byteSequence) {
            this.byteSequence = byteSequence;
        }

        private enum PlainTextDecorator implements ByteSequenceMarshaler.Decorator {
            INSTANCE;

            @Override
            public ByteSequence decorate(ByteSequence byteSequence) {
                return new PlainText(byteSequence);
            }
        }

        @Override
        public ByteSequenceMarshaler.Decorator getDecorator() {
            return PlainTextDecorator.INSTANCE;
        }

        @Override
        public URI getURI() {
            return byteSequence.getURI();
        }

        @Override
        public boolean isSelfContained() {
            return byteSequence.isSelfContained();
        }

        @Override
        public long getContentLength() throws IOException {
            return byteSequence.getContentLength();
        }

        @Override
        public String getContentType() {
            return "text/plain; charset=UTF-8";
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return byteSequence.newInputStream();
        }
    }

    @SimpleModulePlugin("Prepends every line in the input file with the given prefix.")
    public abstract static class PrependModule extends SimpleModule<PrependModule> {
        public abstract InPort<PlainText> inText();
        public abstract InPort<String> inPrefix();
        public abstract OutPort<Integer> outNumLines();
        public abstract OutPort<PlainText> outText();

        @Override
        public void run() throws IOException {
            String prefix = inPrefix().get();

            // We create a temporary file in our working directory. The CloudKeeper environment ensures proper clean-up.
            Path tempFile = Files.createTempFile(getWorkingDirectory(), "prependedFile", "txt");
            int count = 0;
            try (
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inText().get().newInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)
            ) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    ++count;
                    writer.write(prefix + line + '\n');
                }
            }
            outNumLines().set(count);
            outText().set(new PlainText(ByteSequences.fileBacked(tempFile)));
        }
    }

    @SimpleModulePlugin("Determines the size of a file.")
    public abstract static class SizeModule extends SimpleModule<SizeModule> {
        public abstract InPort<ByteSequence> inFile();
        public abstract OutPort<Long> outSize();

        @Override
        public void run() throws IOException {
            outSize().set(inFile().get().getContentLength());
        }
    }

    @CompositeModulePlugin(
        "Prepends each line of a text file with a prefix and returns several statistics for the new file."
    )
    public abstract static class FileManipulationModule extends CompositeModule<FileManipulationModule> {
        public abstract InPort<PlainText> inText();
        public abstract InPort<String> inPrefix();
        public abstract OutPort<PlainText> outText();
        public abstract OutPort<Integer> outNumLines();
        public abstract OutPort<Long> outTextSize();

        private final PrependModule prependModule = child(PrependModule.class)
            .inText().from(inText())
            .inPrefix().from(inPrefix());
        private final SizeModule sizeModule = child(SizeModule.class)
            .inFile().from(prependModule.outText());

        { outText().from(prependModule.outText()); }
        { outNumLines().from(prependModule.outNumLines()); }
        { outTextSize().from(sizeModule.outSize()); }
    }
}
