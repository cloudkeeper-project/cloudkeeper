/**
 * Defines interfaces for user-defined module and serialization implementations, as well as staging-area
 * implementations.
 *
 * <p>While CloudKeeper components also use or even implement the interfaces in this package, by their nature their
 * primary end users are user-defined core CloudKeeper components.
 *
 * <ul><li>
 *     Instances of {@link xyz.cloudkeeper.model.api.ModuleConnector} are passed to CloudKeeper modules in order
 *     to retrieve inputs and set outputs. However, the CloudKeeper DSL component provides an abstraction layer around
 *     this interface.
 * </li><li>
 *     User-defined {@link xyz.cloudkeeper.model.api.Marshaler} implementations are the core part of a
 *     CloudKeeper serialization plugin.
 * </li><li>
 *     {@link xyz.cloudkeeper.model.api.MarshalContext} and
 *     {@link xyz.cloudkeeper.model.api.UnmarshalContext} are mainly to be implemented by staging areas.
 * </li></ul>
 */
@NonNullByDefault
package xyz.cloudkeeper.model.api;

import xyz.cloudkeeper.model.util.NonNullByDefault;
