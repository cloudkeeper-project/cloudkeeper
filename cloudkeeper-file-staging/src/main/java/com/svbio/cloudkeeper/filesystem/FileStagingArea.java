package com.svbio.cloudkeeper.filesystem;

import akka.japi.Option;
import cloudkeeper.types.ByteSequence;
import com.svbio.cloudkeeper.model.api.MarshalContext;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider;
import com.svbio.cloudkeeper.model.api.staging.StagingException;
import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import com.svbio.cloudkeeper.model.immutable.element.Index;
import com.svbio.cloudkeeper.model.immutable.element.Key;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeExecutionTraceVisitor;
import com.svbio.cloudkeeper.model.util.ByteSequences;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import com.svbio.cloudkeeper.staging.ExternalStagingArea;
import com.svbio.cloudkeeper.staging.MutableObjectMetadata;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

/**
 * File-based staging area.
 */
public final class FileStagingArea extends ExternalStagingArea {
    private static final String CONTENT_DIRECTORY = "content";
    private static final String INPUT_DIRECTORY = "input";
    private static final String OUTPUT_DIRECTORY = "output";
    private static final String METADATA_SUFFIX = ".meta.xml";

    /**
     * Monitor (mutex) for accessing parts of the file system that are potentially touched by multiple
     * <em>consistent</em> calls to {@link com.svbio.cloudkeeper.model.api.staging.StagingArea} methods.
     *
     * <p>See the {@link com.svbio.cloudkeeper.model.api.staging.StagingArea} for a definition of <em>consistent</em>.
     * Also note that the contract for staging areas is relatively weak, so using this field as mutex is enough -- in
     * particular, no synchronization across JVMs is necessary.
     */
    private final Object monitor;
    private final JAXBContext jaxbContext;
    private final Path basePath;
    private final ImmutableList<Path> hardLinkEnabledPaths;

    private FileStagingArea(RuntimeAnnotatedExecutionTrace executionTrace, RuntimeContext runtimeContext,
            ExecutionContext executionContext, Object monitor, JAXBContext jaxbContext, Path basePath,
            ImmutableList<Path> hardLinkEnabledPaths) {
        super(executionTrace, runtimeContext, executionContext);
        this.monitor = monitor;
        this.jaxbContext = jaxbContext;
        this.basePath = basePath;
        this.hardLinkEnabledPaths = hardLinkEnabledPaths;
    }

    private static final class TraceElementVisitor implements RuntimeExecutionTraceVisitor<Path, Path> {
        private static final TraceElementVisitor INSTANCE = new TraceElementVisitor();

        @Override
        public Path visitModule(RuntimeExecutionTrace module, @Nullable Path basePath) {
            assert basePath != null;
            return basePath.resolve(module.getSimpleName().toString());
        }

        @Override
        public Path visitContent(RuntimeExecutionTrace content, @Nullable Path basePath) {
            assert basePath != null;
            return basePath.resolve(CONTENT_DIRECTORY);
        }

        @Override
        public Path visitIteration(RuntimeExecutionTrace iteration, @Nullable Path basePath) {
            assert basePath != null;
            return basePath.resolve(iteration.getIndex().toString());
        }

        @Override
        public Path visitInPort(RuntimeExecutionTrace inPort, @Nullable Path basePath) {
            assert basePath != null;
            return basePath.resolve(INPUT_DIRECTORY).resolve(inPort.getSimpleName().toString());
        }

        @Override
        public Path visitOutPort(RuntimeExecutionTrace outPort, @Nullable Path basePath) {
            assert basePath != null;
            return basePath.resolve(OUTPUT_DIRECTORY).resolve(outPort.getSimpleName().toString());
        }

        @Override
        public Path visitArrayIndex(RuntimeExecutionTrace index, @Nullable Path basePath) {
            assert basePath != null;
            return basePath.resolve(index.getIndex().toString());
        }
    }

    /**
     * Returns the path of the file/directory that corresponds to the given execution trace.
     *
     * @param trace execution trace, must be of type {@link RuntimeExecutionTrace.Type#IN_PORT},
     *     {@link RuntimeExecutionTrace.Type#OUT_PORT}, or {@link RuntimeExecutionTrace.Type#ARRAY_INDEX}
     * @return the path
     */
    private Path toPath(RuntimeExecutionTrace trace) {
        @Nullable Path currentPath = basePath;
        for (RuntimeExecutionTrace element: trace.asElementList()) {
            currentPath = element.accept(TraceElementVisitor.INSTANCE, currentPath);
        }
        assert currentPath != null;
        return currentPath;
    }

    private static void deleteEmptyNestedDirectories(Path directory, Path baseDirectory) throws IOException {
        Path currentPath = directory;

        // We want be on the safe side when deleting files/directories. Thus, a necessary requirement for deleting
        // is that we are still within the baseDirectory.
        while (currentPath.startsWith(baseDirectory) && !currentPath.equals(baseDirectory)) {
            if (Files.exists(currentPath)) {
                if (Files.newDirectoryStream(currentPath).iterator().hasNext()) {
                    break;
                }
                Files.delete(currentPath);
            }
            currentPath = currentPath.getParent();
        }
    }

    @Override
    protected void delete(RuntimeExecutionTrace prefix, RuntimeAnnotatedExecutionTrace absolutePrefix)
            throws IOException {
        Path tracePath = toPath(prefix);
        Path traceBasePath = tracePath.getParent();
        if (!prefix.getReference().isEmpty()) {
            Files.deleteIfExists(metadataPath(tracePath));
        }

        synchronized (monitor) {
            // See JavaDoc for monitor
            Files.walkFileTree(tracePath, RecursiveDeleteVisitor.getInstance());
            deleteEmptyNestedDirectories(traceBasePath, basePath);
        }
    }

    private static final class HardLinkVisitor extends SimpleFileVisitor<Path> {
        private Path currentPath;
        private int nestingLevel = 0;

        private HardLinkVisitor(Path currentPath) {
            this.currentPath = currentPath;
        }

        Path target(Path source) {
            return nestingLevel == 0
                ? currentPath
                : currentPath.resolve(source.getFileName());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            currentPath = Files.createDirectories(target(dir));
            ++nestingLevel;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.createLink(target(file), file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, @Nullable IOException exception) throws IOException {
            currentPath = currentPath.getParent();
            --nestingLevel;
            assert nestingLevel >= 0;
            if (exception != null) {
                throw exception;
            }
            return FileVisitResult.CONTINUE;
        }
    }

    @Override
    protected void copy(RuntimeExecutionTrace source, RuntimeExecutionTrace target,
            RuntimeAnnotatedExecutionTrace absoluteSource, RuntimeAnnotatedExecutionTrace absoluteTarget)
            throws IOException {
        Path targetPath = toPath(target);
        synchronized (monitor) {
            // See JavaDoc for monitor
            Files.createDirectories(targetPath.getParent());
        }
        Files.walkFileTree(toPath(source), new HardLinkVisitor(targetPath));
        Files.createLink(
            metadataPath(targetPath),
            metadataPath(toPath(source))
        );
    }

    @Override
    protected boolean exists(RuntimeExecutionTrace source, RuntimeAnnotatedExecutionTrace absoluteSource) {
        Path tracePath = toPath(source);
        return Files.exists(metadataPath(tracePath));
    }

    @Override
    protected Option<Index> getMaximumIndex(RuntimeExecutionTrace trace, RuntimeAnnotatedExecutionTrace absoluteTrace,
            @Nullable Index upperBound) throws IOException {
        int upperBoundInt = upperBound == null
            ? Integer.MAX_VALUE
            : upperBound.intValue();
        int maximumIndex = -1;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(toPath(trace))) {
            for (Path path: stream) {
                int index = Index.parseIndex(path.getFileName().toString());
                if (index > maximumIndex) {
                    if (index <= upperBoundInt) {
                        maximumIndex = index;
                        if (index == upperBoundInt) {
                            // Optimization: If we found the upper bound, there will not be a greater index,
                            // so we can stop here.
                            break;
                        }
                    }
                }
            }
        }
        return maximumIndex >= 0
            ? Option.some(Index.index(maximumIndex))
            : Option.<Index>none();
    }

    @Override
    protected FileStagingArea resolveDescendant(RuntimeExecutionTrace trace,
            RuntimeAnnotatedExecutionTrace absoluteTrace) {
        return new FileStagingArea(absoluteTrace, getRuntimeContext(), getExecutionContext(), monitor,
            jaxbContext, toPath(trace), hardLinkEnabledPaths);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In order for the provider returned by {@link ExternalStagingArea#getStagingAreaProvider()} to be capable of
     * reconstructing a file-based staging area in a separate JVM, the instance provider passed to
     * {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, com.svbio.cloudkeeper.model.api.staging.InstanceProvider)}
     * needs to be able to provide instances of the following classes:
     * <ul><li>
     *     {@link ExecutionContext}: The execution context will be used to execute the futures created
     *     by the staging area.
     * </li></ul>
     */
    @Override
    public StagingAreaProvider getStagingAreaProvider() {
        requireValidRequestForProvider();
        return new StagingAreaProviderImpl(basePath, hardLinkEnabledPaths);
    }

    @Override
    public ReadContext newReadContext(RuntimeExecutionTrace source) {
        return new ReadContextImpl(toPath(source));
    }

    @Override
    public WriteContext newWriteContext(RuntimeExecutionTrace target) {
        return new WriteContextImpl(toPath(target));
    }

    private final class ReadContextImpl implements ReadContext {
        private final Path path;

        private ReadContextImpl(Path path) {
            this.path = path;
        }

        @Override
        public MutableObjectMetadata getMetadata() throws IOException {
            try {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                return (MutableObjectMetadata) unmarshaller.unmarshal(metadataPath(path).toFile());
            } catch (JAXBException exception) {
                throw new StagingException(String.format(
                    "Failed to unmarshal object metadata from path '%s'.", metadataPath(path)
                ), exception);
            }
        }

        @Override
        public ByteSequence getByteSequence(Key key) throws IOException {
            return ByteSequences.fileBacked(
                key instanceof NoKey
                    ? path
                    : path.resolve(key.toString())
            );
        }

        @Override
        public ReadContext resolve(Key key) {
            return key instanceof NoKey
                ? this
                : new ReadContextImpl(path.resolve(key.toString()));
        }
    }

    private final class WriteContextImpl implements WriteContext {
        private final Path path;

        private WriteContextImpl(Path path) {
            this.path = path;
        }

        @Override
        public void putMetadata(MutableObjectMetadata metadata) throws IOException {
            try {
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(metadata, metadataPath(path).toFile());
            } catch (JAXBException exception) {
                throw new StagingException(String.format(
                    "Failed to marshal object metadata to path '%s'.", metadataPath(path)
                ), exception);
            }
        }

        private Path targetPath(Key key) throws IOException {
            if (key instanceof NoKey) {
                Files.createDirectories(path.getParent());
                return path;
            } else {
                Files.createDirectories(path);
                return path.resolve(key.toString());
            }
        }

        @Override
        public OutputStream newOutputStream(Key key, @Nullable MutableObjectMetadata metadata) throws IOException {
            return new BufferedOutputStream(Files.newOutputStream(targetPath(key)));
        }

        @Override
        public void putByteSequence(ByteSequence byteSequence, Key key, @Nullable MutableObjectMetadata metadata)
            throws IOException {

            boolean wroteByteSequence = false;
            @Nullable URI uri = byteSequence.getURI();
            if (uri != null && "file".equals(uri.getScheme())) {
                Path sourcePath = Paths.get(uri);

                // Best case: If the source path is within one of the hard-link enabled paths, simply create a hard link
                for (Path hardLinkEnabledPath: hardLinkEnabledPaths) {
                    if (sourcePath.startsWith(hardLinkEnabledPath)) {
                        Files.createLink(targetPath(key), sourcePath);
                        wroteByteSequence = true;
                    }
                }

                // Second-best case: If the given byte sequence is a file, perform a file-system copy
                if (!wroteByteSequence) {
                    Files.copy(sourcePath, targetPath(key));
                    wroteByteSequence = true;
                }
            }

            // Last case: Perform a copy from the input stream
            if (!wroteByteSequence) {
                Files.copy(byteSequence.newInputStream(), targetPath(key));
            }
        }

        @Override
        public WriteContext resolve(Key key) throws IOException {
            return key instanceof NoKey
                ? this
                : new WriteContextImpl(path.resolve(key.toString()));
        }
    }

    static Path metadataPath(Path path) {
        assert path.getNameCount() > 0;
        return path.getParent().resolve(path.getFileName() + METADATA_SUFFIX);
    }

    /**
     * This class is used to create file-based staging areas.
     */
    public static final class Builder {
        private final RuntimeAnnotatedExecutionTrace absoluteTrace;
        private final Path basePath;
        private final RuntimeContext runtimeContext;
        private final ExecutionContext executionContext;
        private ImmutableList<Path> hardLinkEnabledPaths = ImmutableList.of();

        /**
         * Constructs a builder with the specified arguments.
         *
         * @param runtimeContext runtime context including the CloudKeeper repository and the Java class loader
         * @param executionTrace the absolute execution trace that will correspond to the base path of this staging area
         * @param basePath base path of the new staging area in the file system
         * @param executionContext execution context that file-system tasks will be submitted to
         */
        public Builder(RuntimeContext runtimeContext, RuntimeAnnotatedExecutionTrace executionTrace,
                Path basePath, ExecutionContext executionContext) {
            Objects.requireNonNull(runtimeContext);
            absoluteTrace = Objects.requireNonNull(executionTrace);
            this.basePath = Objects.requireNonNull(basePath);
            this.executionContext = Objects.requireNonNull(executionContext);
            this.runtimeContext = Objects.requireNonNull(runtimeContext);
        }

        /**
         * Sets this builder's hard-link enabled paths.
         *
         * <p>Hard-link enabled paths are relevant when
         * {@link MarshalContext#putByteSequence(ByteSequence, Key)}
         * is called from a serialization plug-in. In that case, if the byte sequence has a URI with scheme {@code file}
         * that corresponds to a path with one of the hard-link enabled paths as prefix, then a hard link would be
         * created instead of a copy operation.
         *
         * <p>For performance reasons, and because any file-system operation is potentially blocking, the list of
         * hard-link enabled paths is neither verified by this method nor by {@link #build()}. If the file-based staging
         * area is backed by a different {@link java.nio.file.FileStore} than any of the hard-link enabled paths, the
         * call to {@link MarshalContext#putByteSequence(ByteSequence, Key)} may throw an I/O exception later.
         * Specifically, callers of this method must ensure that for each {@code path} in {@code hardLinkEnabledPaths},
         * it holds that {@code Files.getFileStore(path).equals(Files.getFileStore(basePath))}, where {@code basePath}
         * is the path previously passed to this builder's constructor.
         *
         * <p>By default, {@link #build()} will assume an empty list of hard-link enabled paths.
         *
         * @param hardLinkEnabledPaths list of hard-link enabled paths, must not be null
         * @return this builder
         */
        public Builder setHardLinkEnabledPaths(List<Path> hardLinkEnabledPaths) {
            Objects.requireNonNull(hardLinkEnabledPaths);
            this.hardLinkEnabledPaths = ImmutableList.copyOf(hardLinkEnabledPaths);
            return this;
        }

        private static JAXBContext jaxbContext() {
            try {
                return JAXBContext.newInstance(MutableObjectMetadata.class);
            } catch (JAXBException exception) {
                throw new IllegalStateException(
                    "Exception while constructing JAXB context. This should not happen.", exception);
            }
        }

        /**
         * Creates and returns a new file-based staging area using the attributes of this builder.
         *
         * @return the new staging area
         */
        public FileStagingArea build() {
            return new FileStagingArea(absoluteTrace, runtimeContext, executionContext, new Object(),
                jaxbContext(), basePath, hardLinkEnabledPaths);
        }
    }
}
