package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.dsl.exception.InvalidClassException;
import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

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
