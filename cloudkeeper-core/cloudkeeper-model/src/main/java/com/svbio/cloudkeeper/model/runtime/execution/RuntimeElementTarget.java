package com.svbio.cloudkeeper.model.runtime.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareElementTarget;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;

import javax.annotation.Nonnull;

public interface RuntimeElementTarget extends RuntimeOverrideTarget, BareElementTarget {
    @Override
    @Nonnull
    RuntimeElement getElement();
}
