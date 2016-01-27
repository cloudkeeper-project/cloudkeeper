package com.svbio.cloudkeeper.model.bare.element.serialization;

import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;

/**
 * Marshaler Declaration.
 *
 * <p>In order to support distributed processing of workflows with simple modules written in a JVM-based language,
 * CloudKeeper needs to serialize and deserialize the values received from out-ports or sent to in-ports. Even if a
 * workflow was processed entirely within a single machine, serializing values would still be necessary for external
 * representation of CloudKeeper workflows that contain input modules (for example, as XML).
 *
 * Therefore, at all <em>serialization contexts</em> have a list of serialization classes. When a serialization class
 * asks
 *
 * Marshaler then proceeds as follows:
 * <ul><li>
 *     Determine the dynamic type of the object to be serialized. Since generic type information is not maintained at
 *     runtime by the JVM, this is always just a {@link Class} object.
 * </li><li>
 *     Scan the list of serialization classes, and choose the first one that is capable of serializing and deserializing
 *     the class determined in the previous step.
 * </li></ul>
 *
 *
 *
 * CloudKeeper then scans this list sequentially, and the first serialization class
 */
public interface BareSerializationDeclaration extends BarePluginDeclaration {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "serialization declaration";


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareSerializationDeclaration#toString()}.
         */
        public static String toString(BareSerializationDeclaration instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
