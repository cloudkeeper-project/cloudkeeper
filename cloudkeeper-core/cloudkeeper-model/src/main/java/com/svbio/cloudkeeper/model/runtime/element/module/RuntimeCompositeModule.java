package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;

/**
 * Composite module with runtime state.
 *
 * This interface models a composite module as part of an optimized abstract syntax tree with runtime state information.
 */
public interface RuntimeCompositeModule extends RuntimeParentModule, BareCompositeModule { }
