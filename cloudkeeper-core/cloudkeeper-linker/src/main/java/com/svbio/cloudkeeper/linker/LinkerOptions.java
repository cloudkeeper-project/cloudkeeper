package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.api.UnmarshalContext;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Class for configuring how to link and verify abstract syntax trees (the bare model).
 *
 * <p>Objects of this class are used with the
 * {@link Linker#createAnnotatedExecutionTrace(BareExecutionTrace, BareModule, List, RuntimeRepository, LinkerOptions)}
 * and {@link Linker#createRepository(List, LinkerOptions)} methods to configure how instances are linked and verified.
 *
 * <p>Instances of this class are shallow immutable.
 */
public final class LinkerOptions {
    private static final LinkerOptions NON_EXECUTABLE = new Builder().build();

    private final ClassProvider classProvider;
    private final ExecutableProvider executableProvider;
    private final boolean serializeValues;
    private final boolean deserializeSerializationTrees;
    @Nullable private final ClassLoader unmarshalClassLoader;

    /**
     * Builder of {@link LinkerOptions} instances.
     */
    public static class Builder {
        private ClassProvider classProvider
            = name -> Optional.of(
                Class.forName(name.getBinaryName().toString(), true, Thread.currentThread().getContextClassLoader())
            );
        private ExecutableProvider executableProvider = ignoredName -> Optional.empty();
        private boolean marshalValues = false;
        private boolean deserializeSerializationTrees = false;
        @Nullable private ClassLoader unmarshalClassLoader;

        /**
         * Sets the class provider that will be used to resolve qualified names of plug-in declarations into
         * {@link Class} instances.
         *
         * <p>This option is only relevant for linker method {@link Linker#createRepository(List, LinkerOptions)}.
         *
         * <p>If a class provider does not return a {@link Class} instance for a given name, then calls to any of the
         * following methods for the respective declaration will throw an {@link IllegalStateException}:
         * <ul>><li>
         *     {@link com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration#toClass()}
         * </li><li>
         *     any method of the {@link com.svbio.cloudkeeper.model.api.Marshaler} returned by
         *     {@link com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationDeclaration#getInstance()}
         * </li></ul>
         *
         * <p>The given class provider is independent of the class loader returned by
         * {@link UnmarshalContext#getClassLoader()} (relevant only if
         * {@link #setDeserializeSerializationTrees(boolean)} has been configured with {@code true}). That class loader
         * can be configured using {@link #setUnmarshalClassLoader(ClassLoader)} instead.
         *
         * <p>By default, a class provider will be used that, given name {@code name}, returns the equivalent of
         * {@code Class.forName(name.getBinaryName().toString(), true, Thread.currentThread().getContextClassLoader())}.
         *
         * @param classProvider class provider that will be used to resolve qualified names of plug-in declarations into
         *     {@link Class} instances
         * @return this builder
         */
        public Builder setClassProvider(ClassProvider classProvider) {
            Objects.requireNonNull(classProvider);
            this.classProvider = classProvider;
            return this;
        }

        /**
         * Sets the executable provider used to resolve qualified names of simple-module declaration into
         * {@link com.svbio.cloudkeeper.model.api.Executable} instances.
         *
         * <p>This option is only relevant for linker method {@link Linker#createRepository(List, LinkerOptions)}.
         *
         * <p>If an executable provider does not return an {@link com.svbio.cloudkeeper.model.api.Executable} instance
         * for a given name, then calls to
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeSimpleModuleDeclaration#toExecutable()} for
         * the respective declaration will raise an {@link IllegalStateException}.
         *
         * <p>By default, an executable provider will be used that <em>never</em> returns an
         * {@link com.svbio.cloudkeeper.model.api.Executable} instance, but always an empty {@link Optional}.
         *
         * @param executableProvider executable provider that will be used to resolve qualified names of simple-module
         *     declarations into {@link com.svbio.cloudkeeper.model.api.Executable} instances
         * @return this builder
         */
        public Builder setExecutableProvider(ExecutableProvider executableProvider) {
            Objects.requireNonNull(executableProvider);
            this.executableProvider = executableProvider;
            return this;
        }

        /**
         * Sets that the value of {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule#getRaw()}
         * should be computed (by serializing
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule#getValue()}) if the
         * corresponding property is not available in the bare model given to the linker.
         *
         * <p>If this option is true, method
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule#getRaw()} is guaranteed to
         * return non-null results.
         *
         * <p>If a bare input module passed to the linker only contains the value (but not the serialization tree), and
         * if this option is turned on, serialization will be performed as part of linking. A
         * {@link com.svbio.cloudkeeper.model.PreprocessingException} will be thrown if serialization fails for any
         * reason.
         *
         * <p>By default, this option is {@code false}.
         *
         * @return this builder
         */
        public Builder setMarshalValues(boolean marshalValues) {
            this.marshalValues = marshalValues;
            return this;
        }

        /**
         * Sets that the value of
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule#getValue()} should be computed
         * (by deserializing {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule#getRaw()}) if
         * the corresponding property is not available in the bare model given to the linker.
         *
         * <p>If this option is true, method
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeInputModule#getValue()} is guaranteed to
         * return non-null results.
         *
         * <p>If a bare input module passed to the linker only contains the serialization tree (but not the original
         * value), and if this option is turned on, deserialization will be performed as part of linking. A
         * {@link com.svbio.cloudkeeper.model.PreprocessingException} will be thrown if serialization fails for any
         * reason.
         *
         * <p>By default, this option is {@code false}.
         *
         * @return this builder
         */
        public Builder setDeserializeSerializationTrees(boolean deserializeSerializationTrees) {
            this.deserializeSerializationTrees = deserializeSerializationTrees;
            return this;
        }

        /**
         * Sets the unmarshal class loader returned by {@link UnmarshalContext#getClassLoader()}.
         *
         * <p>This option is only relevant if {@link #setDeserializeSerializationTrees(boolean)} has been configured
         * with {@code true}.
         *
         * <p>By default, no unmarshal class loader is configured, and the linker will use the context class loader of
         * the thread that invoked {@link UnmarshalContext#getClassLoader()}.
         *
         * @param unmarshalClassLoader unmarshal class loader, may be {@code null} to configure no unmarshal class
         *     loader
         * @return this builder
         */
        public Builder setUnmarshalClassLoader(@Nullable ClassLoader unmarshalClassLoader) {
            this.unmarshalClassLoader = unmarshalClassLoader;
            return this;
        }

        /**
         * Returns a new {@link LinkerOptions} instance.
         *
         * @return the new linker options
         * @throws IllegalStateException if the options are inconsistent (see above)
         */
        public LinkerOptions build() {
            return new LinkerOptions(this);
        }
    }

    /**
     * Returns linker options for creating a non-executable repository; that is, one where {@link Class} and
     * {@link com.svbio.cloudkeeper.model.api.Executable} instances are not available.
     */
    public static LinkerOptions nonExecutable() {
        return NON_EXECUTABLE;
    }

    /**
     * @see Builder#build()
     */
    private LinkerOptions(Builder builder) {
        classProvider = builder.classProvider;
        executableProvider = builder.executableProvider;
        serializeValues = builder.marshalValues;
        deserializeSerializationTrees = builder.deserializeSerializationTrees;
        unmarshalClassLoader = builder.unmarshalClassLoader;
    }

    /**
     * Returns the class resolver that is to be used to resolve plug-in declaration names into {@link Class} instances.
     */
    ClassProvider getClassProvider() {
        return classProvider;
    }

    /**
     * Returns the class loader to be returned by {@link UnmarshalContext#getClassLoader()}, if any is configured.
     */
    Optional<ClassLoader> getUnmarshalClassLoader() {
        return Optional.ofNullable(unmarshalClassLoader);
    }

    /**
     * Returns the {@link ExecutableProvider} that is to be used to resolve simple-module declaration names into
     * {@link com.svbio.cloudkeeper.model.api.Executable} instances.
     */
    ExecutableProvider getExecutableProvider() {
        return executableProvider;
    }

    boolean isSerializeValues() {
        return serializeValues;
    }

    boolean isDeserializeSerializationTrees() {
        return deserializeSerializationTrees;
    }
}
