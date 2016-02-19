/**
 * Defines classes and methods for linking CloudKeeper modules, declarations, and repositories.
 *
 * <p>This package provides the CloudKeeper linker, which takes bare CloudKeeper interfaces and transforms them into the
 * runtime state so that modules can be executed, port types can be interpreted, etc. More generally speaking, the
 * linker takes the abstract syntax tree and decorates it with cross-links (for instance, links from use to definition)
 * and preprocessed information (for instance, convert lists to maps). The result is the <em>linked and optimized
 * abstract syntax tree</em>.
 *
 * <p>The interfaces that model the runtime state are defined in package {@link xyz.cloudkeeper.model.runtime}.
 * This package contains implementations, but only exposes them through the aforementioned interfaces. As required by
 * the contract of the {@link xyz.cloudkeeper.model.runtime} interfaces, all implementations are immutable.
 * Implementations are not serializable, however. This is by design, as the bare state defined by the package
 * {@link xyz.cloudkeeper.model.bare} is sufficient to reconstruct CloudKeeper objects. If serialization is
 * needed, objects should be converted to one of the Java Bean-style classes of package
 * {@link xyz.cloudkeeper.model.beans}. On the receiving end, this linker may then be used again to reconstruct
 * the linked runtime representation.
 *
 * <p>Since the object graph created by the linker necessarily contains cycles, most classes in this package have at
 * least some mutable fields, which are initialized with default values by the constructor and only updated later as
 * part of finishing the object construction. Objects are then "frozen", meaning that they become effectively immutable.
 * Any object created by this package is made effectively immutable <em>before</em> being exposed to client code outside
 * of this package. Therefore, this package is entirely thread-safe.
 */
@NonNullByDefault
package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.util.NonNullByDefault;
