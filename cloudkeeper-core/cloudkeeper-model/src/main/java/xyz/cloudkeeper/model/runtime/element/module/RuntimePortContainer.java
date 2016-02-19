package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.util.ImmutableList;

/**
 * A mixin interface for an element that has ports.
 */
public interface RuntimePortContainer extends RuntimeElement {
    /**
     * Returns the list of all ports.
     *
     * The returned list includes both declared ports and implicit ports (like, e.g., the out-port of an input module).
     */
    ImmutableList<? extends RuntimePort> getPorts();

    /**
     * Returns the list of all in-ports that are contained in {@link #getPorts()}.
     */
    ImmutableList<? extends RuntimeInPort> getInPorts();

    /**
     * Returns the list of all out-ports that are contained in {@link #getPorts()}.
     */
    ImmutableList<? extends RuntimeOutPort> getOutPorts();
}
