package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.BarePackage;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import java.net.URI;

/**
 * Linked and verified CloudKeeper package.
 */
public interface RuntimePackage extends RuntimeElement, BarePackage, Immutable {
    @Override
    ImmutableList<? extends RuntimePluginDeclaration> getDeclarations();

    /**
     * Returns the name of the bundle that provides this pacakge.
     *
     * @return name of bundle that provides this package, guaranteed to be non-null
     * @see RuntimeBundle#getBundleIdentifier()
     */
    URI getBundleIdentifier();
}
