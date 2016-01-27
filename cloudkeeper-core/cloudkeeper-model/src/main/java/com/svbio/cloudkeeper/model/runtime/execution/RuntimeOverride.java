package com.svbio.cloudkeeper.model.runtime.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeAnnotatedConstruct;

import java.util.List;

public interface RuntimeOverride extends BareOverride, RuntimeAnnotatedConstruct {
    @Override
    List<? extends RuntimeOverrideTarget> getTargets();
}
