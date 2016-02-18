package com.svbio.cloudkeeper.samples.maven;

import akka.actor.ActorSystem;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.maven.Bundles;
import com.svbio.cloudkeeper.maven.MavenRuntimeContextFactory;
import com.svbio.cloudkeeper.model.api.CloudKeeperEnvironment;
import com.svbio.cloudkeeper.model.api.WorkflowExecution;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.element.Version;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import com.svbio.cloudkeeper.simple.DSLExecutableProvider;
import com.svbio.cloudkeeper.simple.SimpleInstanceProvider;
import com.svbio.cloudkeeper.simple.SingleVMCloudKeeper;
import com.svbio.cloudkeeper.simple.WorkflowExecutions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class ModuleRunner {
    private static final class AetherConfiguration {
        /**
         * Only repository type supported by
         * {@link org.eclipse.aether.internal.impl.Maven2RepositoryLayoutFactory.Maven2RepositoryLayout}.
         */
        private static final String REPOSITORY_TYPE = "default";

        private final Path localRepositoryPath;
        private final boolean offline;
        private final ImmutableList<RemoteRepository> remoteRepositories;

        private AetherConfiguration(Config config) {
            localRepositoryPath = Paths.get(config.getString("local"));
            offline = config.getBoolean("offline");
            remoteRepositories = config.getConfigList("remote").stream()
                .map(
                    repoConfig
                        -> new RemoteRepository.Builder(null, REPOSITORY_TYPE, repoConfig.getString("url")).build()
                )
                .collect(ImmutableList.collector());
        }
    }

    private static final class Configuration {
        private final AetherConfiguration aetherConfiguration;
        private final URI dependency;
        private final Name module;
        private final SimpleName inPort;
        private final SimpleName outPort;
        private final long timeoutSeconds;

        private Configuration(Config config) {
            aetherConfiguration = new AetherConfiguration(config.getConfig("maven"));
            Config depConfig = config.getConfig("dependency");
            dependency = Bundles.bundleIdentifierFromMaven(depConfig.getString("groupid"),
                depConfig.getString("artifactid"), Version.valueOf(depConfig.getString("version")));
            module = Name.qualifiedName(config.getString("module"));
            inPort = SimpleName.identifier(config.getString("inport"));
            outPort = SimpleName.identifier(config.getString("outport"));
            timeoutSeconds = config.getLong("timeout");
        }
    }

    private ModuleRunner() { }

    static String runWithStringInput(Config config, String input) throws Exception {
        Configuration configuration = new Configuration(config.getConfig("com.svbio.cloudkeeper.samples"));
        AetherConfiguration aetherConfiguration = configuration.aetherConfiguration;
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        RepositorySystem repositorySystem = locator.getService(RepositorySystem.class);
        LocalRepository localRepository = new LocalRepository(aetherConfiguration.localRepositoryPath.toFile());
        DefaultRepositorySystemSession repositorySystemSession = MavenRepositorySystemUtils.newSession();
        repositorySystemSession.setLocalRepositoryManager(
            repositorySystem.newLocalRepositoryManager(repositorySystemSession, localRepository)
        );
        // For this example, let's always check for new versions of the Maven artifacts
        repositorySystemSession.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        repositorySystemSession.setOffline(aetherConfiguration.offline);

        ActorSystem actorSystem = ActorSystem.create("CloudKeeper-Runner");
        ExecutionContext executionContext = actorSystem.dispatcher();

        MavenRuntimeContextFactory runtimeContextFactory = new MavenRuntimeContextFactory.Builder(
                executionContext, repositorySystem, repositorySystemSession)
            .setRemoteRepositories(aetherConfiguration.remoteRepositories)
            .setExecutableProviderProvider(classLoader -> new DSLExecutableProvider(new ModuleFactory(classLoader)))
            .build();
        InstanceProvider instanceProvider = new SimpleInstanceProvider.Builder(executionContext)
            .setRuntimeContextFactory(runtimeContextFactory)
            .build();
        SingleVMCloudKeeper cloudKeeper = new SingleVMCloudKeeper.Builder()
            .setInstanceProvider(instanceProvider)
            .build();
        CloudKeeperEnvironment cloudKeeperEnvironment = cloudKeeper.newCloudKeeperEnvironmentBuilder().build();

        MutableModule<?> module = new MutableProxyModule()
            .setDeclaration(new MutableQualifiedNamable().setQualifiedName(configuration.module));

        WorkflowExecution workflowExecution = cloudKeeperEnvironment.newWorkflowExecutionBuilder(module)
            .setInputs(Collections.singletonMap(configuration.inPort, input))
            .setBundleIdentifiers(Collections.singletonList(configuration.dependency))
            .start();
        String output = (String) WorkflowExecutions.getOutputValue(
            workflowExecution, configuration.outPort.toString(), configuration.timeoutSeconds, TimeUnit.SECONDS);

        cloudKeeper.shutdown().awaitTermination();
        Await.result(actorSystem.terminate(), Duration.Inf());

        return output;
    }

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();
        String input;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            input = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        String output = runWithStringInput(config, input);
        System.out.println(output);
    }
}
