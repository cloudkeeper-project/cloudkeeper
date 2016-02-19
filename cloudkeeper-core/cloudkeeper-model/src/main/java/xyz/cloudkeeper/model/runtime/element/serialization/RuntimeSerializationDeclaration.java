package xyz.cloudkeeper.model.runtime.element.serialization;

import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;

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
