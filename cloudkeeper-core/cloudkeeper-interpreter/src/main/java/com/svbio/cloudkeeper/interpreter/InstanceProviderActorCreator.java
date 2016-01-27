package com.svbio.cloudkeeper.interpreter;

import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * Akka actor creator for instance-provider actor.
 *
 * <p>Note: This actor creator cannot be serialized.
 */
public final class InstanceProviderActorCreator implements Creator<UntypedActor> {
    private static final long serialVersionUID = -2129812853117758774L;

    private final InstanceProvider instanceProvider;

    public InstanceProviderActorCreator(InstanceProvider instanceProvider) {
        this.instanceProvider = Objects.requireNonNull(instanceProvider);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        throw new NotSerializableException(getClass().getName());
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        throw new NotSerializableException(getClass().getName());
    }

    @Override
    public UntypedActor create() {
        return new InstanceProviderActor(this);
    }

    private static final class InstanceProviderActor extends UntypedActor {
        private final InstanceProvider instanceProvider;

        private InstanceProviderActor(InstanceProviderActorCreator creator) {
            assert creator != null && creator.instanceProvider != null;

            instanceProvider = creator.instanceProvider;
        }

        private void getInstanceProvider() {
            getSender().tell(instanceProvider, getSelf());
        }

        @Override
        public void onReceive(Object message) {
            if (message instanceof InstanceProviderActorInterface.GetInstanceProviderMessage) {
                getInstanceProvider();
            } else {
                unhandled(message);
            }
        }
    }
}
