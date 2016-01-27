package com.svbio.cloudkeeper.model.api.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

public class ITRecursiveDeleteVisitor {
    private static class InterceptingFileVisitor implements FileVisitor<Path> {
        private final FileVisitor<Path> fileVisitor = RecursiveDeleteVisitor.getInstance();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            return fileVisitor.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            return fileVisitor.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return fileVisitor.visitFileFailed(file, exc);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return fileVisitor.postVisitDirectory(dir, exc);
        }
    }

    private static class DirectoryStructure {
        private final Path root;
        private final Path otherDir;
        private final Path bar;
        private final Path baz;

        private DirectoryStructure() throws IOException {
            root = Files.createTempDirectory(ITRecursiveDeleteVisitor.class.getSimpleName());
            Files.createFile(root.resolve("foo"));
            Files.createDirectory(root.resolve("emptyDirectory"));
            otherDir = Files.createDirectory(root.resolve("otherDirectory"));
            bar = Files.createFile(otherDir.resolve("bar"));
            baz = Files.createFile(otherDir.resolve("baz"));
        }
    }

    @Test
    public void testConcurrentFileDeletion() throws IOException {
        final DirectoryStructure directoryStructure = new DirectoryStructure();
        final AtomicBoolean intercepted = new AtomicBoolean(false);

        InterceptingFileVisitor interceptingFileVisitor = new InterceptingFileVisitor() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.equals(directoryStructure.bar)) {
                    Files.delete(file);
                    intercepted.set(true);
                }
                return super.visitFile(file, attrs);
            }
        };

        Files.walkFileTree(directoryStructure.root, interceptingFileVisitor);
        Assert.assertTrue(intercepted.get());
        Assert.assertFalse(Files.exists(directoryStructure.root));
    }

    @Test
    public void testConcurrentDirectoryDeletion() throws IOException {
        final DirectoryStructure directoryStructure = new DirectoryStructure();
        final AtomicBoolean intercepted = new AtomicBoolean(false);

        InterceptingFileVisitor interceptingFileVisitor = new InterceptingFileVisitor() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(directoryStructure.otherDir)) {
                    Files.delete(directoryStructure.bar);
                    Files.delete(directoryStructure.baz);
                    Files.delete(directoryStructure.otherDir);
                    intercepted.set(true);
                }
                return super.preVisitDirectory(dir, attrs);
            }
        };

        Files.walkFileTree(directoryStructure.root, interceptingFileVisitor);
        Assert.assertTrue(intercepted.get());
        Assert.assertFalse(Files.exists(directoryStructure.root));
    }
}
