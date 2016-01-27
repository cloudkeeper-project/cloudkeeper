package com.svbio.cloudkeeper.model.bare;

import com.svbio.cloudkeeper.model.immutable.Location;

import javax.annotation.Nullable;

/**
 * A mixin interface to designate that instances may have a source-code location.
 */
public interface BareLocatable {
    /**
     * Returns the source-code location where this object was created, or null if not available.
     */
    @Nullable
    Location getLocation();
}
