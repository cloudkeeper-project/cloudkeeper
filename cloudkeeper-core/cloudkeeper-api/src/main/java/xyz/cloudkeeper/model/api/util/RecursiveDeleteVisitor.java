package xyz.cloudkeeper.model.api.util;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * File visitor that recursively deletes all contained files and directories.
 *
 * <p>This class is stateless, and only a singleton instance is available with {@link #getInstance()}. Clients that wish
 * to recursively delete a directory should pass the instance of this class as second argument to
 * {@link Files#walkFileTree(Path, java.nio.file.FileVisitor)}.
 *
 * <p>This class uses method {@link Files#deleteIfExists(Path)} to remove files. Note, however, that this does not make
 * it safe to concurrently delete nested directories with this visitor. There are no guarantees that
 * {@link Files#walkFileTree(Path, java.nio.file.FileVisitor)} needs to operate atomically.
 */
public final class RecursiveDeleteVisitor extends SimpleFileVisitor<Path> {
    private static final RecursiveDeleteVisitor INSTANCE = new RecursiveDeleteVisitor();

    /**
     * Returns the singleton instance of this class.
     */
    public static RecursiveDeleteVisitor getInstance() {
        return INSTANCE;
    }

    private RecursiveDeleteVisitor() { }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(exception);

        if (exception instanceof NoSuchFileException) {
            return FileVisitResult.CONTINUE;
        } else {
            throw exception;
        }
    }

    @Override
    public FileVisitResult postVisitDirectory(Path directory, @Nullable IOException exception) throws IOException {
        Objects.requireNonNull(directory);
        if (exception != null) {
            throw exception;
        }

        Files.deleteIfExists(directory);
        return FileVisitResult.CONTINUE;
    }
}
