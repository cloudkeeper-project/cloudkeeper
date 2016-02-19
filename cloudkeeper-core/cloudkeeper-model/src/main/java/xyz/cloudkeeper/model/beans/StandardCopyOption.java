package xyz.cloudkeeper.model.beans;

/**
 * Defines the standard copy options that can be used with {@code copyOf...} static factory methods.
 */
public enum StandardCopyOption implements CopyOption {
    /**
     * Do not copy the source location provided by {@link xyz.cloudkeeper.model.bare.BareLocatable#getLocation()}.
     */
    STRIP_LOCATION
}
