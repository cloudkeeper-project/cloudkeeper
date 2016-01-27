package com.svbio.cloudkeeper.linker.examples;

import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeParameterElement;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeVariable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;

public final class SortedMapMixin {
    private SortedMapMixin() { }

    public static MutableTypeDeclaration declaration() {
        return new MutableTypeDeclaration()
            .setSimpleName(SortedMap.class.getSimpleName())
            .setTypeDeclarationKind(BareTypeDeclaration.Kind.INTERFACE)
            .setTypeParameters(Arrays.asList(
                new MutableTypeParameterElement()
                    .setSimpleName("K")
                    .setBounds(
                        Collections.<MutableTypeMirror<?>>singletonList(MutableDeclaredType.fromType(Object.class))
                    ),
                new MutableTypeParameterElement()
                    .setSimpleName("V")
                // omitting bounds defaults to Object upper bound
            ))
            .setInterfaces(Collections.<MutableTypeMirror<?>>singletonList(
                new MutableDeclaredType()
                    .setDeclaration(Map.class.getName())
                    .setTypeArguments(Arrays.<MutableTypeMirror<?>>asList(
                        new MutableTypeVariable().setFormalTypeParameter("K"),
                        new MutableTypeVariable().setFormalTypeParameter("V")
                    ))
            ));
    }
}
