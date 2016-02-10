package com.svbio.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Top-level interpreter.
 */
final class MasterInterpreterActor extends UntypedActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), (UntypedActor) this);

    private final Map<ActorRef, Cancellable> scheduledTerminations = new HashMap<>();
    private long nextExecutionId = 0;

    MasterInterpreterActor(long nextExecutionId) {
        this.nextExecutionId = nextExecutionId;
    }

    void createExecution(MasterInterpreterActorInterface.CreateExecution createExecution) {
        final long executionId = nextExecutionId;
        ++nextExecutionId;
        Props props = Props.create(new TopLevelInterpreterActor.Factory(executionId, null, createExecution));
        getContext().actorOf(props, String.valueOf(executionId));
        getSender().tell(executionId, getSelf());
    }

    void startExecution(long executionId) {
        // From the UntypedActorContext:
        // getChild() "returns a reference to the named child or null if no child with that name exists"
        ActorRef child = getContext().getChild(String.valueOf(executionId));
        if (child != null) {
            child.tell(TopLevelInterpreterActorInterface.Start.INSTANCE, getSelf());
        } else {
            log.warning("Request to start unknown execution {}.", executionId);
        }
    }

    void cancel(long executionId, Throwable throwable) {
        ActorRef child = getContext().getChild(String.valueOf(executionId));
        if (child != null) {
            if (!scheduledTerminations.containsKey(child)) {
                getContext().watch(child);
                child.tell(new Status.Failure(throwable), getSelf());

                // Give the top-level interpreter some time to finish. Otherwise, we will terminate it after a timeout.
                Cancellable scheduledTermination = getContext().system().scheduler().scheduleOnce(
                    Duration.create(1, TimeUnit.MINUTES),
                    child,
                    PoisonPill.getInstance(),
                    getContext().dispatcher(),
                    getSelf()
                );
                scheduledTerminations.put(child, scheduledTermination);
            }
        } else {
            log.warning("Request to cancel unknown execution {} because of: {}", executionId, throwable);
        }
    }

    void terminated(ActorRef child) {
        Cancellable scheduledTermination = scheduledTerminations.get(child);
        if (scheduledTermination != null) {
            // The child terminated in time, so we should cancel the scheduled termination.
            scheduledTermination.cancel();
        }
    }

    private static final SupervisorStrategy SUPERVISOR_STRATEGY
        = new OneForOneStrategy(0, Duration.Inf(), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable throwable) {
            if (throwable instanceof Exception) {
                return SupervisorStrategy.stop();
            } else {
                return SupervisorStrategy.escalate();
            }
        }
    });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return SUPERVISOR_STRATEGY;
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof MasterInterpreterActorInterface.CreateExecution) {
            createExecution((MasterInterpreterActorInterface.CreateExecution) message);
        } else if (message instanceof MasterInterpreterActorInterface.StartExecution) {
            startExecution(((MasterInterpreterActorInterface.StartExecution) message).getExecutionId());
        } else if (message instanceof MasterInterpreterActorInterface.CancelWorkflow) {
            MasterInterpreterActorInterface.CancelWorkflow arguments
                = (MasterInterpreterActorInterface.CancelWorkflow) message;
            cancel(arguments.getExecutionId(), arguments.getThrowable());
        } else if (message instanceof Terminated) {
            terminated(((Terminated) message).getActor());
        } else {
            unhandled(message);
        }
    }
}
