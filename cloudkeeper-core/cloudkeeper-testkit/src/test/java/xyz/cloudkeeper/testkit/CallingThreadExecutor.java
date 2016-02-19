package xyz.cloudkeeper.testkit;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * {@link Executor} implementation that runs all tasks in the current thread upon invocation of {@link #executeNext()}
 * or {@link #executeAll()}.
 */
public final class CallingThreadExecutor implements Executor {
    private final ConcurrentLinkedQueue<Task> pendingTasks = new ConcurrentLinkedQueue<>();

    private static final class Task {
        private final long submissionTimeMillis;
        private final StackTraceElement[] stackTrace;
        private final Runnable runnable;

        private Task(Runnable runnable) {
            submissionTimeMillis = System.currentTimeMillis();
            stackTrace = Thread.currentThread().getStackTrace();
            this.runnable = runnable;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(256);
            builder.append("Task submitted at time ").append(submissionTimeMillis);
            if (stackTrace.length > 0) {
                for (StackTraceElement element: stackTrace) {
                    builder.append("\n\tat ").append(element);
                }
            }
            return builder.toString();
        }
    }

    @Override
    public void execute(Runnable command) {
        pendingTasks.add(new Task(command));
    }

    public boolean hasPendingTasks() {
        return !pendingTasks.isEmpty();
    }

    /**
     * Runs the next task, in the current thread.
     *
     * @throws java.util.NoSuchElementException if there are no more pending tasks
     */
    public void executeNext() {
        Task task = pendingTasks.remove();
        task.runnable.run();
    }

    /**
     * Runs all pending tasks in the current thread, until the queue of pending tasks is empty.
     */
    public void executeAll() {
        while (hasPendingTasks()) {
            executeNext();
        }
    }
}
