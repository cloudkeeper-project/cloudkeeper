package com.svbio.cloudkeeper.interpreter;

import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.svbio.cloudkeeper.model.api.Executable;
import com.svbio.cloudkeeper.model.api.executor.SimpleModuleExecutor;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * Akka actor creator (factory) of CloudKeeper simple-module executors.
 *
 * <p>The <em>executor</em> in a CloudKeeper environment is responsible for processing simple modules. Simple modules
 * are atomic to the CloudKeeper interpreter, that is, they are eventually processed as a simple Java method call to
 * {@link Executable#run(com.svbio.cloudkeeper.model.api.ModuleConnector)}. An
 * executor takes care of all intermediate steps, which includes dispatching the simple-module representation to the
 * target machine, recreating the staging area, creating a {@link com.svbio.cloudkeeper.model.api.ModuleConnector}
 * object, creating an {@link Executable} object, etc.
 *
 * <p>Note: This actor creator cannot be serialized.
 *
 * @see CloudKeeperEnvironmentBuilder#CloudKeeperEnvironmentBuilder(scala.concurrent.ExecutionContext, akka.actor.ActorRef, akka.actor.ActorRef, akka.actor.ActorRef, com.svbio.cloudkeeper.model.api.staging.InstanceProvider, com.svbio.cloudkeeper.model.api.staging.StagingAreaProvider)
 */
public final class ExecutorActorCreator implements Creator<UntypedActor> {
    private static final long serialVersionUID = 2943465418830832802L;

    private final SimpleModuleExecutor simpleModuleExecutor;

    /**
     * Constructs a new executor actor that provides a message interface for the given simple-module executor interface.
     *
     * @param simpleModuleExecutor user-defined simple-module executor
     */
    public ExecutorActorCreator(SimpleModuleExecutor simpleModuleExecutor) {
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
