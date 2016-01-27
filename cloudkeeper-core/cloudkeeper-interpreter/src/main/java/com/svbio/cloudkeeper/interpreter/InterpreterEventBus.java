package com.svbio.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.event.japi.SubchannelEventBus;
import akka.util.Subclassification;
import com.svbio.cloudkeeper.interpreter.event.Event;

/**
 * Event bus for interpreter events.
 *
 * <p>This event bus has events of type {@link Event}, subscribers of type
 * {@link ActorRef}, and classifiers of type {@link Class}.
 */
final class InterpreterEventBus extends SubchannelEventBus<Event, ActorRef, Class<?>> {
    enum Subclassifier implements Subclassification<Class<?>> {
        INSTANCE;

        /**
         * Returns if {@code classifier} and {@code otherClassifier} are the same class.
         */
        @Override
        public boolean isEqual(Class<?> classifier, Class<?> otherClassifier) {
            return classifier.equals(otherClassifier);
        }

        /**
         * Returns if {@code classifier} is a subclass of {@code otherClassifier}; equal classes are sub-classes.
         */
        @Override
        public boolean isSubclass(Class<?> classifier, Class<?> otherClassifier) {
            return otherClassifier.isAssignableFrom(classifier);
        }
    }

    @Override
    public Subclassification<Class<?>> subclassification() {
        return Subclassifier.INSTANCE;
    }

    @Override
    public Class<?> classify(Event event) {
        return event.getClass();
    }

    @Override
    public void publish(Event event, ActorRef actorRef) {
        actorRef.tell(event, ActorRef.noSender());
    }
}
