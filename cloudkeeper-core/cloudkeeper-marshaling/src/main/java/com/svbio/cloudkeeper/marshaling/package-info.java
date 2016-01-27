/**
 * Interfaces and classes for transforming between Java objects and tree representations suitable for storage or
 * transmission.
 *
 * <p>The package is versatile enough to support both in-memory tree-like data structures as well as implicit tree
 * representations (such as a file system). While self-contained by-value transmission is the standard case,
 * by-reference representations are supported, too, in case sender and receiver share common memory/storage.
 */
@NonNullByDefault
package com.svbio.cloudkeeper.marshaling;

import com.svbio.cloudkeeper.model.util.NonNullByDefault;
