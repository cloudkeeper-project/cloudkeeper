package com.svbio.cloudkeeper.model.runtime.element.serialization;

import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;

public interface RuntimeSerializationDeclaration extends RuntimePluginDeclaration, BareSerializationDeclaration {
    /**
     * Returns an instance of the {@link Marshaler} subclass represented by this declaration.
     *
     * <p>Each serialization plug-in is guaranteed to have a unique {@link Marshaler} instance. That is, serialization
     * declarations are safe to be identified by their marshaler instances.
     *
     * @return instance of the {@link Marshaler} subclass represented by this declaration
     */
    Marshaler<?> getInstance();
}
