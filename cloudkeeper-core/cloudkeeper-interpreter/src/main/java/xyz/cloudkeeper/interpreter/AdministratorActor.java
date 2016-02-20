package xyz.cloudkeeper.interpreter;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import xyz.cloudkeeper.interpreter.AdministratorActorInterface.ManageExecution;
import xyz.cloudkeeper.interpreter.AdministratorActorInterface.OutPortAvailable;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.WorkflowExecutionException;
import xyz.cloudkeeper.model.api.staging.StagingArea;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeModule;
import xyz.cloudkeeper.model.runtime.element.module.RuntimeOutPort;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Administrator actor.
 *
 * <p>The <em>administrator</em> in a CloudKeeper environment is responsible for listening to status updates by the
 * potentially remote workflow interpreter actors. In particular, this includes completing promises created by (local)
 * {@link WorkflowExecutionBuilderImpl} instances.
 *
 * @see AdministratorActorCreator
 */
final class AdministratorActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);

    private final NavigableMap<Long, ManagedExecution> executions = new TreeMap<>();

    /**
     * Status of an out-port.
     */
    private enum OutPortStatus {
        /**
         * An {@link OutPortAvailable} message has not yet been received for this out-port.
         *
         * <p>This is the initial status for every out-port.
         */
        UNAVAILABLE,

        /**
         * An {@link OutPortAvailable} has been received and {@link StagingArea#getObject(RuntimeExecutionTrace)} has
         * been called, but the future has not yet been completed.
         */
        STAGING_AREA,

        /**
         * The future returned by {@link StagingArea#getObject(RuntimeExecutionTrace)} has been completed either
         * successfully or exceptionally.
         */
        DONE
    }

    private static final class ManagedExecution {
        private final ManageExecution request;
        private final List<OutPortStatus> outPortStatusList;

        private ManagedExecution(ManageExecution request) {
            assert request.getStagingArea().getAnnotatedExecutionTrace().getModule().getOutPorts().size()
                == request.getOutPortPromises().size();

            this.request = request;
            outPortStatusList
                = new ArrayList<>(Collections.nCopies(request.getOutPortPromises().size(), OutPortStatus.UNAVAILABLE));
        }
    }

    void manageExecution(ManageExecution message) {
        long executionId = message.getExecutionId();

        if (!executions.containsKey(executionId)) {
            executions.put(executionId, new ManagedExecution(message));
        } else {
            log.warning(String.format("Ignoring duplicate %s.", message));
        }

        // In either case, we need to send a reply to the sender
        getSender().tell(executionId, getSelf());
    }

    void outPortAvailable(OutPortAvailable message) {
        long executionId = message.getExecutionId();
        int outPortId = message.getOutPortId();

        @Nullable ManagedExecution execution = executions.get(executionId);
        if (execution != null) {
            StagingArea stagingArea = execution.request.getStagingArea();
            RuntimeModule module = stagingArea.getAnnotatedExecutionTrace().getModule();
            List<? extends RuntimeOutPort> outPorts = module.getOutPorts();

            assert outPorts.size() == execution.request.getOutPortPromises().size();

            if (outPortId >= 0 && outPortId < outPorts.size()) {
                if (execution.outPortStatusList.get(outPortId) == OutPortStatus.UNAVAILABLE) {
                    RuntimeOutPort outPort = outPorts.get(outPortId);
                    CompletableFuture<Object> promise = execution.request.getOutPortPromises().get(outPortId);
                    if (execution.request.isRetrieveResults()) {
                        execution.outPortStatusList.set(outPortId, OutPortStatus.STAGING_AREA);
                        CompletableFuture<Object> future
                            = stagingArea.getObject(ExecutionTrace.empty().resolveOutPort(outPort.getSimpleName()));
                        future.whenComplete((object, failure) -> {
                            // staging-area contract: failure == null || failure instanceof StagingException
                            if (failure != null) {
                                promise.completeExceptionally(failure);
                            } else {
                                promise.complete(object);
                            }
                            getSelf().tell(new GotValueForOutPort(execution, outPort), getSelf());
                        });
                    } else {
                        execution.outPortStatusList.set(outPortId, OutPortStatus.DONE);
                        promise.completeExceptionally(new WorkflowExecutionException(
                            "Workflow execution configured to not provide out-port values."
                        ));
                    }
                } else {
                    log.warning(String.format("Ignoring duplicate %s.", message));
                }
            } else {
                log.warning(String.format(
                    "Ignoring invalid %s: %s has only %d out-ports.",
                    message, module, outPorts.size()
                ));
            }
        } else {
            log.warning(String.format("Ignoring %s because execution ID is unknown.", message));
        }
    }

    private void executionFinished(AdministratorActorInterface.ExecutionFinished message) {
        long executionId = message.getExecutionId();
        @Nullable WorkflowExecutionException exception = message.getException();

        @Nullable ManagedExecution execution = executions.get(executionId);
        if (execution != null) {
            execution.request.getFinishTimeMillisPromise().complete(System.currentTimeMillis());
            execution.request.getExecutionExceptionPromise().complete(
                exception != null
                    ? Optional.of(exception)
                    : Optional.empty()
            );

            List<RuntimeOutPort> uncompletedOutPorts = new ArrayList<>();
            int outPortId = 0;
            for (CompletableFuture<Object> promise: execution.request.getOutPortPromises()) {
                // Note that the following check does not introduce a race. We are inside an actor, which processes
                // only one message at a time. At the end of this method, the execution is removed, so the promise is
                // guaranteed to be never touched again by us.
                if (execution.outPortStatusList.get(outPortId) == OutPortStatus.UNAVAILABLE) {
                    WorkflowExecutionException outPortException;
                    if (exception != null) {
                        outPortException = exception;
                    } else {
                        RuntimeOutPort outPort = execution.request.getStagingArea().getAnnotatedExecutionTrace()
                            .getModule().getOutPorts().get(outPortId);
                        uncompletedOutPorts.add(outPort);
                        outPortException = new WorkflowExecutionException(String.format(
                            "Workflow execution finished before %s received value.", outPort
                        ));
                    }
                    promise.completeExceptionally(outPortException);
                }
                ++outPortId;
            }
            tryCloseRuntimeContext(execution);
            if (!uncompletedOutPorts.isEmpty()) {
                // This should not happen. This case means that we either did not receive all messages, or not
                // all messages were generated. This could mean a bug in the code.
                log.warning("Promises for the following out-ports were only completed after execution id {} "
                    + "finished: {}", executionId, uncompletedOutPorts);
            }

            executions.remove(executionId);
        } else {
            log.warning(String.format("Ignoring %s because execution ID is unknown.", message));
        }
    }

    private void tryCloseRuntimeContext(ManagedExecution managedExecution) {
        List<OutPortStatus> outPortStatusList = managedExecution.outPortStatusList;
        if (outPortStatusList.stream().filter(status -> status != OutPortStatus.DONE).count() == 0) {
            RuntimeContext runtimeContext = managedExecution.request.getRuntimeContext();
            // Shield this actor from exceptions that RuntimeContext#close() may throw
            try {
                runtimeContext.close();
            } catch (Exception exception) {
                // We need a fault barrier with a catch-all here, because closing the runtime context may run user code.
                log.error(exception, "Failed to close runtime context for execution id {}.",
                    managedExecution.request.getExecutionId());
            }
        }
    }

    private void gotValueForOutPort(ManagedExecution managedExecution, RuntimeOutPort outPort) {
        List<OutPortStatus> outPortStatusList = managedExecution.outPortStatusList;
        outPortStatusList.set(outPort.getOutIndex(), OutPortStatus.DONE);
        tryCloseRuntimeContext(managedExecution);
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof ManageExecution) {
            manageExecution((ManageExecution) message);
        } else if (message instanceof OutPortAvailable) {
            outPortAvailable((OutPortAvailable) message);
        } else if (message instanceof AdministratorActorInterface.ExecutionFinished) {
            executionFinished((AdministratorActorInterface.ExecutionFinished) message);
        } else if (message instanceof GotValueForOutPort) {
            GotValueForOutPort arguments = (GotValueForOutPort) message;
            gotValueForOutPort(arguments.managedExecution, arguments.outPort);
        } else {
            unhandled(message);
        }
    }

    private static final class GotValueForOutPort {
        private final ManagedExecution managedExecution;
        private final RuntimeOutPort outPort;

        private GotValueForOutPort(ManagedExecution managedExecution, RuntimeOutPort outPort) {
            this.managedExecution = managedExecution;
            this.outPort = outPort;
        }
    }
}
