/**
 * Defines interfaces that provide an optimized and fully linked representation of the CloudKeeper domain model.
 *
 * <p>The interfaces in this package are extensions to those from package {@link com.svbio.cloudkeeper.model.bare}. They
 * provide an optimized representation of the CloudKeeper domain model containing all runtime state necessary for
 * execution.
 *
 * <p>The interfaces in this package provide a linked (resolved) and thus optimized representation of
 * <em>abstract syntax trees</em>. A module representation can be immediately interpreted by the CloudKeeper
 * interpreter.
 */
@NonNullByDefault
package com.svbio.cloudkeeper.model.runtime;

import com.svbio.cloudkeeper.model.util.NonNullByDefault;
