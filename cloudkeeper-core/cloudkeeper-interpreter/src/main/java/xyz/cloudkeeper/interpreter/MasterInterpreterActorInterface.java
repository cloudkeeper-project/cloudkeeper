package xyz.cloudkeeper.interpreter;

import xyz.cloudkeeper.model.api.RuntimeStateProvider;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Objects;

final class MasterInterpreterActorInterface {
    private MasterInterpreterActorInterface() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Request to master interpreter to create a workflow execution.
     *
     * <p>The workflow execution is created suspended, and will not start to run before a {@link StartExecution}
     * message.
     *
     * <p>The master interpreter will respond with a unique execution identifier of type {@link Long}.
     */
    static final class CreateExecution implements Serializable {
        private static final long serialVersionUID = 4594509318147885733L;

        private final String instanceProviderActorPath;
        private final RuntimeStateProvider runtimeStateProvider;
        private final InterpreterPropsProvider interpreterPropsProvider;
        private final InterpreterProperties executionProperties;
        private final BitSet updatedInPorts;

        CreateExecution(
            String instanceProviderActorPath,
            RuntimeStateProvider runtimeStateProvider,
            InterpreterPropsProvider interpreterPropsProvider,
            InterpreterProperties executionProperties,
            BitSet updatedInPorts
        ) {
            this.instanceProviderActorPath = Objects.requireNonNull(instanceProviderActorPath);
            this.runtimeStateProvider = Objects.requireNonNull(runtimeStateProvider);
            this.interpreterPropsProvider = Objects.requireNonNull(interpreterPropsProvider);
            this.executionProperties = Objects.requireNonNull(executionProperties);
            this.updatedInPorts = (BitSet) Objects.requireNonNull(updatedInPorts).clone();
        }

        String getInstanceProviderActorPath() {
            return instanceProviderActorPath;
        }

        RuntimeStateProvider getRuntimeStateProvider() {
            return runtimeStateProvider;
        }

        InterpreterPropsProvider getInterpreterPropsProvider() {
            return interpreterPropsProvider;
        }

        InterpreterProperties getExecutionProperties() {
            return executionProperties;
        }

        BitSet getUpdatedInPorts() {
            return (BitSet) updatedInPorts.clone();
        }
    }

    /**
     * Starts execution of a workflow that was previously created using {@link CreateExecution}.
     */
    static final class StartExecution implements Serializable {
        private static final long serialVersionUID = 463675157946216340L;

        private final long executionId;

        StartExecution(long executionId) {
            this.executionId = executionId;
        }

        public long getExecutionId() {
            return executionId;
        }
    }

    /**
     * Stops the workflow with the given execution ID.
     */
    static final class CancelWorkflow implements Serializable {
        private static final long serialVersionUID = -6005414802781741655L;

        private final long executionId;
        private final Throwable throwable;

        CancelWorkflow(long executionId, Throwable throwable) {
            this.executionId = executionId;
            this.throwable = throwable;
        }

        public long getExecutionId() {
            return executionId;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
