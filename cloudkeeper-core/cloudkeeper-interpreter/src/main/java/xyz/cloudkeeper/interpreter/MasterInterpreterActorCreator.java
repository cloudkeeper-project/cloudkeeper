package xyz.cloudkeeper.interpreter;

import akka.actor.UntypedActor;
import akka.japi.Creator;

/**
 * Akka actor creator (factory) of CloudKeeper master-interpreter actors.
 *
 * <p>The <em>master interpreter</em> in a CloudKeeper environment is responsible for listening to workflow execution
 * requests. As a result of a new execution request, the master interpreter creates a new <em>top-level interpreter</em>
 * actor under its supervision. The name of this actor will be the {@link String} representation of the new execution
 * ID. Depending on the Akka actor deployment configuration, the top-level interpreter may be created on a remote JVM.
 *
 * <p>The top-level interpreter creates a module interpreter actor for the root module, which in turn may recursively
 * create actors for child modules. All descendants of the top-level interpreter are created on the same JVM (and any
 * other Akka actor configuration would fail).
 *
 * @see CloudKeeperEnvironmentBuilder#CloudKeeperEnvironmentBuilder(scala.concurrent.ExecutionContext, akka.actor.ActorRef, akka.actor.ActorRef, akka.actor.ActorRef, xyz.cloudkeeper.model.api.staging.InstanceProvider, xyz.cloudkeeper.model.api.staging.StagingAreaProvider)
 */
public final class MasterInterpreterActorCreator implements Creator<UntypedActor> {
    private static final long serialVersionUID = -2847877281225806067L;

    private final long firstExecutionId;

    public MasterInterpreterActorCreator(long firstExecutionId) {
        this.firstExecutionId = firstExecutionId;
    }

    @Override
    public UntypedActor create() {
        return new MasterInterpreterActor(firstExecutionId);
    }
}
