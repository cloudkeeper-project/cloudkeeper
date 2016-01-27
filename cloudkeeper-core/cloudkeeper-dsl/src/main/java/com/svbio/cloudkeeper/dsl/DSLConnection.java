package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.ConnectionException;
import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.module.BareChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareInPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareParentInToChildInConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BareShortCircuitConnection;
import com.svbio.cloudkeeper.model.bare.element.module.BareSiblingConnection;
import com.svbio.cloudkeeper.model.immutable.Location;
import com.svbio.cloudkeeper.model.runtime.element.module.ConnectionKind;

import javax.annotation.Nullable;
import java.util.EnumSet;

abstract class DSLConnection implements BareConnection, Immutable {
    private final Location location;
    private final FromConnectable<?> fromConnectable;
    private final ToConnectable<?> toConnectable;

    private DSLConnection(
        FromConnectable<?> fromConnectable,
        ToConnectable<?> toConnectable,
        Location location
    ) {
        this.location = location;
        this.fromConnectable = fromConnectable;
        this.toConnectable = toConnectable;
    }

    static final class Sibling extends DSLConnection implements BareSiblingConnection {
        private Sibling(FromConnectable<?> fromConnectable, ToConnectable<?> toConnectable,
            Location location) {

            super(fromConnectable, toConnectable, location);
        }

        @Override
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, P parameter) {
            return visitor.visitSiblingConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareSiblingConnection.Default.toString(this);
        }
    }

    static final class ParentToChild extends DSLConnection
        implements BareParentInToChildInConnection {

        private ParentToChild(FromConnectable<?> fromConnectable, ToConnectable<?> toConnectable,
            Location location) {

            super(fromConnectable, toConnectable, location);
        }

        @Override
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, P parameter) {
            return visitor.visitParentInToChildInConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareParentInToChildInConnection.Default.toString(this);
        }
    }

    static final class ChildOutToParent extends DSLConnection
        implements BareChildOutToParentOutConnection {

        private ChildOutToParent(FromConnectable<?> fromConnectable, ToConnectable<?> toConnectable,
            Location location) {

            super(fromConnectable, toConnectable, location);
        }

        @Override
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, P parameter) {
            return visitor.visitChildOutToParentOutConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareChildOutToParentOutConnection.Default.toString(this);
        }
    }

    static final class ShortCircuit extends DSLConnection implements BareShortCircuitConnection {
        private ShortCircuit(FromConnectable<?> fromConnectable, ToConnectable<?> toConnectable,
            Location location) {

            super(fromConnectable, toConnectable, location);
        }

        @Override
        public <T, P> T accept(BareConnectionVisitor<T, P> visitor, P parameter) {
            return visitor.visitShortCircuitConnection(this, parameter);
        }

        @Override
        public String toString() {
            return BareShortCircuitConnection.Default.toString(this);
        }
    }

    /**
     * Creates and returns the appropriate connection type between the given two ports.
     *
     * This method distinguished whether {@code filter} is {@code null} or not:
     * <ul><li>
     *     Case 1, {@code filter} is non-null. A new connection will only be created and returned if the topological
     *     relationship between the connection source and connection target is contained in {@code filter}. Otherwise,
     *     {@code null} is returned.
     * </li><li>
     *     Case 2, {@code filter} is null. A new connection will always be created and returned, unless the topological
     *     relationship is invalid and an exception is raised.
     * </li></ul>
     *
     * @param fromConnectable source of new connection
     * @param toConnectable target of new connection
     * @param location location where connection is defined in the source code
     * @param filter if
     * @return the appropriate connection subclass for the given two ports; {@code null} if {@code filter} is not
     *     {@code null} and the topological relationship between the source and destination is not contained in
     *     {@code filter}
     * @throws NullPointerException if any argument is null
     * @throws ConnectionException if {@code filter} is {@code null}, and the given source/target pair does not allow a
     *     valid connection
     */
    static DSLConnection create(FromConnectable<?> fromConnectable, ToConnectable<?> toConnectable,
        Location location, @Nullable EnumSet<ConnectionKind> filter) {

        // source port of a hypothetical connection
        Module<?>.Port fromPort = fromConnectable.getPort();
        // module that contains the source port
        Module<?> fromModule = fromPort.getDSLModule();
        // parent module of the module that contains the source port
        DSLParentModule<?> fromModuleParent = fromModule.getDSLParent();
        // target port of a hypothetical connection
        Module<?>.Port toPort = toConnectable.getPort();
        // module that contains the target port
        Module<?> toModule = toPort.getDSLModule();
        // parent module of the module that contains the target port
        DSLParentModule<?> toModuleParent = toModule.getDSLParent();

        if (fromModule == toModule) {
            if (fromPort instanceof BareInPort && toPort instanceof BareOutPort) {
                return filter == null || filter.contains(ConnectionKind.SHORT_CIRCUIT)
                    ? new ShortCircuit(fromConnectable, toConnectable, location)
                    : null;
            }
        } else if (fromModuleParent == toModuleParent) {
            if (fromPort instanceof BareOutPort && toPort instanceof BareInPort) {
                return filter == null || filter.contains(ConnectionKind.SIBLING_CONNECTION)
                    ? new Sibling(fromConnectable, toConnectable, location)
                    : null;
            }
        } else if (fromModule == toModuleParent) {
            if (fromPort instanceof BareInPort && toPort instanceof BareInPort) {
                return filter == null || filter.contains(ConnectionKind.COMPOSITE_IN_TO_CHILD_IN)
                    ? new ParentToChild(fromConnectable, toConnectable, location)
                    : null;
            }
        } else if (fromModuleParent == toModule) {
            if (fromPort instanceof BareOutPort && toPort instanceof BareOutPort) {
                return filter == null || filter.contains(ConnectionKind.CHILD_OUT_TO_COMPOSITE_OUT)
                    ? new ChildOutToParent(fromConnectable, toConnectable, location)
                    : null;
            }
        }
        if (filter == null) {
            throw new ConnectionException(String.format(
                "Cannot connect %s to %s because the topological relationship is neither a sibling, parent-to-child, " +
                "child-to-parent, nor short-circuit connection.", fromConnectable, toConnectable
            ), location);
        } else {
            return null;
        }
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public BareModule getFromModule() {
        return fromConnectable.getPort().getDSLModule().getBareModule();
    }

    @Override
    public BarePort getFromPort() {
        return fromConnectable.getPort();
    }

    public BareModule getToModule() {
        return toConnectable.getPort().getDSLModule().getBareModule();
    }

    @Override
    public BarePort getToPort() {
        return toConnectable.getPort();
    }
}
