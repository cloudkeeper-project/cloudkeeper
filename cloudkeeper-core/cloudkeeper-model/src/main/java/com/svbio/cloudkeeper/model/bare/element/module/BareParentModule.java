package com.svbio.cloudkeeper.model.bare.element.module;

import java.util.List;

public interface BareParentModule extends BareModule {
    /**
     * Returns all declared ports, but not implicit ports (such as the continue-port in a
     * {@link BareLoopModule}).
     */
    List<? extends BarePort> getDeclaredPorts();

    /**
     * Returns the child modules in this parent module.
     */
    List<? extends BareModule> getModules();

    /**
     * Returns all connections within this parent module (in a non-transitive fashion).
     */
    List<? extends BareConnection> getConnections();
}
