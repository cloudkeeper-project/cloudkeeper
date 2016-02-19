package xyz.cloudkeeper.model.bare.element.module;

import javax.annotation.Nullable;

/**
 * Visitor of modules, in the style of the visitor design pattern.
 */
public interface BareModuleVisitor<T, P> {
    /**
     * Visits an input module.
     */
    @Nullable
    T visitInputModule(BareInputModule inputModule, @Nullable P parameter);

    /**
     * Visits a composite module.
     */
    @Nullable
    T visitCompositeModule(BareCompositeModule compositeModule, @Nullable P parameter);

    /**
     * Visits a loop module.
     */
    @Nullable
    T visitLoopModule(BareLoopModule loopModule, @Nullable P parameter);

    /**
     * Visits a proxy module.
     */
    @Nullable
    T visitLinkedModule(BareProxyModule linkedModule, @Nullable P parameter);
}
