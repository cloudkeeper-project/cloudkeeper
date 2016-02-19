package xyz.cloudkeeper.model.runtime.execution;

import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.runtime.element.RuntimeAnnotatedConstruct;

import java.util.List;

public interface RuntimeOverride extends BareOverride, RuntimeAnnotatedConstruct {
    @Override
    List<? extends RuntimeOverrideTarget> getTargets();
}
