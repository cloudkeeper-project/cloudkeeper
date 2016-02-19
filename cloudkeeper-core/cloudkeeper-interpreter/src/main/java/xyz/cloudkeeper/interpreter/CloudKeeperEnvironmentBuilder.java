package xyz.cloudkeeper.interpreter;

import akka.actor.ActorRef;
import akka.util.Timeout;
import scala.concurrent.ExecutionContext;
import xyz.cloudkeeper.model.api.CloudKeeperEnvironment;
import xyz.cloudkeeper.model.api.RuntimeContext;
import xyz.cloudkeeper.model.api.RuntimeContextFactory;
import xyz.cloudkeeper.model.api.WorkflowExecution;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.StagingAreaProvider;
import xyz.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to create CloudKeeper environments.
 */
public final class CloudKeeperEnvironmentBuilder {
    /**
     * Default path for the actor that responds to messages of type
     * {@link InstanceProviderActorInterface.GetInstanceProviderMessage}.
     */
    public static final String DEFAULT_INSTANCE_PROVIDER_ACTOR_PATH = "cloudkeeper/instance-provider";

    private final ExecutionContext executionContext;
    private String instanceProviderActorPath = DEFAULT_INSTANCE_PROVIDER_ACTOR_PATH;
    private final InstanceProvider instanceProvider;
    private InterpreterPropsProvider interpreterPropsProvider = DefaultInterpreterPropsProvider.INSTANCE;
    private final StagingAreaProvider stagingAreaProvider;
    private final ActorRef administrator;
    private final ActorRef masterInterpreter;
    private final ActorRef executor;
    private boolean cleaningRequested = true;
    private boolean retrieveResults = true;

    /**
     * @see #setRemoteAskTimeout
     */
    private Timeout remoteAskTimeout = new Timeout(1, TimeUnit.MINUTES);

    /**
     * @see #setLocalAskTimeout
     */
    private Timeout localAskTimeout = new Timeout(1, TimeUnit.SECONDS);
    private ImmutableList<EventSubscription> eventSubscriptions = ImmutableList.of();

    /**
     * Constructor.
     *
     * <p>Note that this constructor only sets the instance provider that will be used to create
     * {@link WorkflowExecution} instances (which will always happen locally). The
     * interpreter and executors will instead query the instance-provider actor (configurable with
     * {@link #setInstanceProviderActorPath(String)}) for an {@link InstanceProvider} instance.
     *
     * @param executionContext the execution context that the CloudKeeper workflow execution will use for scheduling
     *     internal asynchronous tasks (futures)
     * @param administrator actor in the current JVM that will listen to messages from the master interpreter and its
     *     delegates
     * @param masterInterpreter Master interpreter that does not have to reside in the current JVM. This actor will
     *     be sent workflow execution requests.
     * @param executor Simple-module executor actor that does not have to reside in the current JVM. The actor
     *     reference will be included in the workflow execution request sent to {@code masterInterpreter}.
     * @param instanceProvider instance provider that provides the {@link RuntimeContextFactory} and that will be passed
     *     to the staging-area provider (see
     *     {@link StagingAreaProvider#provideStaging(RuntimeContext, RuntimeAnnotatedExecutionTrace, InstanceProvider)})
     * @param stagingAreaProvider staging-area provider
     */
    public CloudKeeperEnvironmentBuilder(ExecutionContext executionContext, ActorRef administrator,
            ActorRef masterInterpreter, ActorRef executor, InstanceProvider instanceProvider,
            StagingAreaProvider stagingAreaProvider) {
        this.executionContext = Objects.requireNonNull(executionContext);
        this.administrator = Objects.requireNonNull(administrator);
        this.masterInterpreter = Objects.requireNonNull(masterInterpreter);
        this.executor = Objects.requireNonNull(executor);
        this.instanceProvider = Objects.requireNonNull(instanceProvider);
        this.stagingAreaProvider = Objects.requireNonNull(stagingAreaProvider);
    }

    /**
     * Sets the purely local actor path that interpreter actors will send
     * {@link InstanceProviderActorInterface.GetInstanceProviderMessage} to.
     *
     * <p>By default, the path is {@link #DEFAULT_INSTANCE_PROVIDER_ACTOR_PATH}.
     *
     * @param instanceProviderActorPath purely local actor path
     * @return this builder
     */
    public CloudKeeperEnvironmentBuilder setInstanceProviderActorPath(String instanceProviderActorPath) {
        this.instanceProviderActorPath = Objects.requireNonNull(instanceProviderActorPath);
        return this;
    }

    /**
     * Sets the actor-creator provider.
     *
     * <p>By default, {@link DefaultInterpreterPropsProvider} is used.
     *
     * @param interpreterPropsProvider actor-creator provider
     * @return this builder
     */
    CloudKeeperEnvironmentBuilder setInterpreterPropsProvider(InterpreterPropsProvider interpreterPropsProvider) {
        this.interpreterPropsProvider = Objects.requireNonNull(interpreterPropsProvider);
        return this;
    }

    /**
     * Sets whether intermediate results should be cleaned as soon as possible.
     *
     * <p>By default, this option is set to {@code true}.
     *
     * @param cleaningRequested whether intermediate results should be cleaned as soon as possible
     * @return this builder
     */
    public CloudKeeperEnvironmentBuilder setCleaningRequested(boolean cleaningRequested) {
        this.cleaningRequested = cleaningRequested;
        return this;
    }

    /**
     * Sets whether results should be retrieved and made available through {@link WorkflowExecution#getOutput(String)}.
     *
     * <p>If this option is set to {@code false}, the futures returned by {@link WorkflowExecution#getOutput(String)}
     * will always be completed exceptionally with a {@link xyz.cloudkeeper.model.api.WorkflowExecutionException}
     * once the out-port value is available in the staging area.
     *
     * <p>By default, this option is set to {@code true}.
     *
     * @param retrieveResults whether results should be retrieved
     * @return this builder
     */
    public CloudKeeperEnvironmentBuilder setRetrieveResults(boolean retrieveResults) {
        this.retrieveResults = retrieveResults;
        return this;
    }

    /**
     * Sets the timeout for receiving answers from from potentially remote actors.
     *
     * <p>The master interpreter may run on a different JVM and is therefore potentially remote. By default, the remote
     * timeout is one minute.
     *
     * @param remoteAskTimeout timeout for receiving answers from potentially remote actors
     * @return this builder
     */
    public CloudKeeperEnvironmentBuilder setRemoteAskTimeout(Timeout remoteAskTimeout) {
        this.remoteAskTimeout = Objects.requireNonNull(remoteAskTimeout);
        return this;
    }

    /**
     * Sets the timeout for receiving answers from from local actors.
     *
     * <p>By default, the timeout is one minute.
     *
     * @param localAskTimeout timeout for receiving answers from local actors
     * @return this builder
     */
    public CloudKeeperEnvironmentBuilder setLocalAskTimeout(Timeout localAskTimeout) {
        this.localAskTimeout = Objects.requireNonNull(localAskTimeout);
        return this;
    }

    /**
     * Sets the event subscriptions, containing the actors that will receive workflow-interpretation event messages.
     *
     * <p>The builder does not keep a reference to the given list; that is, subsequent modifications to the given list
     * will not have any effect on the builder. By default, the list of event subscriptions is empty.
     *
     * @return this builder
     */
    public CloudKeeperEnvironmentBuilder setEventListeners(List<EventSubscription> eventSubscriptions) {
        this.eventSubscriptions = ImmutableList.copyOf(eventSubscriptions);
        return this;
    }

    /**
     * Returns a new CloudKeeper environment using the attributes of this builder.
     *
     * @return new CloudKeeper environment
     */
    public CloudKeeperEnvironment build() {
        return new CloudKeeperEnvironmentImpl(executionContext, instanceProviderActorPath, instanceProvider,
            interpreterPropsProvider, stagingAreaProvider, administrator, masterInterpreter, executor,
            eventSubscriptions, cleaningRequested, retrieveResults, remoteAskTimeout, localAskTimeout);
    }
}
