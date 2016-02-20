package xyz.cloudkeeper.simple;

import scala.concurrent.ExecutionContext;
import xyz.cloudkeeper.model.api.RuntimeContextFactory;
import xyz.cloudkeeper.model.api.staging.InstanceProvider;
import xyz.cloudkeeper.model.api.staging.InstanceProvisionException;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Simple instance provider for {@link RuntimeContextFactory} and {@link ExecutionContext} instances.
 */
public final class SimpleInstanceProvider implements InstanceProvider {
    private final Executor executor;
    private final RuntimeContextFactory runtimeContextFactory;

    private SimpleInstanceProvider(Builder builder) {
        executor = builder.executor;
        runtimeContextFactory = builder.runtimeContextFactory == null
            ? new DSLRuntimeContextFactory.Builder(executor).build()
            : builder.runtimeContextFactory;
    }

    public static final class Builder {
        @Nullable private RuntimeContextFactory runtimeContextFactory = null;
        private final Executor executor;

        /**
         * Constructor.
         *
         * @param executor Executor that will be returned by the instance provider. If no bundle loader will be set, the
         *     execution context will also be used for constructing a {@link DSLRuntimeContextFactory}.
         */
        public Builder(Executor executor) {
            this.executor = Objects.requireNonNull(executor);
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
        } else if (Executor.class.equals(requestedClass)) {
            return (T) executor;
        } else {
            throw new InstanceProvisionException(String.format(
                "Cannot provide instance of %s.", requestedClass
            ));
        }
    }
}
