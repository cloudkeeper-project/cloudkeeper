package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.dsl.exception.ConstructorForChildModuleException;
import xyz.cloudkeeper.dsl.exception.InvalidTypeException;
import xyz.cloudkeeper.model.api.CloudKeeperEnvironment;
import xyz.cloudkeeper.model.api.WorkflowExecutionBuilder;
import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import xyz.cloudkeeper.model.bare.element.module.BareIOPort;
import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.bare.element.module.BarePort;
import xyz.cloudkeeper.model.bare.element.module.BarePortVisitor;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.element.Version;
import xyz.cloudkeeper.model.runtime.element.module.ConnectionKind;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Abstract base class representing a module declaration.
 *
 * Implementation note: DSL modules implement {@link xyz.cloudkeeper.model.bare.element.module.BareModule} and not
 * {@link xyz.cloudkeeper.model.bare.element.module.BareModuleDeclaration} because not all user-defined module
 * classes will be declarations. For instance, loop modules are never declarations.
 *
 * @param <D> The derived class (the "curiously recurring template pattern")
 */
public abstract class Module<D extends Module<D>> extends AbstractFreezable implements BareModule, BareLocatable {
    /**
     * Scheme for URIs that represent repositories that are the transitive hull of a DSL plug-in declaration.
     */
    public static final String URI_SCHEME = "x-cloudkeeper-dsl";

    private final Location declarationLocation;
    @Nullable private final ModuleCreationArguments moduleCreationArguments;
    private List<DSLAnnotation> declaredAnnotations;

    static class PortComparator implements Comparator<Module<?>.Port>, Serializable {
        private static final long serialVersionUID = -3603023395952441578L;

        private enum ToNumberVisitor implements BarePortVisitor<Byte, Void> {
            INSTANCE;

            @Override
            public Byte visitInPort(BareInPort inPort, Void ignored) {
                return 0;
            }

            @Override
            public Byte visitIOPort(BareIOPort ioPort, Void ignored) {
                return 1;
            }

            @Override
            public Byte visitOutPort(BareOutPort outPort, Void ignored) {
                return 2;
            }
        };

        @Override
        public int compare(Module<?>.Port left, Module<?>.Port right) {
            int comparison = left.accept(ToNumberVisitor.INSTANCE, null) - right.accept(ToNumberVisitor.INSTANCE, null);
            return comparison != 0
                ? comparison
                : left.getSimpleName().compareTo(right.getSimpleName());
        }
    }

    /**
     * Sorted of port {@link Port} objects created within this module as enclosing object.
     *
     * The set is sorted by 1) the directions and 2) by the port name. The sorting ensures predictable iteration order
     * over a module's ports. (The alternative of using {@link Class#getFields()} does not guarantee a predictable
     * order.) Directions are sorted in the order: in-ports, i/o-ports, out-ports.
     */
    private final SortedSet<Port> declaredPorts = new TreeSet<>(new PortComparator());

    /**
     * Set of connections that have any of this module's ports as target.
     */
    private final Set<DSLConnection> connectionsToPorts = new LinkedHashSet<>();

    @Nullable private final Map<SimpleName, Object> inputValues;

    @Nullable private final DSLParentModule<?> parent;
    private SimpleName simpleName;

    Module() {
        declarationLocation = Locatables.getCallingStackTraceElement();
        moduleCreationArguments = ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.get();
        if (moduleCreationArguments == null) {
            // Since we do not have module creation arguments, the closest location we can report is declarationLocation
            throw new ConstructorForChildModuleException(declarationLocation);
        }
        ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.remove();

        inputValues = moduleCreationArguments.getModuleConnector() == null
            ? new LinkedHashMap<SimpleName, Object>()
            : null;

        parent = moduleCreationArguments.getParent();
    }

    @Override
    public abstract String toString();

    /**
     * Port visitor that takes a port name and {@link Class} object and instantiates the appropriate {@link Module.Port}
     * subclass.
     */
    interface PortVisitor {
        void visitPortClass(SimpleName name, Class<?> portClass, Type type,
            List<DSLAnnotation> annotations);
    }

    /**
     * Create a {@link Port} instance for each no-argument method that returns a {@link Port}.
     *
     * This method is called by the constructors of subclasses. It <strong>must not</strong> be called after
     * construction.
     */
    final void createPorts(PortVisitor portVisitor) {
        for (Method method: getModuleClass().getMethods()) {
            Class<?> portClass = method.getReturnType();
            if (
                Modifier.isAbstract(method.getModifiers())
                    && method.getParameterTypes().length == 0
                    && Port.class.isAssignableFrom(portClass)
            ) {
                Type type = method.getGenericReturnType();
                @Nullable Type typeParameter = null;
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = ((ParameterizedType) type);
                    if (parameterizedType.getActualTypeArguments().length == 1) {
                        typeParameter = parameterizedType.getActualTypeArguments()[0];
                    }
                }

                if (typeParameter == null) {
                    throw new InvalidTypeException(String.format(
                        "Expected port type with parameter, but got %s.", type
                    ), getLocation());
                }

                portVisitor.visitPortClass(SimpleName.identifier(method.getName()), portClass, typeParameter,
                    DSLAnnotation.unmodifiableAnnotationList(method));
            }
        }
    }

    /**
     * Abstract base class representing a module port.
     */
    abstract class Port implements Connectable, BarePort {
        private final SimpleName name;
        private Type javaType;
        private BareTypeMirror portType;
        private final List<DSLAnnotation> annotations;

        /**
         * Constructor.
         *
         * @param name port name
         * @param javaType port type as Java Type
         * @param implicitPort whether this is an implicit port, such as the out-port of an input module, or the
         *     continue port of a loop module
         * @param annotations an unmodifiable list of annotations, may be null
         */
        Port(SimpleName name, Type javaType, boolean implicitPort, List<DSLAnnotation> annotations) {
            requireUnfrozen();
            this.name = Objects.requireNonNull(name);
            this.javaType = javaType;
            this.annotations = annotations;

            if (!implicitPort) {
                declaredPorts.add(this);
            }
        }

        @Override
        public abstract String toString();

        /**
         * {@inheritDoc}
         *
         * Unfortunately, Java does not provide a way to determine the source-code location of abstract methods.
         * The most accurate location we can give is therefore the declaration location (of the abstract module).
         */
        @Override
        public final Location getLocation() {
            return declarationLocation;
        }

        final Module<D> getDSLModule() {
            return Module.this;
        }

        /**
         * Returns the name of this port.
         *
         * The name of a port is given by the field name in the enclosing {@link Module} object.
         *
         * @return {@inheritDoc}
         * @throws IllegalStateException if the enclosing module is not immutable yet
         */
        @Override
        @Nonnull
        public final SimpleName getSimpleName() {
            return name;
        }

        /**
         * Returns this port.
         *
         * A {@link Connectable} may be a proxy for a port, so this method may be used to distinguish between
         * proxies and real port instances.
         *
         * @return this port
         */
        @Override
        public final Port getPort() {
            return this;
        }

        @Override
        public final BareTypeMirror getType() {
            requireFrozen();
            return portType;
        }

        @Override
        public List<? extends BareAnnotation> getDeclaredAnnotations() {
            return annotations;
        }

        final Type getJavaType() {
            return javaType;
        }

        final void setJavaType(Type javaType) {
            this.javaType = javaType;
        }

        final void finishConstruction() {
            Objects.requireNonNull(javaType, "Port type cannot be null.");

            portType = MutableTypeMirror.fromJavaType(javaType, DSLNestedNameCopyOption.INSTANCE);
        }
    }

    final List<? extends BarePort> getDeclaredPortsInternal() {
        return new ArrayList<>(declaredPorts);
    }

    /**
     * Returns the port with the given name, or {@code null} if no port with the given name can be found.
     *
     * @param portName name of the port
     * @return port with the given name, or {@code null} if no port with the given name can be found
     */
    protected final BarePort getPort(String portName) {
        // This method's primary purpose is to be called by the class generated by
        // {@link xyz.cloudkeeper.dsl.ProxyClassLoader}. Unfortunately, this method therefore needs at least
        // {@code protected} visibility.
        for (BarePort port: declaredPorts) {
            if (port.getSimpleName().toString().equals(portName)) {
                return port;
            }
        }
        return null;
    }

    /**
     * Abstract base class for a port that can accept incoming connections.
     *
     * @param <T> Type of the port
     */
    abstract class ToConnectablePort<T> extends Port implements ToConnectable<T> {
        ToConnectablePort(SimpleName name, Type type, boolean implicitPort, List<DSLAnnotation> annotations) {
            super(name, type, implicitPort, annotations);
        }

        /**
         * Add connection from the given port to this port.
         *
         * @param fromConnectable the connection source
         * @return the enclosing module of this port
         * @throws IllegalStateException if {@link #freeze()} has been called before for either the source module or
         *     for this module.
         */
        @Override
        public final D from(FromConnectable<? extends T> fromConnectable) {
            Objects.requireNonNull(fromConnectable);
            Objects.requireNonNull(fromConnectable.getPort());
            Objects.requireNonNull(fromConnectable.getPort().getDSLModule());

            requireUnfrozen();
            fromConnectable.getPort().getDSLModule().requireUnfrozen();

            // This method is called directly by client code, so fine to get stack trace here.
            Location connectionLocation = Locatables.getCallingStackTraceElement();
            DSLConnection connection
                = DSLConnection.create(fromConnectable, this, connectionLocation, getConnectionFilter());
            // connection may be null if the source/target pair has a topological relationship that is filtered out
            if (connection != null) {
                connectionsToPorts.add(connection);
            }
            return self();
        }

        public D fromValue(T value) {
            if (inputValues == null || getDSLParent() != null) {
                throw new IllegalStateException("Illegal use of method in this context.");
            }

            inputValues.put(super.getSimpleName(), value);
            return self();
        }
    }

    /**
     * Returns a reference to this object, with the derived class as static type.
     *
     * This is a convenience method intended for use from within this class and subclasses.
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    final D self() {
        return (D) this;
    }

    @Override
    public final List<? extends BareAnnotation> getDeclaredAnnotations() {
        requireFrozen();

        return declaredAnnotations;
    }

    @Override
    public final Location getLocation() {
        return moduleCreationArguments.getLocation();
    }

    /**
     * Returns the location of the declaration of this module.
     *
     * This will usually be the location of the constructor of the abstract module class.
     */
    final Location getDeclarationLocation() {
        return declarationLocation;
    }

    final Class<?> getModuleClass() {
        Type genericType = moduleCreationArguments.getGenericType();
        if (genericType instanceof Class) {
            return (Class<?>) genericType;
        } else if (genericType instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) genericType).getRawType();
        } else {
            throw new IllegalStateException(String.format("Generic type %s unexpected for module of %s.",
                genericType, getClass().getSuperclass()));
        }
    }

    final ModuleCreationArguments getModuleCreationArguments() {
        return moduleCreationArguments;
    }

    /**
     * Returns the bare-module instance corresponding to this module instance.
     *
     * This is not necessarily always this object. For example, a module might have to be a
     * {@link xyz.cloudkeeper.model.bare.element.module.BareProxyModule} according to the model, but the DSL
     * module is actually a {@link CompositeModule}.
     *
     * @return model instance
     */
    BareModule getBareModule() {
        return this;
    }

    /**
     * Returns the set of topological relationships between potential source/target pairs that potential connections can
     * assume. Returns {@code null} if there are no restrictions on connections.
     */
    EnumSet<ConnectionKind> getConnectionFilter() {
        return null;
    }

    /**
     * Returns the set of connections that have any of this module's ports as target.
     *
     * @return set of connections
     */
    final Set<DSLConnection> getConnectionsToPorts() {
        requireFrozen();

        return Collections.unmodifiableSet(connectionsToPorts);
    }

    /**
     * Finish construction of this module and validate.
     *
     * Subclasses may override this method but <strong>must</strong> call this method first.
     *
     * @param simpleName the name to use for this module
     * @param declaredType the static type of the parent's {@link Field} that contains this module, may be {@code null}
     *     if this module is not contained in a field or does not have a parent
     */
    void finishConstruction(String simpleName, @Nullable Type declaredType, List<DSLAnnotation> annotations) {
        super.finishConstruction();

        if (simpleName != null) {
            this.simpleName = SimpleName.identifier(simpleName);
        }
        declaredAnnotations = annotations;

        for (Port port: declaredPorts) {
            port.finishConstruction();
        }
    }

    @Override
    final void finishConstruction() {
        // The following calls super.finishConstruction()
        finishConstruction(null, null, Collections.<DSLAnnotation>emptyList());
    }

    @Nullable
    final DSLParentModule<?> getDSLParent() {
        return parent;
    }

    /**
     * Returns the simple name of this module; that is, the last component of the qualified name.
     *
     * @return simple name
     */
    @Override
    public final SimpleName getSimpleName() {
        requireFrozen();
        return simpleName;
    }

    /**
     * Returns a new workflow-execution builder that is pre-configured with the inputs previously set with
     * {@link ToConnectablePort#fromValue(Object)}.
     *
     * <p>The returned builder also has a pre-configured dependency on a bundle with name {@code dsl.<module-name>}
     * where {@code <module-name>} is the name of this module class. The version of this dependency is
     * {@link Version#latest()}, and it has a single location URI with schema {@link #URI_SCHEME} and schema-specific
     * party which is {@code <module-name>}.
     *
     * <p>Note that this method returns a <em>pre-configured</em> workflow-execution builder; that is, it performs a
     * superset of the actions of {@link CloudKeeperEnvironment#newWorkflowExecutionBuilder(BareModule)}.
     *
     * @param cloudKeeperEnvironment the CloudKeeper environment
     * @return the new workflow-execution builder
     * @throws NullPointerException if the argument is null
     * @throws IllegalStateException if this method is called for a module that has a parent module or that was not
     *     created with {@link ModuleFactory#create(Class)}
     */
    public final WorkflowExecutionBuilder newPreconfiguredWorkflowExecutionBuilder(
        CloudKeeperEnvironment cloudKeeperEnvironment) {

        Objects.requireNonNull(cloudKeeperEnvironment);
        if (inputValues == null || parent != null) {
            throw new IllegalStateException("Illegal use of method in this context.");
        }

        return cloudKeeperEnvironment.newWorkflowExecutionBuilder(this)
            .setInputs(inputValues)
            .setBundleIdentifiers(Collections.singletonList(toBundleIdentifier()));
    }

    URI toBundleIdentifier() {
        try {
            return new URI(URI_SCHEME, getModuleClass().getName(), null);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException(String.format(
                "Unexpected exception while trying to create URI '%s:%s'.", URI_SCHEME, getModuleClass().getName()
            ), exception);
        }
    }
}
