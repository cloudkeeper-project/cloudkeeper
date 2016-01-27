package com.svbio.cloudkeeper.model.beans.element.type;

import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeVariable;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MutableTypeDeclarationTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableTypeDeclaration.class);
    }

    @Test
    public void fromClass() {
        MutableTypeDeclaration actual = MutableTypeDeclaration.fromClass(List.class);
        TypeVariable<?> typeVariable = List.class.getTypeParameters()[0];
        MutableTypeDeclaration expected = new MutableTypeDeclaration()
            .setSimpleName(List.class.getSimpleName())
            .setInterfaces(Collections.<MutableTypeMirror<?>>singletonList(
                new MutableDeclaredType()
                    .setDeclaration(Collection.class.getName())
                    .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                        new MutableTypeVariable()
                            .setFormalTypeParameter(typeVariable.getName())
                    ))
            ))
            .setTypeDeclarationKind(BareTypeDeclaration.Kind.INTERFACE)
            .setTypeParameters(Collections.singletonList(
                new MutableTypeParameterElement()
                    .setSimpleName(typeVariable.getName())
            ));
        Assert.assertEquals(actual, expected);
    }
}
