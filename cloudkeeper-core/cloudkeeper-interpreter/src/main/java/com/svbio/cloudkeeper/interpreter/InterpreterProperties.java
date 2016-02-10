package com.svbio.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import java.io.Serializable;
import java.util.Objects;

/**
 * Interpreter properties.
 *
 * <p>Properties needed for interpreting a workflow. This base class only contains a subset of the properties of
 * {@link CloudKeeperEnvironmentImpl}, all of which are serializable and of the same value across JVM boundaries.
 */
class InterpreterProperties implements Serializable {
    private static final long serialVersionUID = -8018556844836990193L;

    private final boolean cleaningRequested;
    private final ActorRef administrator;
    private final ActorRef executor;
    private final ImmutableList<EventSubscription> eventSubscriptions;

    InterpreterProperties(InterpreterProperties original) {
        this(original.cleaningRequested, original.administrator, original.executor, original.eventSubscriptions);
    }

    InterpreterProperties(boolean cleaningRequested, ActorRef administrator, ActorRef executor,
            ImmutableList<EventSubscription> eventSubscriptions) {
        Objects.requireNonNull(administrator);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(eventSubscriptions);

        this.cleaningRequested = cleaningRequested;
        this.administrator = administrator;
        this.executor = executor;
        this.eventSubscriptions = ImmutableList.copyOf(eventSubscriptions);
    }

    /**
     * Returns whether the interpreter component should delete intermediate results.
     */
    boolean isCleaningRequested() {
        return cleaningRequested;
    }

    ActorRef getAdministrator() {
        return administrator;
    }

    ActorRef getExecutor() {
        return executor;
    }

    ImmutableList<EventSubscription> getEventSubscriptions() {
        return eventSubscriptions;
    }
}
