package xyz.cloudkeeper.model.bare.element.serialization;

import javax.annotation.Nullable;

/**
 * Named byte sequence.
 *
 * A named byte sequence consists of a hierarchical name and a byte array.
 *
 * @see xyz.cloudkeeper.model.bare.element.module.BareInputModule#getRaw()
 */
public interface BareByteSequence extends BareSerializationNode {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "byte-sequence node";

    /**
     * Returns the Java byte array that backs this byte sequence.
     */
    @Nullable
    byte[] getArray();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        public static String toString(BareByteSequence instance) {
            return String.format("%s '%s'", NAME, instance.getKey());
        }
    }
}
