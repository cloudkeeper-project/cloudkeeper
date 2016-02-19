package xyz.cloudkeeper.model.runtime.element.module;

/**
 * Options in which the types of two ports can relate to each other in a (potential) connection.
 *
 * <p>Let {@code T} denote the type of the source port, and {@code U} the type of the destination port. Let "extends" be
 * the partial order over all types that corresponding to the Java {@code extends} keyword.
 */
public enum TypeRelationship {
    /**
     * A port of type {@code T} cannot be connected to a port of type {@code U}. While the most obvious cases are
     * prevented by the Java type system, there are some incompatible connections the type system cannot detect. As an
     * example, a connection from an array of arrays to the element type is {@code INCOMPATIBLE}.
     */
    INCOMPATIBLE,

    /**
     * Type {@code T} extends {@code U}.
     */
    SIMPLE,

    /**
     * Type {@code T} is an array type and the element type of {@code T} extends {@code U}.
     *
     * <p>This relationship places the following constraint on the connection:
     * <ul><li>
     *     The topological relationship must be {@link ConnectionKind#COMPOSITE_IN_TO_CHILD_IN} or
     *     {@link ConnectionKind#SIBLING_CONNECTION}.
     * </li></ul>
     *
     * <p>This relationship implies that the destination module is applied to each element of the source port. The
     * following constraints are placed on the destination module:
     * <ul><li>
     *     There may be no other {@code APPLY_TO_ALL} connection to the destination module.
     * </li><li>
     *     All connections leaving the destination module must be {@link #MERGE} connections.
     * </li></ul>
     */
    APPLY_TO_ALL,

    /**
     * Type {@code T} extends the element type of {@code U}, which an array type.
     *
     * <p>This relationship places the following constraint on the connection:
     * <ul><li>
     *     The topological relationship must be {@link ConnectionKind#SIBLING_CONNECTION} or
     *     {@link ConnectionKind#CHILD_OUT_TO_COMPOSITE_OUT}.
     * </li></ul>
     */
    MERGE
}
