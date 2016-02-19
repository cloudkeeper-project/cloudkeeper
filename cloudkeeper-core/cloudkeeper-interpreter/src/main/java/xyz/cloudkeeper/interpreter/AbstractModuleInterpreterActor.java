package xyz.cloudkeeper.interpreter;

import akka.actor.AllForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import scala.concurrent.duration.Duration;
import xyz.cloudkeeper.interpreter.event.BeginExecutionTraceEvent;
import xyz.cloudkeeper.interpreter.event.EndExecutionTraceEvent;
import xyz.cloudkeeper.interpreter.event.EndSimpleModuleTraceEvent;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.util.Objects;

abstract class AbstractModuleInterpreterActor extends AbstractActor {
    private final LocalInterpreterProperties interpreterProperties;
    private final RuntimeExecutionTrace absoluteTrace;
    private final int moduleId;

    AbstractModuleInterpreterActor(LocalInterpreterProperties interpreterProperties,
            RuntimeExecutionTrace absoluteTrace, int moduleId) {
        super(interpreterProperties.getAsyncTaskContext());
        Objects.requireNonNull(absoluteTrace);

        this.interpreterProperties = interpreterProperties;
        this.absoluteTrace = ExecutionTrace.copyOf(absoluteTrace);
        this.moduleId = moduleId;
    }

    final LocalInterpreterProperties getInterpreterProperties() {
        return interpreterProperties;
    }

    final RuntimeExecutionTrace getAbsoluteTrace() {
        return absoluteTrace;
    }

    final int getModuleId() {
        return moduleId;
    }

    /**
     * Handles event that an in-port of the module represented by this actor received a value.
     *
     * @param inPortId index of the in-port in
     *     {@link xyz.cloudkeeper.model.runtime.element.module.RuntimeModule#getInPorts()}
     */
    abstract void inPortHasSignal(int inPortId);

    final void publishStartModule() {
        interpreterProperties.getEventBus().publish(
            BeginExecutionTraceEvent.of(
                interpreterProperties.getExecutionId(), System.currentTimeMillis(), absoluteTrace
            )
        );
    }

    final void publishStopModule(boolean successful) {
        interpreterProperties.getEventBus().publish(
            EndExecutionTraceEvent.of(
                interpreterProperties.getExecutionId(), System.currentTimeMillis(), absoluteTrace, successful
            )
        );
    }

    final void publishStopSimpleModule(@Nullable SimpleModuleExecutorResult result) {
        interpreterProperties.getEventBus().publish(
            EndSimpleModuleTraceEvent.of(
                interpreterProperties.getExecutionId(), System.currentTimeMillis(), absoluteTrace, result
            )
        );
    }

    @Override
    protected final void asynchronousActionFailed(String message, Throwable cause) throws InterpreterException {
        throw new InterpreterException(absoluteTrace, message, cause);
    }

    private static void childActorFailed(InterpreterException exception) throws InterpreterException {
        throw exception;
    }

    @Override
    public void onReceive(Object message) throws InterpreterException {
        if (message instanceof InterpreterInterface.InPortHasSignal) {
            inPortHasSignal(((InterpreterInterface.InPortHasSignal) message).getInPortId());
        } else if (message instanceof ChildActorFailed) {
            childActorFailed(((ChildActorFailed) message).exception);
        } else {
            super.onReceive(message);
        }
    }

    /**
     * Returns an {@link InterpreterException} that wraps the given exception that occurred while a child actor
     * processed a message.
     *
     * <p>As the Akka documentation states (Akka v2.3.0, Section 3.3.2, Logging of Actor Failures): "Customized logging
     * can be done inside the Decider. Note that the reference to the currently failed child is available as the
     * {@link UntypedActor#getSender()}} when the {@link SupervisorStrategy} is declared inside the supervising actor."
     *
     * <p>Sublcasses that override this method are allowed but not required to call
     * {@code super.mapChildException(exception)}. The default implementation returns an {@link InterpreterException}
     * for the execution trace of the current module and with a message stating that an unknown child actor failed.
     *
     * @param exception the exception that occurred while a child actor processed a message
     * @return the execution exception that will be used as reason to stop the module interpretation; guaranteed not
     *     null
     */
    InterpreterException mapChildException(Exception exception) {
        return new InterpreterException(
            absoluteTrace,
            String.format("Failure of unknown child actor %s.", getSender()),
            exception
        );
    }

    /**
     * Returns how to handle the given fault that occurred in a child actor.
     */
    private SupervisorStrategy.Directive supervisorDirective(Throwable throwable) {
        if (throwable instanceof InterpreterException) {
            return SupervisorStrategy.escalate();
        } else if (throwable instanceof Exception) {
            InterpreterException interpreterException = mapChildException((Exception) throwable);
            getSelf().tell(new ChildActorFailed(interpreterException), getSelf());
            return SupervisorStrategy.stop();
        } else {
            return SupervisorStrategy.escalate();
        }
    }

    /**
     * Supervisor strategy.
     *
     * @see #supervisorStrategy()
     */
    private final SupervisorStrategy supervisorStrategy
        = new AllForOneStrategy(0, Duration.Inf(), this::supervisorDirective, false);

    /**
     * Returns the supervisor strategy used by this actor.
     *
     * <p>The supervisor strategy of an interpreter actor is always an all-for-one-strategy; that is, all child actors
     * will be stopped in case of a failure. If the {@link Throwable} that caused the failure is a
     * {@link InterpreterException} or a {@link Throwable} that is not an {@link Exception} the failure will be
     * escalated (that is, this actor will fail itself). Otherwise, {@link #mapChildException(Exception)} will be called
     * to translate the exception into a {@link InterpreterException}, and a {@link ChildActorFailed} instance will be
     * sent to this actor with the translated exception as cause. The failure will then be handled by
     * {@link #childActorFailed}.
     *
     * <p>The strategy deactivates the Akka-provided logging, which logs all exception as errors by default. Instead,
     * this strategy relies on the fact that there is always a {@link TopLevelInterpreterActor} in the supervision
     * hierarchy, and this actor is guaranteed to reside on the same machine. The exception is logged there. See
     * {@link TopLevelInterpreterActor#supervisorStrategy()}.
     *
     * @see #mapChildException(Exception)
     */
    @Override
    public final SupervisorStrategy supervisorStrategy() {
        return supervisorStrategy;
    }

    private static final class ChildActorFailed {
        private final InterpreterException exception;

        ChildActorFailed(InterpreterException exception) {
            this.exception = exception;
        }
    }
}
