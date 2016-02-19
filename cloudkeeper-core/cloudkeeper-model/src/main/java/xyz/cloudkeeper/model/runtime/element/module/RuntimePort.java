package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BarePort;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimeAnnotatedConstruct;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;

/**
 * Port.
 *
 * A port is an endpoint of a data-flow connection. It consists of a name, type information, a reference to the
 * module this port belongs to, and a list of connections. In a most basic case, data "flows" from an out-port to
 * an in-port. Yet, other connections are possible, too. A parent module's in-port may be connected to a child module's
 * in-port, in order to forward the input data. Likewise, the out-port of a parent module's child module may be
 * connected to an out-port of the composite module, in order to forward the output.
 */
public interface RuntimePort extends BarePort, RuntimeAnnotatedConstruct, RuntimeElement {
    @Override
    @Nonnull
    SimpleName getSimpleName();

    @Override
    @Nonnull
    RuntimeTypeMirror getType();

    /**
     * Returns the module this port belongs to.
     *
     * @return the enclosing module
     */
    // TODO: Remove in favor of getEnclosingElement()?
    RuntimeModule getModule();

    /**
     * Returns the port container that owns this port.
     */
    @Override
    @Nonnull
    RuntimePortContainer getEnclosingElement();

    /**
     * Returns the index of this port in the port list of the enclosing element.
     *
     * <p>The index returned by this method is equal to the result of
     * {@code getEnclosingElement().getPorts().indexOf(port)}.
     *
     * @see RuntimePortContainer#getPorts()
     *
     * @return the index of this port in the port list of the enclosing element
     */
    int getIndex();

    /**
     * Returns all incoming connections.
     */
    ImmutableList<? extends RuntimeConnection> getInConnections();

    /**
     * Returns all incoming connections.
     */
    ImmutableList<? extends RuntimeConnection> getOutConnections();
}
