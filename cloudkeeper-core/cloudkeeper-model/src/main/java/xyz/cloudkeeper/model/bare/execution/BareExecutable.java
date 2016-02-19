package xyz.cloudkeeper.model.bare.execution;

import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.bare.element.module.BareModule;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

public interface BareExecutable extends BareLocatable {
    /**
     * Returns the module to execute.
     *
     * There are no restrictions on the module that can be executed. It can be an anonymous module or a declared module.
     */
    @Nullable
    BareModule getModule();

    /**
     * Returns the annotation overrides.
     */
    List<? extends BareOverride> getOverrides();

    /**
     * Returns the identifiers of all bundles needed for this executable.
     */
    List<URI> getBundleIdentifiers();
}
