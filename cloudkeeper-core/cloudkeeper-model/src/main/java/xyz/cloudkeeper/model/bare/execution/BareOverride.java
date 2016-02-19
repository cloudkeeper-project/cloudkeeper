package xyz.cloudkeeper.model.bare.execution;

import xyz.cloudkeeper.model.bare.element.BareAnnotatedConstruct;

import java.util.List;

public interface BareOverride extends BareAnnotatedConstruct {
    /**
     * Returns a list of targets (such as execution traces or element references) that the declared annotations apply
     * to.
     *
     * @return list of targets (such as execution traces or element references) that the declared annotations apply;
     *     guaranteed to be non-null
     */
    List<? extends BareOverrideTarget> getTargets();
}
