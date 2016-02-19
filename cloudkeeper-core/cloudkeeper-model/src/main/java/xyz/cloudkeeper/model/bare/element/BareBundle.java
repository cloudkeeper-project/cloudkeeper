package xyz.cloudkeeper.model.bare.element;

import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.immutable.element.Version;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Date;
import java.util.List;

public interface BareBundle extends BareLocatable {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "bundle";

    /**
     * Returns the creation time/date of this repository.
     */
    @Nullable
    Date getCreationTime();

    /**
     * Returns the CloudKeeper version used to create this repository.
     */
    @Nullable
    Version getCloudKeeperVersion();

    /**
     * Returns the identifier of this repository.
     *
     * @return bundle identifier
     */
    @Nullable
    URI getBundleIdentifier();

    /**
     * Returns a list of all packages in this repository.
     *
     * @return list of all packages in this repository, guaranteed non-null
     */
    List<? extends BarePackage> getPackages();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        public static String toString(BareBundle instance) {
            return String.format(
                "%s '%s' (%d packages)", NAME, instance.getBundleIdentifier(), instance.getPackages().size()
            );
        }
    }
}
