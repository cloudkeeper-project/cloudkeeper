package xyz.cloudkeeper.model.bare.execution;

import xyz.cloudkeeper.model.bare.element.BareQualifiedNameable;

import javax.annotation.Nullable;

public interface BareElementTarget extends BareOverrideTarget {
    /**
     * Returns the referenced element.
     */
    @Nullable
    BareQualifiedNameable getElement();
}
