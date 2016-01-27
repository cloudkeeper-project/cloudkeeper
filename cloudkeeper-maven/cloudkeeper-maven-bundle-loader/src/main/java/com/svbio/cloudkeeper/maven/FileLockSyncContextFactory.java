package com.svbio.cloudkeeper.maven;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.impl.SyncContextFactory;
import org.eclipse.aether.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A file-lock-based {@link SyncContextFactory} that synchronizes concurrent access to Eclipse Aether artifacts or
 * metadata.
 *
 * <p>This class implements a very coarse-grained locking strategy, locking the entire Aether repository.
 *
 * <p>Methods {@link SyncContext#acquire(Collection, Collection)} and {@link SyncContext#close()} may throw unchecked
 * {@link SyncContextException} exceptions if synchronization or locking fails. Unfortunately, {@link SyncContext} does
 * not specify any standard exceptions for this case.
 *
 * @see org.eclipse.aether.internal.impl.DefaultRepositorySystem#setSyncContextFactory(SyncContextFactory)
 */
public final class FileLockSyncContextFactory implements SyncContextFactory, Closeable {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Path lockFile;
    private final RandomAccessFile randomAccessFile;
    private final FileChannel fileChannel;
    @Nullable private volatile FileLock fileLock = null;

    public FileLockSyncContextFactory(Path lockFile) throws IOException {
        Objects.requireNonNull(lockFile);
        this.lockFile = lockFile;
        randomAccessFile = new RandomAccessFile(lockFile.toFile(), "rw");
        fileChannel = randomAccessFile.getChannel();
    }

    @Override
    public void close() throws IOException {
        // This also closes the associated channel
        randomAccessFile.close();
    }

    @Override
    public SyncContext newInstance(RepositorySystemSession session, boolean shared) {
        Objects.requireNonNull(session);
        return new SyncContextImpl();
    }

    private final class SyncContextImpl implements SyncContext {
        private final Logger log = LoggerFactory.getLogger(getClass());

        @Override
        public void acquire(@Nullable Collection<? extends Artifact> artifacts,
                @Nullable Collection<? extends Metadata> metadatas) {
            // First obtain a hold on the ReentrantLock.
            lock.lock();

            // The rest of this method is only reachable if the current thread has a hold on the ReentrantLock. Hence,
            // there is no race. If this methods throws, the lock *must* be released!
            boolean success = false;
            try {
                if (fileLock == null) {
                    fileLock = fileChannel.lock();
                }
                success = true;
            } catch (IOException exception) {
                throw new SyncContextException(String.format(
                    "Failed to acquire lock on lock file '%s'.", lockFile
                ), exception);
            } finally {
                if (!success) {
                    // This case implies that a Throwable will be thrown from this method.
                    lock.unlock();
                }
            }
        }

        @Override
        public void close() {
            try {
                if (lock.getHoldCount() == 1) {
                    // This case implies that we will release the last hold (by this thread) on the lock. Note that this
                    // code (assuming correct use, which requires acquire() having called before) is only reachable if
                    // the current thread has a hold on the ReentrantLock. Hence, there is no race in this method before
                    // "lock.unlock()".
                    @Nullable FileLock localFileLock = fileLock;
                    if (localFileLock == null) {
                        log.warn("{} about to release last hold on {}, but there is no corresponding file lock.",
                            Thread.currentThread(), lock.getClass().getSimpleName());
                    } else {
                        localFileLock.release();
                        fileLock = null;
                    }
                }
            } catch (IOException exception) {
                throw new SyncContextException(String.format(
                    "Failed to release lock on file '%s'.", lockFile
                ), exception);
            } finally {
                // Whether this method throws or not, we do need to release the hold on the ReentrantLock.
                lock.unlock();
            }
        }
    }
}
