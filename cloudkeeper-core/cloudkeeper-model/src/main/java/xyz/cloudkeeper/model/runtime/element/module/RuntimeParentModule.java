package xyz.cloudkeeper.model.runtime.element.module;

import xyz.cloudkeeper.model.bare.element.module.BareParentModule;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;

public interface RuntimeParentModule extends BareParentModule, RuntimeModule {
    /**
     * {@inheritDoc}
     *
     * <p>Children in the returned list may be accessed with the index returned by {@link RuntimeModule#getIndex()}.
     *
     * @see #getIndex()
     */
    @Override
    ImmutableList<? extends RuntimeModule> getModules();

    @Override
    ImmutableList<? extends RuntimePort> getDeclaredPorts();

    /**
     * Returns the child with the given name.
     *
     * @param name simple name of child module
     * @return module or {@code null} if not found
     * @throws NullPointerException if the argument is null
     */
    // TODO: Omit in favor of getEnclosedElement()?
    @Nullable
    RuntimeModule getModule(SimpleName name);

    @Override
    ImmutableList<? extends RuntimeConnection> getConnections();
}
