package xyz.cloudkeeper.examples.repositories;

import xyz.cloudkeeper.model.bare.element.BareBundle;

public interface TestKitBundleProvider {
    /**
     * Returns a fresh copy of the bare bundle provided by this class.
     */
    BareBundle get();
}
