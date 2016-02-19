/**
 * Defines interfaces for modeling CloudKeeper modules, declarations, and repositories.
 *
 * <p>The interfaces in this package define the (bare) CloudKeeper object model with that all CloudKeeper language
 * elements (such as modules, declarations, and repositories) can be defined. All interfaces in this package are as bare
 * as reasonably possible; that is, they model only what is necessary to represent abstract syntax trees without the
 * excess weight of any convenience methods.
 *
 * <p>In particular, any valid CloudKeeper language element can be represented as a tree of instances from interfaces in
 * this package -- that is, the Java object graph, where instances of the interfaces in this package are nodes and
 * object references are edges, is a tree. Marshaling of the interfaces in this package is therefore straightforward.
 * Package {@link xyz.cloudkeeper.model.beans} provides Java Bean-style classes with support for JAXB
 * marshaling/unmarshaling.
 *
 * <p>At runtime, a richer optimized representation is typically necessary, and this runtime state is provided by the
 * package {@link xyz.cloudkeeper.model.runtime} and its subpackages. The CloudKeeper component that transforms a
 * bare representation in to the optimized runtime representation is called the <em>CloudKeeper linker</em>. It resolves
 * all symbolic references into Java object references and performs validation and preprocessing.
 *
 * <p>Implementations of the interfaces in this package are not required to be mutable, immutable, or serializable.
 * Moreover, this package does not prescribe a particular behavior for {@code equals()} or {@code hashCode()}.
 * As such, the effects of relying on these methods are undefined. In particular, it is not a requirement that different
 * implementations of an interface could ever compare equal.
 *
 * <p>Implementations and subinterfaces may, however, implement/impose a particular behavior. For instance,
 * implementations may also implement the {@link xyz.cloudkeeper.model.Immutable} interface. This information
 * could be used, for instance, when copying values (for immutable objects it would be enough to simply copy the object
 * reference).
 *
 * <p>This package provides default implementations of {@code toString()} methods for each interface. All
 * CloudKeeper-provided implementations delegate to the default implementations, thus facilitating debugging and
 * composing error messages.
 */
package xyz.cloudkeeper.model.bare;
