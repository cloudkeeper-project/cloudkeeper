package xyz.cloudkeeper.model.runtime.element;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.BareBundle;
import xyz.cloudkeeper.model.immutable.element.Version;
import xyz.cloudkeeper.model.util.ImmutableList;

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
