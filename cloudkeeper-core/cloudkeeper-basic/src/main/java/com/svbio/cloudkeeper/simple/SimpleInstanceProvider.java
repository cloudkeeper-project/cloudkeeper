package com.svbio.cloudkeeper.simple;

import com.svbio.cloudkeeper.model.api.RuntimeContextFactory;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvider;
import com.svbio.cloudkeeper.model.api.staging.InstanceProvisionException;
import scala.concurrent.ExecutionContext;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Simple instance provider for {@link RuntimeContextFactory} and {@link ExecutionContext} instances.
 */
public final class SimpleInstanceProvider implements InstanceProvider {
    private final ExecutionContext executionContext;
    private final RuntimeContextFactory runtimeContextFactory;

    private SimpleInstanceProvider(Builder builder) {
        executionContext = builder.executionContext;
        runtimeContextFactory = builder.runtimeContextFactory == null
            ? new DSLRuntimeContextFactory.Builder(executionContext).build()
            : builder.runtimeContextFactory;
    }

    public static final class Builder {
        @Nullable private RuntimeContextFactory runtimeContextFactory = null;
        private final ExecutionContext executionContext;

        /**
         * Constructor.
         *
         * @param executionContext Execution context that will be returned by the instance provider. If no bundle loader
         *     will be set, the execution context will also be used for constructing a {@link DSLRuntimeContextFactory}.
         */
        public Builder(ExecutionContext executionContext) {
            this.executionContext = Objects.requireNonNull(executionContext);
        }

        /**
         * Sets the runtime-context provider of this builder.
         *
         * <p>By default, a newly created {@link DSLRuntimeContextFactory} will be used.
         *
         * @param runtimeContextFactory runtime-context provider
         * @return this builder
         */
        public Builder setRuntimeContextFactory(RuntimeContextFactory runtimeContextFactory) {
            this.runtimeContextFactory = Objects.requireNonNull(runtimeContextFactory);
            return this;
        }

        public SimpleInstanceProvider build() {
            return new SimpleInstanceProvider(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(Class<T> requestedClass) throws InstanceProvisionException {
        if (RuntimeContextFactory.class.equals(requestedClass)) {
            return (T) runtimeContextFactory;
        } else if (ExecutionContext.class.equals(requestedClass)) {
            return (T) executionContext;
        } else {
            throw new InstanceProvisionException(String.format(
                "Cannot provide instance of %s.", requestedClass
            ));
        }
    }
}
