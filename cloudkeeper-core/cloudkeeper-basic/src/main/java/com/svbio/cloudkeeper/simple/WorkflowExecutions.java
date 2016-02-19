package com.svbio.cloudkeeper.simple;

import com.svbio.cloudkeeper.dsl.DSLOutPort;
import com.svbio.cloudkeeper.model.api.WorkflowExecution;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class WorkflowExecutions {
    private WorkflowExecutions() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    public static <T> T getOutputValue(WorkflowExecution workflowExecution, DSLOutPort<T> outPort, long timeout,
            TimeUnit unit) throws ExecutionException, TimeoutException, InterruptedException {
        // TODO: It should be verified that the DSL port fits to this workflow execution.
        @SuppressWarnings("unchecked")
        T outputValue = (T) workflowExecution.getOutput(outPort.getSimpleName().toString()).get(timeout, unit);
        return outputValue;
    }
}
