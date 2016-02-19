package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.dsl.exception.AnonymousRecursionException;
import xyz.cloudkeeper.dsl.exception.ConstructorForChildModuleException;
import xyz.cloudkeeper.dsl.exception.DanglingChildException;
import xyz.cloudkeeper.dsl.exception.RedundantFieldException;
import xyz.cloudkeeper.dsl.exception.UnassignedFieldException;
import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.bare.element.module.BareParentModule;
import xyz.cloudkeeper.model.bare.element.module.BarePort;
import xyz.cloudkeeper.model.bare.element.module.BarePortVisitor;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.module.ConnectionKind;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class DSLParentModule<D extends DSLParentModule<D>> extends Module<D> implements BareParentModule, Immutable {
    /**
     * Map of all child modules (as {@link BareModule} references) that have
     * this module as parent.
     *
     * This map has predictable iteration order.
     */
    private final Map<String, BareModule> modules = new LinkedHashMap<>();

    /**
     * List of all child modules (as {@link Module} references).
     *
     * Note that {@code dslModules} contains {@link Module} instances where {@link #modules} would contain a
     * {@link DSLProxyModule} instance.
     */
    private final List<Module<?>> dslModules = new ArrayList<>();

    /**
     * {@link IdentityHashMap} of all modules created by {@link #child(Class)} or
     * {@link #valueWithPortType(Object, Type)}.
     */
    private final IdentityHashMap<Module<?>, Boolean> unfinishedModules = new IdentityHashMap<>();

    static final class ValueAndType<T> {
        private final T value;
        private final Type staticType;

        ValueAndType(T value, Type staticType) {
            this.value = Objects.requireNonNull(value);
            this.staticType = staticType;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            ValueAndType<?> other = (ValueAndType<?>) otherObject;
            return value.equals(other.value)
                && Objects.equals(staticType, other.staticType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, staticType);
        }
    }

    /**
     * Map from input values to {@link InputModule}s. Multiple calls to one of the {@code value()} methods will return
     * the same input module.
     */
    private final Map<ValueAndType<?>, InputModule<?>> valuesToInputModules = new LinkedHashMap<>();

    /**
     * The {@link DSLProxyModule} that serves as a proxy for this module in all
     * {@link xyz.cloudkeeper.model.bare} interfaces.
     *
     * If null, then this module is either a top-level module, or it is anonymous (does not have a declaration).
     */
    @Nullable private final DSLProxyModule linkedModule;


    DSLParentModule() {
        // A composite module that does not have a CompositeModulePlugin annotation is an anonymous module.
        linkedModule
            = (getDSLParent() != null && getModuleClass().isAnnotationPresent(CompositeModulePlugin.class))
                ? new DSLProxyModule(this)
                : null;
    }

    @Override
    public final List<? extends BarePort> getDeclaredPorts() {
        return getDeclaredPortsInternal();
    }

    public final class InPort<T> extends ToConnectablePort<T> implements FromConnectable<T>, DSLInPort<T> {
        /**
         * Constructor.
         *
         * Package-protected so that it cannot be instantiated by user-level code.
         */
        InPort(SimpleName name, Type type, List<DSLAnnotation> annotations) {
            super(name, type, false, annotations);
        }

        @Override
        public String toString() {
            return BareInPort.Default.toString(this);
        }

        @Override
        public <U, P> U accept(BarePortVisitor<U, P> visitor, P parameter) {
            return visitor.visitInPort(this, parameter);
        }

        @Override
        public D fromValue(T value) {
            if (getDSLParent() == null) {
                return super.fromValue(value);
            }

            return from(value(value));
        }
    }

    public final class OutPort<T> extends ToConnectablePort<T> implements FromConnectable<T>, DSLOutPort<T> {
        /**
         * Constructor.
         *
         * Package-protected so that it cannot be instantiated by user-level code.
         */
        OutPort(SimpleName name, Type type, List<DSLAnnotation> annotations) {
            super(name, type, false, annotations);
        }

        @Override
        public String toString() {
            return BareOutPort.Default.toString(this);
        }

        @Override
        public <U, P> U accept(BarePortVisitor<U, P> visitor, P parameter) {
            return visitor.visitOutPort(this, parameter);
        }
    }

    @Nullable
    private DSLParentModule<?> findAncestor(Type genericType) {
        DSLParentModule<?> compositeModule = this;
        while (compositeModule != null) {
            if (genericType.equals(compositeModule.getModuleCreationArguments().getGenericType())) {
                return compositeModule;
            }

            compositeModule = compositeModule.getDSLParent();
        }
        return null;
    }

    /**
     * Create a new child module.
     *
     * This method will detect recursion. Recursion is allowed within declared modules, but not for anonymous modules.
     * That is, if the current module has a {@link CompositeModulePlugin} annotation), it may create a child that
     * directly or indirectly creates a child of the same type as the current module.
     *
     * Recursion in an anonymous module will also be detected, and a {@link AnonymousRecursionException} exception will
     * be thrown.
     *
     * Internally, recursion is resolved by scanning through the "creation stack" induced by this method, and returning
     * the last occurrence of the same module class.
     *
     * @param moduleClass The class corresponding to {@code T}.
     * @param <T> The type of the child module, must be a subclass of {@link Module}.
     * @return the new child module
     * @throws xyz.cloudkeeper.dsl.exception.DSLException if instantiation of the child module
     *     fails
     */
    protected final <T extends Module<T>> T child(Class<T> moduleClass) {
        try {
            // Find if this module or same parent was created with the same abstract module type.
            DSLParentModule<?> ancestor = findAncestor(moduleClass);
            if (ancestor != null) {
                // ancestor is, of course, a composite module.
                // We first verify that ancestor (and thus moduleClass) is not anonymous, because anonymous composite
                // modules cannot be recursive.
                if (!moduleClass.isAnnotationPresent(CompositeModulePlugin.class)) {
                    throw new AnonymousRecursionException(getLocation());
                }

                // ancestor is either this module, or an ancestor of this module. Therefore, if ancestor is a proxy
                // module, we can just reuse its reference. Typically, after the child method return, Port#from() will
                // be called to create the connections to the child module's in-ports. None of these connection can
                // possibly be a sibling or a composite-in-to-child-in connection (see #addConnectionToPorts()).
                // Therefore, these calls to Port#from() will have no effect.
                if (ancestor.linkedModule != null) {
                    @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
                    T typedAncestor = (T) ancestor;
                    unfinishedModules.put(typedAncestor, Boolean.TRUE);
                    return typedAncestor;
                }

                // We only reach this point if ancestor is the top-level module that does not have a parent.
            }

            // Since we don't want to force users to write module constructors with internal arguments, we pass
            // arguments to the internal constructors via a ThreadLocal.
            ModuleFactory moduleFactory = getModuleCreationArguments().getModuleFactory();
            ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.set(new ModuleCreationArguments(
                moduleFactory, this, moduleClass, Locatables.getCallingStackTraceElement(), null
            ));
            Class<? extends T> proxyClass = moduleFactory.getProxyClass(moduleClass);
            T child = Instantiator.instantiate(proxyClass);
            unfinishedModules.put(child, Boolean.TRUE);
            return child;
        } finally {
            ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.remove();
        }
    }

    /**
     * {@inheritDoc}
     *
     * If this module is a proxy module (because it has a parent module and also a {@link CompositeModulePlugin}
     * annotation), then only connections from the parent module or from a sibling of this module will be recorded.
     * Any other connections (of type
     * {@link xyz.cloudkeeper.model.runtime.element.module.ConnectionKind#SHORT_CIRCUIT}
     * or {@link xyz.cloudkeeper.model.runtime.element.module.ConnectionKind#CHILD_OUT_TO_COMPOSITE_OUT}, but also
     * "invalid" connections) will be ignored.
     *
     * "Invalid" connections are ignored because they may arise as part of the "terminating case" of a recursive module.
     * See {@link #child(Class)} for details. This implementation will still detect all errors because the module must,
     * in the template of the composite-module declaration, appear as a real parent module (and not a proxy module).
     */
    @Override
    EnumSet<ConnectionKind> getConnectionFilter() {
        return linkedModule == null
            ? null
            : EnumSet.of(ConnectionKind.COMPOSITE_IN_TO_CHILD_IN, ConnectionKind.SIBLING_CONNECTION);
    }

    /**
     * Operator for creating an apply-to-all connection.
     *
     * The {@code forEach} operator cannot be chained.
     *
     * @param fromConnectable source that is of array type
     * @param <T> element type of array
     * @return {@link FromConnectable} instance that can be connected to a {@link ToConnectable} of type {code U},
     *     provided that {@code T} extends {@code U}
     */
    protected static <T> FromConnectable<T> forEach(FromConnectable<? extends Collection<T>> fromConnectable) {
        return new ApplyToAllProxy<>(fromConnectable);
    }

    /**
     * Operator for creating an combine-into-array connection.
     *
     * Combine-into-array connections are necessary to gather the results of a module that has an incoming apply-to-all
     * connection.
     *
     * @param fromConnectable source that is of element type
     * @param <T> element type of array
     * @return {@link FromConnectable} instance that can be connected to a {@link ToConnectable} of array type {code U},
     *     provided that {@code T} extends {@code U}
     */
    protected static <T> FromConnectable<Collection<T>> arrayOf(FromConnectable<? extends T> fromConnectable) {
        return new CombineIntoArrayPort<>(fromConnectable);
    }

    private <T> InputModule<T> valueWithPortType(T value, Type staticType) {
        requireUnfrozen();

        ValueAndType<T> valueAndType = new ValueAndType<>(value, staticType);
        @SuppressWarnings("unchecked")
        InputModule<T> module = (InputModule<T>) valuesToInputModules.get(valueAndType);
        if (module != null) {
            return module;
        } else {
            try {
                ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.set(new ModuleCreationArguments(
                    getModuleCreationArguments().getModuleFactory(), this, InputModule.class,
                    Locatables.getCallingStackTraceElement(), null
                ));
                module = new InputModule<>(staticType, value);
                unfinishedModules.put(module, Boolean.TRUE);
            } finally {
                ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.remove();
            }
        }
        valuesToInputModules.put(valueAndType, module);
        return module;
    }

    /**
     * Create an input module that provides the given value.
     *
     * The out-port type of the input module will be inferred from the value's dynamic type. Therefore, this method
     * may only be used if the result will be assigned to a field of type {@link InputModule}, or if {@code value} is of
     * non-generic type. Otherwise, {@link #value(Object, TypeToken)} needs to be used to explicitly set the out-port
     * type of the input module.
     *
     * @param value input value, must not be null
     * @param <T> CloudKeeper type of this input
     * @return input module
     */
    protected final <T> InputModule<T> value(T value) {
        return valueWithPortType(value, null);
    }

    protected final <T> InputModule<T> value(T value, TypeToken<T> typeToken) {
        return valueWithPortType(value, typeToken.getJavaType());
    }

    @Override
    public final List<BareModule> getModules() {
        requireFrozen();
        return new ArrayList<>(modules.values());
    }

    /**
     * Returns all connections in this composite module that involve this module or that link two child modules.
     *
     * @return connections
     */
    @Override
    public final List<DSLConnection> getConnections() {
        requireFrozen();

        List<DSLConnection> connections = new ArrayList<>();
        for (DSLConnection connection: getConnectionsToPorts()) {
            if (connection instanceof DSLConnection.ShortCircuit
                || connection instanceof DSLConnection.ChildOutToParent) {

                connections.add(connection);
            }
        }
        for (Module<?> child: dslModules) {
            for (DSLConnection connection: child.getConnectionsToPorts()) {
                if (connection instanceof DSLConnection.ParentToChild || connection instanceof DSLConnection.Sibling) {
                    connections.add(connection);
                }
            }
        }
        return connections;
    }

    static Collection<String> fieldNames(Collection<Field> fields) {
        Set<String> names = new LinkedHashSet<>();
        for (Field field: fields) {
            names.add(field.getName());
        }
        return names;
    }

    @Override
    void finishConstruction(String simpleName, Type declaredType, List<DSLAnnotation> annotations) {
        super.finishConstruction(simpleName, declaredType, annotations); // calls requireUnfrozen()

        for (Field field: Fields.filteredFields(this, DSLParentModule.class, Module.class)) {
            @SuppressWarnings("unchecked")
            @Nullable Module<?> child = (Module<?>) Fields.valueOfField(this, field);

            if (child == null) {
                // location is imprecise, as unfortunately the source-code location of a field declaration is not
                // recorded in the Java byte code. We give a best-effort location.
                throw new UnassignedFieldException(field, getLocation());
            } else if (unfinishedModules.containsKey(child)) {
                String name = field.getName();
                if (!child.isConstructionFinished()) {
                    // Need the safeguard here because the #child() method may reuse references CompositeModule when it
                    // detects recursion.
                    child.finishConstruction(name, field.getGenericType(),
                        DSLAnnotation.unmodifiableAnnotationList(field));
                }
                unfinishedModules.remove(child);
                dslModules.add(child);
                modules.put(name, child.getBareModule());
            } else {
                // We have here: child != null && !unfinishedModules.containsKey(child)
                if (child.getDSLParent() != this) {
                    // Catch the case where a field contains a module created within another composite module.
                    // (and this did not happen in the #child() method in order to terminate a recursion).
                    throw new ConstructorForChildModuleException(getLocation());
                } else {
                    // No child should be assigned to more than one field.
                    throw new RedundantFieldException(
                        fieldNames(Fields.getFieldsWithValue(this, DSLParentModule.class, child)),
                        DSLParentModule.class.toString(),
                        child.getClass().toString(), child.getLocation()
                    );
                }
            }
        }

        // Deal with InputModules separately now. Some of them may be anonymous, i.e., not be references by any
        // instance field. Therefore, they would not have been captured by the previous for-loop.
        Map<String, Integer> typeToCount = new LinkedHashMap<>();
        for (InputModule<?> child: valuesToInputModules.values()) {
            if (unfinishedModules.containsKey(child)) {
                String typeName = child.getValue().getClass().getSimpleName();
                int count = typeToCount.containsKey(typeName)
                    ? typeToCount.get(typeName) + 1
                    : 0;

                // Find a name for the input module
                final String prefix = "input$" + typeName;
                String name = prefix + count;
                while (modules.containsKey(name)) {
                    ++count;
                    name = prefix + count;
                }
                typeToCount.put(typeName, count);

                // InputModule instances are never create by the #child() method, so we do not have to check whether
                // finishConstruction() has been called before.
                child.finishConstruction(name, null, Collections.<DSLAnnotation>emptyList());
                unfinishedModules.remove(child);
                dslModules.add(child);
                modules.put(name, child);
            }
        }

        // Make sure there are no dangling children (i.e., children created using the child method but not assigned
        // to any field).
        if (!unfinishedModules.isEmpty()) {
            throw new DanglingChildException(unfinishedModules.keySet().iterator().next().getLocation());
        }
    }

    @Override
    final void freeze() {
        super.freeze();

        for (Module<?> child: dslModules) {
            // We check that the child is not yet frozen. This will terminate potential recursions.
            // Rationale: Make sure that indeed all references reachable from this module are frozen afterwards.
            if (!child.isFrozen()) {
                child.freeze();
            }
        }
    }
}
