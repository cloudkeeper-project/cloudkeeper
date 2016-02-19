package xyz.cloudkeeper.marshaling;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Class that facilitates "try-with-dynamically-many-resources".
 */
final class Closer implements Closeable {
    private final Deque<Closeable> closeables = new ArrayDeque<>();

    void register(Closeable closeable) {
        Objects.requireNonNull(closeable);
        closeables.add(closeable);
    }

    @Override
    public void close() throws IOException {
        @Nullable Throwable previousThrowable = null;
        while (!closeables.isEmpty()) {
            Closeable closeable = closeables.removeFirst();
            try {
                closeable.close();
            } catch (Throwable throwable) {
                if (previousThrowable != null) {
                    throwable.addSuppressed(previousThrowable);
                } else {
                    previousThrowable = throwable;
                }
            }
        }

        // previousThrowable must be of type IOException, or it must be of unchecked exception class. According to
        // JLS §11.1, "the unchecked exception classes are the run-time exception classes and the error classes".
        if (previousThrowable instanceof IOException) {
            throw (IOException) previousThrowable;
        } else if (previousThrowable instanceof RuntimeException) {
            throw (RuntimeException) previousThrowable;
        } else if (previousThrowable != null) {
            throw (Error) previousThrowable;
        }
    }
}
