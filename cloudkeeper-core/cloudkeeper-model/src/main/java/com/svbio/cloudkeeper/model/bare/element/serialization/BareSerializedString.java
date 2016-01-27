package com.svbio.cloudkeeper.model.bare.element.serialization;

import javax.annotation.Nullable;

public interface BareSerializedString extends BareSerializationNode {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "serialized-as-string";

    /**
     * Returns the string representation of this byte sequence data.
     */
    @Nullable
    String getString();

    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        public static String toString(BareSerializedString instance) {
            return String.format("%s %s", NAME, instance.getKey());
        }
    }
}
