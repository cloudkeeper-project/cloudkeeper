package com.svbio.cloudkeeper.model.bare.element.module;

import javax.annotation.Nullable;

/**
 * Visitor of ports, in the style of the visitor design pattern.
 */
public interface BarePortVisitor<T, P> {
    /**
     * Visits an in-port.
     */
    @Nullable
    T visitInPort(BareInPort inPort, @Nullable P parameter);

    /**
     * Visits an out-port.
     */
    @Nullable
    T visitOutPort(BareOutPort outPort, @Nullable P parameter);

    /**
     * Visits an I/O-port.
     */
    @Nullable
    T visitIOPort(BareIOPort ioPort, @Nullable P parameter);
}
