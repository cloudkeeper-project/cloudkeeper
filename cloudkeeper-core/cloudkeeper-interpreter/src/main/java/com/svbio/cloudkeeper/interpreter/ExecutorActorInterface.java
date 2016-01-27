package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.model.api.RuntimeStateProvider;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Simple-module executor message interface.
 *
 * <p>This class contains messages that the CloudKeeper interpreter component sends to the executor. Interpreter
 * component and executor component do not have to reside in the same JVM; hence, all messages in this class are
 * serializable.
 */
final class ExecutorActorInterface {
    private ExecutorActorInterface() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Starts execution of the simple module represented by the contained {@link RuntimeStateProvider} instance. The
     * response will either be {@link ExecutionFinished} in case of success or {@link akka.actor.Status.Failure} in case
     * of an exception.
     */
    static final class ExecuteTrace implements Serializable {
        private static final long serialVersionUID = -4881300776086872911L;

        private final long executionId;
        private final RuntimeStateProvider runtimeStateProvider;

        ExecuteTrace(long executionId, RuntimeStateProvider runtimeStateProvider) {
            this.executionId = executionId;
            this.runtimeStateProvider = Objects.requireNonNull(runtimeStateProvider);
        }

        @Override
        public String toString() {
            return String.format(
                "message %s (execution Id = %d, runtimeStateProvider = %s)",
                getClass().getSimpleName(), executionId, runtimeStateProvider);
        }

        long getExecutionId() {
            return executionId;
        }

        RuntimeStateProvider getRuntimeStateProvider() {
            return runtimeStateProvider;
        }
    }

    /**
     * Response by executor to notify successful execution of a simple module.
     */
    public enum ExecutionFinished { INSTANCE }

    static final class CancelExecution implements Serializable {
        private static final long serialVersionUID = 5238855982089876161L;

        private final String reason;

        CancelExecution(String reason) {
            this.reason = reason;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            return this == otherObject || (
                otherObject != null
                    && getClass() == otherObject.getClass()
                    && reason.equals(((CancelExecution) otherObject).reason)
            );
        }

        @Override
        public int hashCode() {
            return reason.hashCode();
        }

        @Override
        public String toString() {
            return String.format("message %s (reason = %s)", getClass().getSimpleName(), reason);
        }

        String getReason() {
            return reason;
        }
    }
}
