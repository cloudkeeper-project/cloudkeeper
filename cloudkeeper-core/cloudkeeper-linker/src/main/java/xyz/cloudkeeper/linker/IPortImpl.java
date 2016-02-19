package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.runtime.element.module.RuntimePort;
import xyz.cloudkeeper.model.util.ImmutableList;

interface IPortImpl extends RuntimePort {
    @Override
    ModuleImpl getModule();

    /**
     * Adds the given connection that ends with this port.
     *
     * <p>This method is only to be called during construction.
     */
    void addIncomingConnection(ConnectionImpl connection);

    /**
     * Adds the given connection that starts with this port.
     *
     * <p>This method is only to be called during construction.
     */
    void addOutgoingConnection(ConnectionImpl connection);

    @Override
    ImmutableList<ConnectionImpl> getInConnections();

    @Override
    ImmutableList<ConnectionImpl> getOutConnections();
}
