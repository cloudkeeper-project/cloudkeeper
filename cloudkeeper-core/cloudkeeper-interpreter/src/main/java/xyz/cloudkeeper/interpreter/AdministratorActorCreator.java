package xyz.cloudkeeper.interpreter;

import akka.actor.UntypedActor;
import akka.japi.Creator;

/**
 * Akka actor creator (factory) of CloudKeeper administrator actors.
 *
 * <p>The <em>administrator</em> in a CloudKeeper environment is responsible for listening to status updates by the
 * potentially remote workflow interpreter actors. A single administrator is shared by all workflow executions in a
 * CloudKeeper environment.
 *
 * @see CloudKeeperEnvironmentBuilder#CloudKeeperEnvironmentBuilder(scala.concurrent.ExecutionContext, akka.actor.ActorRef, akka.actor.ActorRef, akka.actor.ActorRef, xyz.cloudkeeper.model.api.staging.InstanceProvider, xyz.cloudkeeper.model.api.staging.StagingAreaProvider)
 */
public final class AdministratorActorCreator implements Creator<UntypedActor> {
    private static final long serialVersionUID = -35730749025625203L;

    private static final AdministratorActorCreator INSTANCE = new AdministratorActorCreator();

    private AdministratorActorCreator() { }

    private Object readResolve() {
        return INSTANCE;
    }

    public static AdministratorActorCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public UntypedActor create() {
        return new AdministratorActor();
    }
}
