package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.element.BareElement;
import com.svbio.cloudkeeper.model.bare.element.BareSimpleNameable;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;

import javax.annotation.Nullable;

/**
 * Port.
 *
 * <p>Modules receive their inputs through in-ports, and provide results through out-ports. Thus, a port is to a module
 * what a formal parameter is to a function definition in a imperative programming languages.
 *
 * In the simplest case, data "flows" from an out-port of a module to an in-port of the successor module. Beyond these
 * "sibling" connections, two ports may support other kinds of connections (specifically, parent-in-to-child-in,
 * child-out-to-parent-out, or short-circuit connections).
 */
public interface BarePort extends BareElement, BareSimpleNameable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "port";

    /**
     * Calls the visitor method that is appropriate for the actual type of this port.
     *
     * @param visitor the visitor operating on this port
     * @param parameter additional parameter to the visitor
     * @param <T> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @return a visitor-specified result
     */
    @Nullable
    <T, P> T accept(BarePortVisitor<T, P> visitor, @Nullable P parameter);

    /**
     * Returns the port type.
     */
    @Nullable
    BareTypeMirror getType();
}
