package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.InvalidClassException;
import com.svbio.cloudkeeper.model.api.Marshaler;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import com.svbio.cloudkeeper.model.immutable.Location;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

final class DSLSerializationDeclaration extends DSLMixinPluginDeclaration implements BareSerializationDeclaration {
    private final SimpleName simpleName;

    DSLSerializationDeclaration(DSLPluginDescriptor descriptor) {
        super(descriptor);
        Class<?> pluginClass = descriptor.getPluginClass();
        if (!Marshaler.class.isAssignableFrom(pluginClass)) {
            throw new InvalidClassException(String.format(
                "Expected subclass of %s, but got %s.", Marshaler.class, pluginClass));
        }
        simpleName = Shared.simpleNameOfClass(pluginClass);
    }

    @Override
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public String toString() {
        return BareSerializationDeclaration.Default.toString(this);
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public SimpleName getSimpleName() {
        return simpleName;
    }
}
