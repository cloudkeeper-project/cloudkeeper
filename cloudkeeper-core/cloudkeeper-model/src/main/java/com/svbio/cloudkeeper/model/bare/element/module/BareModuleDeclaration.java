package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;

import javax.annotation.Nullable;

public interface BareModuleDeclaration extends BarePluginDeclaration {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "module declaration";

    /**
     * Calls the visitor method that is appropriate for the actual type of this instance.
     *
     * @param visitor module visitor
     * @param parameter parameter that will be passed to the visit method, may be null
     * @param <T> return type of visitor
     * @return result returned by visitor
     */
    @Nullable
    <T, P> T accept(BareModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter);
}
