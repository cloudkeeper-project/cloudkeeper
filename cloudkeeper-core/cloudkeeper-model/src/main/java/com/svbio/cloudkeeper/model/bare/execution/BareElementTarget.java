package com.svbio.cloudkeeper.model.bare.execution;

import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;

import javax.annotation.Nullable;

public interface BareElementTarget extends BareOverrideTarget {
    /**
     * Returns the referenced element.
     */
    @Nullable
    BareQualifiedNameable getElement();
}
