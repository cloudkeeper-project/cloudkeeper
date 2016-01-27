package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.MissingAnnotationException;
import com.svbio.cloudkeeper.model.api.ModuleConnector;
import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;
import com.svbio.cloudkeeper.model.bare.element.module.BareInPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.bare.element.module.BarePortVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import com.svbio.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;

public abstract class SimpleModule<D extends SimpleModule<D>> extends Module<D> implements BareProxyModule {
    protected SimpleModule() {
        if (!getModuleClass().isAnnotationPresent(SimpleModulePlugin.class)) {
            throw new MissingAnnotationException(getModuleClass(), SimpleModulePlugin.class, getLocation());
        }

        createPorts(new PortVisitor() {
            @Override
            public void visitPortClass(SimpleName name, Class<?> portClass, Type type,
                List<DSLAnnotation> annotations) {

                if (InPort.class.isAssignableFrom(portClass)) {
                    new InPort<>(name, type, annotations);
                } else if (OutPort.class.isAssignableFrom(portClass)) {
                    new OutPort<>(name, type, annotations);
                }
            }
        });
    }


    @Override
    public final String toString() {
        return BareProxyModule.Default.toString(this);
    }

    /**
     * In-Port.
     *
     * An in-port can be the target for connections from ports that are at least as narrow as {@code T}.
     *
     * @param <T> type of the in-port
     */
    public final class InPort<T> extends ToConnectablePort<T> implements DSLInPort<T> {
        InPort(SimpleName name, Type portType, List<DSLAnnotation> annotations) {
            super(name, portType, false, annotations);
        }

        @SuppressWarnings("unchecked")
        public T get() {
            return (T) getModuleCreationArguments().getModuleConnector().getInput(getSimpleName());
        }

        @Override
        public String toString() {
            return BareInPort.Default.toString(this);
        }

        @Override
        public <U, P> U accept(BarePortVisitor<U, P> visitor, P parameter) {
            return visitor.visitInPort(this, parameter);
        }
    }

    /**
     * Out-Port.
     *
     * An out-port can the source for connections to ports that are at least as wide as {@code T}.
     *
     * @param <T> type of the out-port
     */
    public final class OutPort<T> extends Module<D>.Port implements FromConnectable<T>, DSLOutPort<T> {
        OutPort(SimpleName name, Type type, List<DSLAnnotation> annotations) {
            super(name, type, false, annotations);
        }

        public void set(T value) {
            getModuleCreationArguments().getModuleConnector().setOutput(getSimpleName(), value);
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

    @Override
    public final <T, P> T accept(BareModuleVisitor<T, P> visitor, P parameter) {
        return visitor.visitLinkedModule(this, parameter);
    }

    @Override
    public final BareQualifiedNameable getDeclaration() {
        return new MutableQualifiedNamable()
            .setQualifiedName(Name.qualifiedName(getModuleClass().getName()));
    }

    /**
     * Performs the user-defined actions of this simple module.
     *
     * <p>For any simple module implemented in Java (or another JVM-based language), this method is the entry point into
     * the user-defined module code (similar to what {@code public static void main(String[])} is for Java programs).
     *
     * <p>The default implementation does nothing. This is because simple modules could possibly also be implemented in
     * a non-JVM language, and the CloudKeeper DSL is used only to define the module interface (that is, in-ports and
     * out-ports). In this case, it would be undesirable if this method was abstract and would have to be implemented.
     *
     * <p>When overriding this method, it is good practice to restrict the checked exceptions that may be thrown.
     *
     * @throws Exception if an exception occurs
     */
    public void run() throws Exception { }

    private ModuleConnector requireModuleConnector() {
        ModuleConnector moduleConnector = getModuleCreationArguments().getModuleConnector();
        if (moduleConnector == null) {
            throw new IllegalStateException(String.format(
                "Illegal use of method in this context, because the DSL module was not created with a %s.",
                ModuleConnector.class.getSimpleName()
            ));
        }
        return moduleConnector;
    }

    /**
     * Returns the execution trace representing the current module invocation.
     *
     * @see com.svbio.cloudkeeper.model.api.ModuleConnector#getExecutionTrace()
     */
    protected final RuntimeAnnotatedExecutionTrace getExecutionTrace() {
        return requireModuleConnector().getExecutionTrace();
    }

    /**
     * Returns the working directory for this module execution that may be used as temporary workspace.
     *
     * <p>User-defined code must not rely on the working directory of the JVM (system property {@code user.dir}), but
     * should use the directory returned by this method instead. This also precludes relying on methods that may use
     * {@code user.dir} indirectly, such as {@link java.nio.file.Paths#get(String, String...)} when called with a
     * relative path.
     *
     * <p>The returned directory is guaranteed to be cleaned up once the user-define code is out of scope. Multiple
     * invocations always return the same directory.
     *
     * @return the working directory for this module execution
     */
    protected final Path getWorkingDirectory() {
        return requireModuleConnector().getWorkingDirectory();
    }

    /**
     * Returns the value of the in-port with the given name.
     *
     * @param inPortName name of in-port
     * @return value of the given in-port
     * @throws NullPointerException if the argument is null
     * @throws com.svbio.cloudkeeper.model.api.ConnectorException If the value cannot be retrieved; for instance,
     *     because the identifier does not refer to an in-port, or because the value could not be read from the staging
     *     area.
     *
     * @see ModuleConnector#getInput(SimpleName)
     */
    protected final Object getInput(SimpleName inPortName) {
        return getModuleCreationArguments().getModuleConnector().getInput(inPortName);
    }

    /**
     * Sets the value of the out-port with the given name.
     *
     * @param outPortName name of out-port
     * @param value value
     * @throws NullPointerException if any argument is null
     * @throws com.svbio.cloudkeeper.model.api.ConnectorException If the value cannot be written; for instance, because
     *     the identifier does not refer to an out-port, or because the value could not be written to the staging area.
     *
     * @see ModuleConnector#setOutput(SimpleName, Object)
     */
    protected final void setOutput(SimpleName outPortName, Object value) {
        getModuleCreationArguments().getModuleConnector().setOutput(outPortName, value);
    }
}
