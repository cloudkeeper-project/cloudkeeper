package com.svbio.cloudkeeper.model.util;

import cloudkeeper.serialization.ByteSequenceMarshaler;
import cloudkeeper.types.ByteSequence;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;

/**
 * This class consists exclusively of static methods that operate on byte sequences.
 */
public final class ByteSequences {
    /**
     * Buffer size when copying from one stream to another.
     */
    private static final int BUFFER_SIZE = 8192;

    private ByteSequences() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns a new {@link ByteSequence} representing the content of the given file.
     *
     * <p>The returned byte sequence will be an undecorated (also called raw or untyped) byte sequence, with default
     * content type {@link ByteSequence#DEFAULT_CONTENT_TYPE}.
     *
     * <p>Note that no guarantees are made about the visibility of concurrent modifications to the given file.
     *
     * @param path the {@link Path} of the file
     * @return the resulting {@link ByteSequence}
     */
    public static ByteSequence fileBacked(Path path) {
        return new FileByteSequence(path);
    }

    /**
     * Returns a new {@link ByteSequence} representing the content of the given byte array.
     *
     * <p>The returned byte sequence will be an undecorated (also called raw or untyped) byte sequence, with default
     * content type {@link ByteSequence#DEFAULT_CONTENT_TYPE}.
     *
     * <p>Note that no guarantees are made about the visibility of concurrent modifications to the given byte array.
     *
     * @param bytes the byte array
     * @return the resulting {@link ByteSequence}
     */
    public static ByteSequence arrayBacked(byte[] bytes) {
        return new ByteArrayByteSequence(bytes);
    }

    /**
     * Returns a self-contained {@link ByteSequence} representing the same content as the given byte sequence.
     *
     * <p>The returned byte sequence will be decorated just like the given byte sequence, with content type
     * {@link ByteSequence#getContentType()}.
     *
     * @param byteSequence byte sequence, possibly not self-contained
     * @return self-contained byte sequence
     * @throws IOException if an I/O error occurs
     */
    public static ByteSequence selfContained(ByteSequence byteSequence) throws IOException {
        if (byteSequence.isSelfContained()) {
            return byteSequence;
        }

        ByteSequenceMarshaler.Decorator decorator = byteSequence.getDecorator();
        try (
            InputStream inputStream = byteSequence.newInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE)
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                int numBytesRead = inputStream.read(buffer);
                if (numBytesRead == -1) {
                    break;
                }
                outputStream.write(buffer, 0, numBytesRead);
            }

            // From the ByteArrayOutputStream JavaDoc: The methods [...] can be called after the stream has been
            // closed without generating an IOException. Closing streams is idempotent, so there will not be a
            // problem at the end of the try-with-resources statement.
            outputStream.close();
            return decorator.decorate(arrayBacked(outputStream.toByteArray()));
        }
    }

    /**
     * Copies a byte sequence to a file.
     *
     * <p>If the {@link CopyOptimizationOption#LINK_INSTEAD_OF_COPY} option is specified, this method tries to perform
     * a hard link using {@link Files#createLink(Path, Path)} if the following conditions hold:
     * <ul><li>
     *     The byte sequence is stored in a file; that is, {@link ByteSequence#getURI()} must return a URI with scheme
     *     {@code "file"}.
     * </li><li>
     *     The file containing the byte sequence is located in the same file store as the parent path of the target
     *     path. (If the target path does not have a parent, this condition is always violated.)
     * </li><li>
     *     The target path does not yet exist.
     * </li></ul>
     *
     * <p>If the REPLACE_EXISTING option is specified, and the target file already exists, then it is replaced if it is
     * not a non-empty directory. This holds also if {@link CopyOptimizationOption#LINK_INSTEAD_OF_COPY} is specified;
     * if in this case the target file already exists, a normal copy operation will be performed instead of creating a
     * hard link.
     *
     * <p>If no hard link is performed and if the byte sequence is stored in a file, this method will call
     * {@link Files#copy(Path, Path, CopyOption...)}. Otherwise, {@link Files#copy(InputStream, Path, CopyOption...)}
     * will be called. In either case, the copy options passed to this method (except any
     * {@link CopyOptimizationOption}) will be passed to the respective copy method in {@link Files}.
     *
     * @param byteSequence the byte sequence to copy
     * @param target the path to the file
     * @param options options specifying how the copy should be done
     * @return the path to the target file
     * @throws UnsupportedOperationException if the array contains a copy option that is not supported
     * @throws IOException if an I/O error occurs
     * @throws  SecurityException if the operation is denied by the security manager
     */
    public static Path copy(ByteSequence byteSequence, Path target, CopyOption... options) throws IOException {
        EnumSet<CopyOptimizationOption> optimizationOptions = EnumSet.noneOf(CopyOptimizationOption.class);

        // Split options into our options and regular (Java) options.
        @Nullable LinkedList<CopyOption> standardOptions = null;
        int index = 0;
        for (CopyOption option: options) {
            if (option instanceof CopyOptimizationOption) {
                optimizationOptions.add((CopyOptimizationOption) option);
                if (standardOptions == null) {
                    standardOptions = new LinkedList<>(Arrays.asList(options).subList(0, index));
                }
            } else if (standardOptions != null) {
                standardOptions.add(option);
            }
            ++index;
        }
        CopyOption[] standardOptionsArray = standardOptions != null
            ? standardOptions.toArray(new CopyOption[standardOptions.size()])
            : options;

        @Nullable URI uri = byteSequence.getURI();
        if (uri != null && "file".equals(uri.getScheme())) {
            Path sourcePath = Paths.get(uri);

            @Nullable Path sourceParentPath = sourcePath.getParent();
            @Nullable Path targetParentPath = target.getParent();
            // Best case: If the source path is within one of the hard-link enabled paths, simply create a hard link
            // Note that we are using the enclosing directories in order to get the file store. If we didn't, and if
            // sourcePath was a symbolic link, then the following check could give a false negative.
            if (optimizationOptions.contains(CopyOptimizationOption.LINK_INSTEAD_OF_COPY)
                    && sourceParentPath != null && targetParentPath != null
                    && Files.getFileStore(sourceParentPath).equals(Files.getFileStore(targetParentPath))) {
                try {
                    return Files.createLink(target, sourcePath);
                } catch (FileAlreadyExistsException exception) {
                    // We catch this exception instead of calling Files#exists(Path) prior to
                    // Files#createLink(Path, Path), because the latter implementation would lead to a race condition.
                    if (!Arrays.asList(standardOptionsArray).contains(StandardCopyOption.REPLACE_EXISTING)) {
                        throw exception;
                    }
                }
            }

            // Second-best case: If the given byte sequence is a file, perform a file-system copy
            return Files.copy(sourcePath, target, standardOptionsArray);
        }

        // Last case: Perform a copy from the input stream
        Files.copy(byteSequence.newInputStream(), target, standardOptionsArray);
        return target;
    }

    private static final class FileByteSequence implements ByteSequence {
        private final Path path;

        private FileByteSequence(Path path) {
            assert path != null;
            this.path = path;
        }

        @Override
        public ByteSequenceMarshaler.Decorator getDecorator() {
            return ByteSequenceMarshaler.noDecorator();
        }

        @Override
        public URI getURI() {
            return path.toUri();
        }

        @Override
        public boolean isSelfContained() {
            return false;
        }

        @Override
        public long getContentLength() throws IOException {
            return Files.size(path);
        }

        @Override
        public String getContentType() {
            return DEFAULT_CONTENT_TYPE;
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return Files.newInputStream(path);
        }
    }

    /**
     * Defines the options as to how copies are to be performed.
     */
    public enum CopyOptimizationOption implements CopyOption {
        /**
         * If possible, perform a hard-link instead of a physical copy of the byte stream.
         *
         * @see ByteSequences#copy(ByteSequence, Path, CopyOption...)
         */
        LINK_INSTEAD_OF_COPY
    }

    private static final class ByteArrayByteSequence implements ByteSequence {
        private final byte[] bytes;

        private ByteArrayByteSequence(byte[] bytes) {
            assert bytes != null;
            this.bytes = bytes;
        }

        @Override
        public ByteSequenceMarshaler.Decorator getDecorator() {
            return ByteSequenceMarshaler.noDecorator();
        }

        @Override
        public URI getURI() {
            return null;
        }

        @Override
        public boolean isSelfContained() {
            return true;
        }

        @Override
        public long getContentLength() throws IOException {
            return bytes.length;
        }

        @Override
        public String getContentType() throws IOException {
            return DEFAULT_CONTENT_TYPE;
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }
    }
}
