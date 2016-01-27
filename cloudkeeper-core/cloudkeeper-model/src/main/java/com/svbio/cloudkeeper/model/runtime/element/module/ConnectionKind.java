package com.svbio.cloudkeeper.model.runtime.element.module;

/**
 * Kind of a connection.
 *
 * This enum is sometimes more convenient than checking whether instances implement one or more of the
 * {@link RuntimeConnection} subinterfaces.
 */
public enum ConnectionKind {
    /**
     * out -> in: These are connections between siblings: These are the most common connections.
     *
     * @see RuntimeSiblingConnection
     */
    SIBLING_CONNECTION,

    /**
     * in -> in: These are connections from an in-port of a parent module to the in-port of a child module.
     *
     * @see RuntimeParentInToChildInConnection
     */
    COMPOSITE_IN_TO_CHILD_IN,

    /**
     * out -> out: These are connections from the out-port of a child module to the out-port of the enclosing parent
     * module.
     *
     * @see RuntimeChildOutToParentOutConnection
     */
    CHILD_OUT_TO_COMPOSITE_OUT,

    /**
     * in -> out: These are short-circuit connections in a composite module, from an in-port directly to an out-port.
     *
     * @see RuntimeShortCircuitConnection
     */
    SHORT_CIRCUIT
}
