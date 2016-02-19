package xyz.cloudkeeper.model.beans.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareConnection;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareParentModule;
import xyz.cloudkeeper.model.bare.element.module.BarePort;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlType(propOrder = { "declaredPorts", "modules", "connections" })
public abstract class MutableParentModule<D extends MutableParentModule<D>>
        extends MutableModule<D>
        implements BareParentModule {
    private static final long serialVersionUID = 4744751589966311180L;

    private final ArrayList<MutablePort<?>> declaredPorts = new ArrayList<>();
    private final ArrayList<MutableModule<?>> modules = new ArrayList<>();
    private final ArrayList<MutableConnection<?>> connections = new ArrayList<>();

    MutableParentModule() { }

    MutableParentModule(BareParentModule original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        for (BarePort port: original.getDeclaredPorts()) {
            declaredPorts.add(MutablePort.copyOfPort(port, copyOptions));
        }
        for (BareModule module: original.getModules()) {
            modules.add(MutableModule.copyOfModule(module, copyOptions));
        }
        for (BareConnection connection: original.getConnections()) {
            connections.add(MutableConnection.copyOfConnection(connection, copyOptions));
        }
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        // Note that super.equals() performs the verification that otherObject is of the same class
        if (!super.equals(otherObject)) {
            return false;
        }

        MutableParentModule<?> other = (MutableParentModule<?>) otherObject;
        return Objects.equals(declaredPorts, other.declaredPorts)
            && Objects.equals(modules, other.modules)
            && Objects.equals(connections, other.connections);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(declaredPorts, modules, connections);
    }

    @XmlElementWrapper(name = "ports")
    @XmlElementRef
    @Override
    public final List<MutablePort<?>> getDeclaredPorts() {
        return declaredPorts;
    }

    public D setDeclaredPorts(List<MutablePort<?>> declaredPorts) {
        Objects.requireNonNull(declaredPorts);
        List<MutablePort<?>> backup = new ArrayList<>(declaredPorts);
        this.declaredPorts.clear();
        this.declaredPorts.addAll(backup);
        return self();
    }

    @XmlElementWrapper(name = "modules")
    @XmlElementRef
    @Override
    public final List<MutableModule<?>> getModules() {
        return modules;
    }

    public D setModules(List<MutableModule<?>> modules) {
        Objects.requireNonNull(modules);
        List<MutableModule<?>> backup = new ArrayList<>(modules);
        this.modules.clear();
        this.modules.addAll(backup);
        return self();
    }

    @XmlElementWrapper(name = "connections")
    @XmlElements({
        @XmlElement(type = MutableSiblingConnection.class, name = "sibling-connection"),
        @XmlElement(type = MutableParentInToChildInConnection.class, name = "parent-to-child-connection"),
        @XmlElement(type = MutableChildOutToParentOutConnection.class, name = "child-to-parent-connection"),
        @XmlElement(type = MutableShortCircuitConnection.class, name = "short-circuit-connection")
    })
    @Override
    public final List<MutableConnection<?>> getConnections() {
        return connections;
    }

    public D setConnections(List<MutableConnection<?>> connections) {
        Objects.requireNonNull(connections);
        List<MutableConnection<?>> backup = new ArrayList<>(connections);
        this.connections.clear();
        this.connections.addAll(backup);
        return self();
    }
}
