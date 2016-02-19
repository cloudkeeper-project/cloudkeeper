package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.util.Success;
import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.api.RuntimeStateProvider;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutor;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class ExecutorActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);
    private final SimpleModuleExecutor simpleModuleExecutor;

    private final Map<ActorRef, Promise<String>> activeTasks = new HashMap<>();

    ExecutorActor(SimpleModuleExecutor simpleModuleExecutor) {
        Objects.requireNonNull(simpleModuleExecutor);
        this.simpleModuleExecutor = simpleModuleExecutor;
    }

    /**
     * Factory for creating an executor actor.
     *
     * <p>Note: This actor creator cannot be serialized.
     */
    static final class Factory implements Creator<UntypedActor> {
        private static final long serialVersionUID = 5003979406567991034L;

        private final SimpleModuleExecutor simpleModuleExecutor;

        Factory(SimpleModuleExecutor simpleModuleExecutor) {
            this.simpleModuleExecutor = Objects.requireNonNull(simpleModuleExecutor);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            throw new NotSerializableException(getClass().getName());
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            throw new NotSerializableException(getClass().getName());
        }

        @Override
        public UntypedActor create() {
            return new ExecutorActor(simpleModuleExecutor);
        }
    }

    /**
     * Handles request to execute the simple module represented by the given
     * {@link xyz.cloudkeeper.model.api.RuntimeStateProvider}.
     *
     * <p>It is guaranteed that this method is called with non-null arguments.
     */
    void executeSimpleModule(ExecutorActorInterface.ExecuteTrace message) {
        final ActorRef sender = getSender();
        if (activeTasks.containsKey(sender)) {
            log.warning(String.format(
                "Ignoring %s because sender %s sent the same request previously.", message, sender
            ));

            Failure failure = new Failure(new ExecutionException(String.format(
                "Already executing simple module submitted by %s.", sender
            )));
            sender.tell(failure, getSelf());
            return;
        }

        RuntimeStateProvider runtimeStateProvider = message.getRuntimeStateProvider();
        ExecutionTrace executionTrace = runtimeStateProvider.getExecutionTrace();

        Promise<String> promise = Futures.promise();
        activeTasks.put(sender, promise);
        log.debug(
            "[Execution ID {}] [Trace {}] Submitting to simple-module executor (of {}).",
            message.getExecutionId(), executionTrace, simpleModuleExecutor.getClass()
        );
        Future<SimpleModuleExecutorResult> resultFuture = simpleModuleExecutor.submit(
            runtimeStateProvider,
            promise.future()
        );

        long executionId = message.getExecutionId();
        resultFuture.onComplete(new OnComplete<SimpleModuleExecutorResult>() {
            @Override
            public void onComplete(@Nullable Throwable throwable, @Nullable SimpleModuleExecutorResult executorResult) {
                assert (throwable == null) != (executorResult == null);

                CompletionAction completionAction;
                if (throwable != null) {
                    completionAction = new FailureAction(sender, executionId, executionTrace, new Failure(
                        new ExecutionException(String.format(
                            "Unexpected exception returned by simple-module executor (of %s).",
                            simpleModuleExecutor.getClass()
                        ), throwable)
                    ));
                } else {
                    completionAction = new ResultAction(sender, executionId, executionTrace, executorResult);
                }

                // The following is essentially an asynchronous call of CompletionAction#run(). Note that we must not
                // call this method directly in order to avoid a race condition.
                getSelf().tell(completionAction, getSelf());
            }
        }, getContext().dispatcher());
    }

    void cancelExecution(ExecutorActorInterface.CancelExecution message) {
        @Nullable Promise<String> promise = activeTasks.get(getSender());
        if (promise != null) {
            promise.tryComplete(new Success<>(message.getReason()));
        } else {
            log.warning(String.format("Ignoring %s because sender %s is unknown.", message, getSender()));
        }
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof ExecutorActorInterface.ExecuteTrace) {
            executeSimpleModule((ExecutorActorInterface.ExecuteTrace) message);
        } else if (message instanceof ExecutorActorInterface.CancelExecution) {
            cancelExecution((ExecutorActorInterface.CancelExecution) message);
        } else if (message instanceof CompletionAction) {
            ((CompletionAction) message).run();
        } else {
            unhandled(message);
        }
    }

    private abstract class CompletionAction<T> {
        private final ActorRef originalSubmitter;
        private final long executionId;
        private final ExecutionTrace executionTrace;
        private final T messageToSubmitter;

        private CompletionAction(ActorRef originalSubmitter, long executionId, ExecutionTrace executionTrace,
                T messageToSubmitter) {
            this.originalSubmitter = originalSubmitter;
            this.executionId = executionId;
            this.executionTrace = executionTrace;
            this.messageToSubmitter = messageToSubmitter;
        }

        @Nullable
        abstract Throwable throwable(T message);

        final void run() {
            activeTasks.remove(originalSubmitter);
            @Nullable Throwable throwable = throwable(messageToSubmitter);
            if (throwable == null) {
                log.debug("[Execution ID {}] [Trace {}] Simple-module execution finished successfully.",
                    executionId, executionTrace);
            } else {
                log.debug("[Execution ID {}] [Trace {}] Simple-module execution failed.{}",
                    executionId, executionTrace, Logging.stackTraceFor(throwable));
            }
            originalSubmitter.tell(messageToSubmitter, getSelf());
        }
    }

    private final class FailureAction extends CompletionAction<Failure> {
        private FailureAction(ActorRef originalSubmitter, long executionId, ExecutionTrace executionTrace,
                Failure failure) {
            super(originalSubmitter, executionId, executionTrace, failure);
        }

        @Override
        Throwable throwable(Failure message) {
            return message.cause();
        }
    }

    private final class ResultAction extends CompletionAction<SimpleModuleExecutorResult> {
        private ResultAction(ActorRef originalSubmitter, long executionId, ExecutionTrace executionTrace,
                SimpleModuleExecutorResult result) {
            super(originalSubmitter, executionId, executionTrace, result);
        }

        @Nullable
        @Override
        Throwable throwable(SimpleModuleExecutorResult message) {
            return message.getExecutionException().getOrElse(null);
        }
    }
}
