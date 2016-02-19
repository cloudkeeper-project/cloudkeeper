package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import xyz.cloudkeeper.interpreter.event.Event;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

public final class EventSubscription implements Serializable {
    private static final long serialVersionUID = 8783766108586995731L;

    private final ActorRef actorRef;
    private final Class<?> classifier;

    public EventSubscription(ActorRef actorRef) {
        this(actorRef, Event.class);
    }

    public EventSubscription(ActorRef actorRef, Class<? extends Event> classifier) {
        Objects.requireNonNull(actorRef);
        Objects.requireNonNull(classifier);

        this.actorRef = actorRef;
        this.classifier = classifier;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        EventSubscription other = (EventSubscription) otherObject;
        return actorRef.equals(other.actorRef)
            && classifier.equals(other.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorRef, classifier);
    }

    @Override
    public String toString() {
        return String.format("event subscription: %s for %s", actorRef, classifier);
    }

    public ActorRef getActorRef() {
        return actorRef;
    }

    public Class<?> getClassifier() {
        return classifier;
    }
}
