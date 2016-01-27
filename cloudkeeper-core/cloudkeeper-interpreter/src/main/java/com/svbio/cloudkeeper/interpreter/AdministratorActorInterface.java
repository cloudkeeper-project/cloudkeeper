package com.svbio.cloudkeeper.interpreter;

import akka.japi.Option;
import com.svbio.cloudkeeper.model.api.RuntimeContext;
import com.svbio.cloudkeeper.model.api.WorkflowExecutionException;
import com.svbio.cloudkeeper.model.api.staging.StagingArea;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import scala.concurrent.Promise;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Message interface for {@link AdministratorActor}.
 */
final class AdministratorActorInterface {
    private AdministratorActorInterface() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Registers the given workflow execution with the messenger and returns the execution ID as acknowledgement.
     *
     * <p>This message is sent only locally, as the promises contained in this message cannot be completed in a
     * different JVM. Instead, the messenger actor listens to future messages (coming from possibly remote interpreter
     * actors) and completes the promises.
     */
    static final class ManageExecution {
        private final long executionId;
        private final RuntimeContext runtimeContext;
        private final StagingArea stagingArea;
        private final boolean retrieveResults;
        private final ImmutableList<Promise<Object>> outPortPromises;
        private final Promise<Long> finishTimeMillisPromise;
        private final Promise<Option<WorkflowExecutionException>> executionExceptionPromise;

        ManageExecution(long executionId, RuntimeContext runtimeContext, StagingArea stagingArea,
                boolean retrieveResults, List<Promise<Object>> outPortPromises, Promise<Long> finishTimeMillisPromise,
                Promise<Option<WorkflowExecutionException>> executionExceptionPromise) {
            this.executionId = executionId;
            this.runtimeContext = Objects.requireNonNull(runtimeContext);
            this.stagingArea = Objects.requireNonNull(stagingArea);
            this.retrieveResults = retrieveResults;
            this.outPortPromises = ImmutableList.copyOf(Objects.requireNonNull(outPortPromises));
            this.finishTimeMillisPromise = Objects.requireNonNull(finishTimeMillisPromise);
            this.executionExceptionPromise = Objects.requireNonNull(executionExceptionPromise);
        }

        @Override
        public String toString() {
            return String.format(
                "message %s (executionId = %d, stagingArea = %s)",
                getClass().getSimpleName(), executionId, stagingArea
            );
        }

        long getExecutionId() {
            return executionId;
        }

        RuntimeContext getRuntimeContext() {
            return runtimeContext;
        }

        StagingArea getStagingArea() {
            return stagingArea;
        }

        boolean isRetrieveResults() {
            return retrieveResults;
        }

        List<Promise<Object>> getOutPortPromises() {
            return outPortPromises;
        }

        Promise<Long> getFinishTimeMillisPromise() {
            return finishTimeMillisPromise;
        }

        Promise<Option<WorkflowExecutionException>> getExecutionExceptionPromise() {
            return executionExceptionPromise;
        }
    }

    /**
     * Informs the messenger that a new output is available.
     */
    static final class OutPortAvailable implements Serializable {
        private static final long serialVersionUID = -5945346854042409534L;

        private final long executionId;
        private final int outPortId;

        OutPortAvailable(long executionId, int outPortId) {
            this.executionId = executionId;
            this.outPortId = outPortId;
        }

        @Override
        public String toString() {
            return String.format(
                "message %s (executionId = %d, outPortId = %d)", getClass().getSimpleName(), executionId, outPortId
            );
        }

        long getExecutionId() {
            return executionId;
        }

        int getOutPortId() {
            return outPortId;
        }
    }

    /**
     * Informs the operator that an execution has finished.
     *
     * <p>This message is sent from the workflow interpreter to the operator. Since workflow interpretation may happen
     * on a different machine, this message is serializable.
     */
    static final class ExecutionFinished implements Serializable {
        private static final long serialVersionUID = -8895015652348672466L;

        private final long executionId;
        private final InterpreterException exception;

        /**
         * Constructor.
         *
         * @param executionId the execution ID
         * @param exception exception that caused the workflow execution to fail, or {@code null} if execution was
         *     successful
         */
        ExecutionFinished(long executionId, InterpreterException exception) {
            this.executionId = executionId;
            this.exception = exception;
        }

        @Override
        public String toString() {
            return String.format(
                "message %s (executionId = %d, exception = %s)", getClass().getSimpleName(), executionId, exception
            );
        }

        long getExecutionId() {
            return executionId;
        }

        InterpreterException getException() {
            return exception;
        }
    }
}
