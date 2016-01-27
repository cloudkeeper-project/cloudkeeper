package com.svbio.cloudkeeper.model.bare.element.module;

import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;

import javax.annotation.Nullable;

/**
 * Input module.
 */
public interface BareInputModule extends BareModule {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "input module";

    /**
     * Name of the out-port of an input module.
     */
    String OUT_PORT_NAME = "value";

    /**
     * Returns the type of the implicit out-port of this input module.
     */
    @Nullable
    BareTypeMirror getOutPortType();

    /**
     * Returns the value provided by this input module, represented as tree suitable for storage and transmission.
     */
    @Nullable
    BareSerializationRoot getRaw();

    /**
     * Returns the value assigned to this input module.
     */
    @Nullable
    Object getValue();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareInputModule#toString()}.
         */
        public static String toString(BareInputModule instance) {
            @Nullable Object value = instance.getValue();
            String content = instance.getOutPortType() + ": "
                + (value != null
                    ? value.toString()
                    : "<raw>"
                );
            if (content.length() > 64) {
                content = content.substring(0, 61) + "...";
            }
            return String.format("%s %s (%s)", NAME, instance.getSimpleName(), content);
        }
    }
}
