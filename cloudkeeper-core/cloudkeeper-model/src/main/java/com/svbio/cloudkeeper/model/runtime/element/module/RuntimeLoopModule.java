package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;

public interface RuntimeLoopModule extends RuntimeParentModule, BareLoopModule {
    /**
     * Returns the continue-port of this loop module.
     */
    RuntimeOutPort getContinuePort();
}
