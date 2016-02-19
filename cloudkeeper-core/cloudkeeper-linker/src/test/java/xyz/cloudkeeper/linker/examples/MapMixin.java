package xyz.cloudkeeper.linker.examples;

import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import xyz.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import xyz.cloudkeeper.model.beans.element.type.MutableTypeParameterElement;

import java.util.Arrays;
import java.util.Map;

public final class MapMixin {
    private MapMixin() { }

    public static MutableTypeDeclaration declaration() {
        return new MutableTypeDeclaration()
            .setSimpleName(Map.class.getSimpleName())
            .setTypeDeclarationKind(BareTypeDeclaration.Kind.INTERFACE)
            .setTypeParameters(Arrays.asList(
                new MutableTypeParameterElement()
                    .setSimpleName("K"),
                new MutableTypeParameterElement()
                    .setSimpleName("V")
            ));
    }
}
