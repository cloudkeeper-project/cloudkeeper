package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareLoopModule;

public interface RuntimeLoopModule extends RuntimeParentModule, BareLoopModule {
    /**
     * Returns the continue-port of this loop module.
     */
    RuntimeOutPort getContinuePort();
}
