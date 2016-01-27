package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.immutable.element.Version;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * Bundle.
 *
 * <p>This interface is the immutable runtime equivalent of {@link BareBundle}.
 */
public interface RuntimeBundle extends BareBundle, Immutable {
    @Nonnull
    @Override
    Version getCloudKeeperVersion();

    @Nonnull
    @Override
    URI getBundleIdentifier();

    @Override
    ImmutableList<? extends RuntimePackage> getPackages();
}
