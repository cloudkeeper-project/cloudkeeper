package com.svbio.cloudkeeper.model.bare.element;

import javax.annotation.Nullable;

public interface BarePluginDeclaration extends BareElement, BareSimpleNameable {
    /**
     * Applies a visitor to this plug-in declaration.
     *
     * @param visitor the visitor operating on this plug-in declaration
     * @param parameter additional parameter to the visitor
     * @param <T> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @return a visitor-specified result
     */
    @Nullable
    <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter);
}
