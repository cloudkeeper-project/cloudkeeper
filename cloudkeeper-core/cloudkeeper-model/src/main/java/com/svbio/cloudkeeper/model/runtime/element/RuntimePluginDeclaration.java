package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;
import com.svbio.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Plug-in declaration, containing static information about a module, such as the name (identifier).
 */
public interface RuntimePluginDeclaration extends BarePluginDeclaration, RuntimeAnnotatedConstruct, RuntimeElement {
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
    <T, P> T accept(RuntimePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter);

    @Override
    @Nonnull
    Name getQualifiedName();

    /**
     * Returns the package of this plug-in declaration.
     */
    RuntimePackage getPackage();
}
