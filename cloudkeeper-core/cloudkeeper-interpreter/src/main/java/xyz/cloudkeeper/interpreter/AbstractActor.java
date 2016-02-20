package xyz.cloudkeeper.interpreter;

import akka.actor.UntypedActor;
import net.florianschoppmann.java.futures.Futures;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;

abstract class AbstractActor extends UntypedActor {
    private final IdentityHashMap<Object, AsynchronousAction> pendingActions = new IdentityHashMap<>();
    private final ExecutionContextExecutor asyncTaskContext;

    /**
     * Constructor.
     *
     * @param asyncTaskContext the {@link ExecutionContext} that is to be used for scheduling asynchronous tasks (such
     *     as futures), or {@code null} to indicate that {@code getContext().dispatcher()} should be used
     */
    AbstractActor(@Nullable ExecutionContextExecutor asyncTaskContext) {
        this.asyncTaskContext = asyncTaskContext == null
            ? getContext().dispatcher()
            : asyncTaskContext;
    }

    /**
     * Returns the {@link ExecutionContext} that is to be used for scheduling asynchronous tasks (such as futures).
     *
     * <p>During testing, this may be different from the message dispatcher returned by
     * {@link akka.actor.UntypedActorContext#dispatcher()} (accessible through {@link #getContext()}.
     */
    final ExecutionContextExecutor getAsyncTaskContext() {
        return asyncTaskContext;
    }

    /**
     * Calls {@link #onEmptySetOfAsynchronousActions} if there are no pending asynchronous actions.
     *
     * <p>Note that there are no pending asynchronous actions if {@link #pendingActions} is empty.
     */
    final void checkIfNoAsynchronousActions() {
        if (pendingActions.isEmpty()) {
            onEmptySetOfAsynchronousActions();
        }
    }

    /**
     * Handles event that there are no pending asynchronous actions.
     */
    abstract void onEmptySetOfAsynchronousActions();

    private AsynchronousAction createAsynchronousAction(Object key, String description, Object... args) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(description);

        AsynchronousAction action = new AsynchronousAction(key, String.format(description, args));
        @Nullable AsynchronousAction previous = pendingActions.put(key, action);
        if (previous != null) {
            throw new IllegalStateException(String.format(
                "Tried to create new %s with key %s, which already has the associated %s.", action, key, previous
            ));
        }
        return action;
    }

    private <T> void createAsynchronousActionForFuture(CompletionStage<T> completionStage, boolean pipeResultToSelf,
            String description, Object... args) {
        AsynchronousAction action = createAsynchronousAction(completionStage, description, args);
        completionStage.whenCompleteAsync((result, throwable) -> {
            getSelf().tell(action, getSelf());
            if (throwable != null) {
                getSelf().tell(
                    new AsynchronousActionFailed(action, Futures.unwrapCompletionException(throwable)),
                    getSelf()
                );
            } else if (pipeResultToSelf) {
                getSelf().tell(result, getSelf());
            }
        }, asyncTaskContext);
    }

    /**
     * Adds a completion handler to the completion stage, which sends the result to this actor. In case the stage is
     * completed exceptionally, fails the current actor with the given description.
     *
     * <p>This method also adds a new {@link AsynchronousAction} to {@link #pendingActions} with {@code completionStage}
     * as key. This asynchronous action is automatically removed from the map after the completion stage is completed.
     * At that time, {@link #checkIfNoAsynchronousActions} is automatically called.
     *
     * @param completionStage Completion stage to that a completion handler will be added. If completed exceptionally,
     *     method {@link #asynchronousActionFailed(String, Throwable)} will be triggered asynchronously.
     * @param description Description of this asynchronous action. The string should be a reduced gerund clause that
     *     could extend "while ...". There should be no period(.) at the end. For instance, "cleaning intermediate
     *     files".
     * @param args arguments referenced by the format specifiers in the format string
     */
    final void pipeResultToSelf(CompletionStage<?> completionStage, String description, Object... args) {
        createAsynchronousActionForFuture(completionStage, true, description, args);
    }

    /**
     * Registers a completion handler for the given completion stage. In case the stage is completed exceptionally,
     * fails the current actor with the given description.
     *
     * <p>This method also adds a new {@link AsynchronousAction} to {@link #pendingActions} with {@code completionStage}
     * as key. This asynchronous action is automatically removed from the map after the stage is completed. At that
     * time, {@link #checkIfNoAsynchronousActions} is automatically called.
     *
     * <p>Unlike {@link #pipeResultToSelf}, this method does not cause any messages to be sent to this actor.
     *
     * @param completionStage Completion stage to that a completion handler will be added. If completed exceptionally,
     *     method {@link #asynchronousActionFailed(String, Throwable)} will be triggered asynchronously.
     * @param description Description of this asynchronous action. The string should be a reduced gerund clause that
     *     could extend "while ...". There should be no period(.) at the end. For instance, "cleaning intermediate
     *     files".
     * @param args arguments referenced by the format specifiers in the format string
     */
    final void awaitAsynchronousAction(CompletionStage<?> completionStage, String description, Object... args) {
        createAsynchronousActionForFuture(completionStage, false, description, args);
    }

    /**
     * Starts an asynchronous action.
     *
     * <p>This method adds a new {@link AsynchronousAction} to {@link #pendingActions} with {@code key} as key. This
     * asynchronous action needs to be explicitly removed from the map by calling {@link #endAsynchronousAction}.
     *
     * @param key lookup key (in {@link #pendingActions}) for this asynchronous action
     * @param description Description of this asynchronous action. The string should be a reduced gerund clause that
     *     could extend "while ...". There should be no period(.) at the end. For instance, "cleaning intermediate
     *     files".
     * @param args arguments referenced by the format specifiers in the format string
     */
    final void startAsynchronousAction(Object key, String description, Object... args) {
        createAsynchronousAction(key, description, args);
    }

    /**
     * Ends an asynchronous action previously started with {@link #startAsynchronousAction}.
     *
     * <p>This method calls {@link #checkIfNoAsynchronousActions}.
     *
     * @param key lookup key (in {@link #pendingActions}) for this asynchronous action
     */
    final void endAsynchronousAction(Object key) {
        Objects.requireNonNull(key);
        @Nullable AsynchronousAction removedValue = pendingActions.remove(key);

        if (removedValue == null) {
            throw new IllegalStateException(String.format(
                "Tried to finish unknown asynchronous action (key: %s). This may indicate that the asynchronous "
                    + "action was already finished before.", key
            ));
        }

        checkIfNoAsynchronousActions();
    }

    private void finishAsynchronousAction(AsynchronousAction action) {
        endAsynchronousAction(action.key);
    }

    /**
     * Handles an exception in an asynchronous action.
     *
     * <p>If an implementation throws an exception, it is expected to create a new {@link InterpreterException} with the
     * given message and the given cause.
     *
     * @param message high-level message explaining the failure, including what the asynchronous action was about
     * @param cause The failure that caused the asynchronous action to fail. This will not be a
     *     {@link java.util.concurrent.CompletionException} with cause, as this would have been unwrapped at send time
     *     (see {@link #createAsynchronousActionForFuture(CompletionStage, boolean, String, Object...)}).
     */
    protected abstract void asynchronousActionFailed(String message, Throwable cause)
        throws InterpreterException;

    private void asynchronousActionFailed(Throwable throwable, AsynchronousAction action)
            throws InterpreterException {
        asynchronousActionFailed(String.format("Exception while %s.", action.description), throwable);
    }

    private static final class AsynchronousAction {
        private final Object key;
        private final String description;

        AsynchronousAction(Object key, String description) {
            this.key = key;
            this.description = description;
        }

        String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return "asynchronous action (" + description + ')';
        }
    }

    private static final class AsynchronousActionFailed {
        private final AsynchronousAction action;
        private final Throwable throwable;

        AsynchronousActionFailed(AsynchronousAction action, Throwable throwable) {
            this.action = action;
            this.throwable = throwable;
        }
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message instanceof AsynchronousAction) {
            finishAsynchronousAction((AsynchronousAction) message);
        } else if (message instanceof AsynchronousActionFailed) {
            AsynchronousActionFailed arguments = (AsynchronousActionFailed) message;
            asynchronousActionFailed(arguments.throwable, arguments.action);
        } else {
            unhandled(message);
        }
    }
}
