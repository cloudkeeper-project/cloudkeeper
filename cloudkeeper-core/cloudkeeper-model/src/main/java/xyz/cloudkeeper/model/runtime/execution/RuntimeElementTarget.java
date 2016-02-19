package xyz.cloudkeeper.model.runtime.execution;

import xyz.cloudkeeper.model.bare.execution.BareElementTarget;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;

import javax.annotation.Nonnull;

public interface RuntimeElementTarget extends RuntimeOverrideTarget, BareElementTarget {
    @Override
    @Nonnull
    RuntimeElement getElement();
}
