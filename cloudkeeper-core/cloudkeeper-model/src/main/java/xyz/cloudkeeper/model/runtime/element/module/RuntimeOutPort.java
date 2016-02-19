package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.util.ImmutableList;

import java.util.List;

public interface RuntimeOutPort extends RuntimePort, BareOutPort {
    /**
     * Returns the index of this out-port in the list of all its enclosing module's out-ports.
     *
     * <p>The result of this method is the index of this out-port in the {@link List} that is obtained by calling
     * {@link RuntimePortContainer#getOutPorts()} on {@link #getEnclosingElement()}).
     *
     * @return index of this out-port in the list of all its enclosing module's out-ports
     */
    int getOutIndex();

    /**
     * Returns a list of in-ports that this out-port depends on.
     *
     * <p>An out-port depends on an in-port if and only if the out-port is contained in the list returned by
     * {@link RuntimeInPort#getDependentOutPorts()}.
     *
     * @return list of in-ports that this out-port depends on
     */
    ImmutableList<? extends RuntimeInPort> getInPortDependencies();
}
