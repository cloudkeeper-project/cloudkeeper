package com.svbio.cloudkeeper.simple;

import akka.dispatch.Futures;
import com.svbio.cloudkeeper.dsl.DSLOutPort;
import com.svbio.cloudkeeper.model.api.WorkflowExecution;
import scala.concurrent.Await;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;
import scala.util.Failure;
import scala.util.Success;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class WorkflowExecutions {
    private WorkflowExecutions() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    private static class WhenComplete<T> implements WorkflowExecution.OnActionComplete<T> {
        private final Promise<T> promise = Futures.promise();

        @Override
        public void complete(Throwable throwable, T value) {
            promise.complete(throwable != null
                ? new Failure<T>(throwable)
                : new Success<>(value)
            );
        }

        private T await(long timeout, TimeUnit unit) throws AwaitException, TimeoutException, InterruptedException {
            try {
                return Await.result(promise.future(), Duration.create(timeout, unit));
            } catch (TimeoutException|InterruptedException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new AwaitException(exception);
            }
        }
    }

    public static long getExecutionId(WorkflowExecution workflowExecution, long timeout, TimeUnit unit)
        throws AwaitException, TimeoutException, InterruptedException {

        WhenComplete<Long> whenComplete = new WhenComplete<>();
        workflowExecution.whenHasExecutionId(whenComplete);
        return whenComplete.await(timeout, unit);
    }

    public static Object getOutputValue(WorkflowExecution workflowExecution, String outPortName, long timeout,
        TimeUnit unit) throws AwaitException, TimeoutException, InterruptedException {

        WhenComplete<Object> whenComplete = new WhenComplete<>();
        workflowExecution.whenHasOutput(outPortName, whenComplete);
        return whenComplete.await(timeout, unit);
    }

    public static <T> T getOutputValue(WorkflowExecution workflowExecution, DSLOutPort<T> outPort, long timeout,
        TimeUnit unit) throws AwaitException, TimeoutException, InterruptedException {

        // TODO: It should be verified that the DSL port fits to this workflow execution.
        @SuppressWarnings("unchecked")
        T outputValue = (T) getOutputValue(workflowExecution, outPort.getSimpleName().toString(), timeout, unit);
        return outputValue;
    }

    public static void awaitFinish(WorkflowExecution workflowExecution, long timeout, TimeUnit unit)
        throws AwaitException, TimeoutException, InterruptedException {

        WhenComplete<Void> whenComplete = new WhenComplete<>();
        workflowExecution.whenExecutionFinished(whenComplete);
        whenComplete.await(timeout, unit);
    }
}
