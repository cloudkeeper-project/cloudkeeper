package xyz.cloudkeeper.model.api.executor;

import akka.japi.Option;
import xyz.cloudkeeper.model.api.ExecutionException;
import xyz.cloudkeeper.model.immutable.ParseException;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a simple-module execution, as provides by {@link SimpleModuleExecutor}.
 */
public final class SimpleModuleExecutorResult implements Serializable {
    private static final long serialVersionUID = 4088221416920116085L;

    private final Name executorName;
    @Nullable private final ExecutionException exception;
    private final Map<Name, Serializable> properties;

    private static final List<? extends Class<?>> VALUE_CLASSES = Collections.unmodifiableList(
        Arrays.asList(Boolean.class, Long.class, Double.class, String.class)
    );

    /**
     * This class is used to create simple-module executor results.
     */
    public static class Builder {
        private final Name executorName;
        @Nullable private ExecutionException exception;
        private final LinkedHashMap<Name, Serializable> properties = new LinkedHashMap<>();

        /**
         * Constructs a new builder with the given simple-module executor name.
         *
         * @param executorName name of the simple-module executor
         * @throws NullPointerException if the argument is null
         */
        public Builder(Name executorName) {
            Objects.requireNonNull(executorName);
            this.executorName = executorName;
        }

        /**
         * Sets the execution exception of this builder.
         *
         * @param exception the execution exception
         * @return this builder
         */
        public Builder setException(@Nullable ExecutionException exception) {
            this.exception = exception;
            return this;
        }

        /**
         * Adds the exception (if any) and the properties of the given execution result to this builder.
         *
         * @param executionResult the execution result
         * @return this builder
         */
        public Builder addExecutionResult(SimpleModuleExecutorResult executionResult) {
            Objects.requireNonNull(executionResult);
            if (executionResult.exception != null) {
                exception = executionResult.exception;
            }
            properties.putAll(executionResult.properties);
            return this;
        }

        /**
         * Adds a property to this builder.
         *
         * @param key simple name of the new property
         * @param value value of the new property, must be of type {@link Boolean}, {@link Long}, {@link Double}, or
         *     {@link String}
         * @return this builder
         */
        public Builder addProperty(SimpleName key, Object value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);

            if (!VALUE_CLASSES.contains(value.getClass())) {
                throw new IllegalArgumentException(String.format(
                    "Expected property values to be one of %s, but got %s.", VALUE_CLASSES, value)
                );
            }

            properties.put(executorName.join(key), (Serializable) value);
            return this;
        }

        public SimpleModuleExecutorResult build() {
            return new SimpleModuleExecutorResult(this);
        }
    }

    private SimpleModuleExecutorResult(Builder builder) {
        executorName = builder.executorName;
        exception = builder.exception;
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
    }

    private static String exceptionAsString(ExecutionException exception) {
        StringWriter stringWriter = new StringWriter(1024);
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        SimpleModuleExecutorResult other = (SimpleModuleExecutorResult) otherObject;
        return executorName.equals(other.executorName)
            && (
                exception == other.exception
                || (
                    exception != null
                    && other.exception != null
                    && exceptionAsString(exception).equals(exceptionAsString(other.exception))
               )
            )
            && properties.equals(other.properties);
    }

    @Override
    public int hashCode() {
        @Nullable String exceptionString = exception == null
            ? null
            : exceptionAsString(exception);
        return Objects.hash(executorName, exceptionString, properties);
    }

    public Option<ExecutionException> getExecutionException() {
        return exception == null
            ? Option.<ExecutionException>none()
            : Option.some(exception);
    }

    /**
     * Returns the binary name of the {@link SimpleModuleExecutor} class that created this instance.
     *
     * @return the binary name of the {@link SimpleModuleExecutor} class that created this instance; guaranteed to be
     *     valid according to {@link SourceVersion#isName}
     */
    public Name getExecutorName() {
        return executorName;
    }

    /**
     * Returns all properties of this module-execution result.
     *
     * @return properties of this module-execution result, all keys and values of the returned map are non-null
     */
    @SuppressWarnings("unchecked")
    public Map<Name, Object> getProperties() {
        return (Map<Name, Object>) (Map<Name, ?>) properties;
    }

    /**
     * Returns the value of the property with the given key, or {@code null} if not present.
     *
     * @param type class object representing the expected type of value
     * @param executorName name of executor that was passed to {@link Builder#Builder(Name)}
     * @param propertyName name of property that was passed to {@link Builder#addProperty(SimpleName, Object)}
     * @param <T> expected type of the value
     * @return the value or {@code null} if not present
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the first argument is not one of the expected types ({@link Boolean},
     *     {@link Long}, {@link Double}, or {@link String})
     * @throws ParseException if a property with the given key is present but of unexpected type
     */
    @Nullable
    public <T> T getProperty(Class<T> type, Name executorName, SimpleName propertyName) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(executorName);
        Objects.requireNonNull(propertyName);
        if (!VALUE_CLASSES.contains(type)) {
            throw new IllegalArgumentException(String.format(
                "Expected type that is one of %s, but got %s.", VALUE_CLASSES, type
            ));
        }

        Name key = executorName.join(propertyName);
        @Nullable Object value = properties.get(key);
        if (value == null) {
            return null;
        } else if (!type.isInstance(value)) {
            throw new ParseException(String.format(
                "Expected value for key '%s' to be of %s, but value is %s.", key, type, value
            ));
        }

        @SuppressWarnings("unchecked")
        T typedValue = (T) value;
        return typedValue;
    }
}
