package xyz.cloudkeeper.model.beans.element.type;

import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import xyz.cloudkeeper.model.beans.MutableLocatable;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;
import xyz.cloudkeeper.model.beans.type.MutableTypeVariable;

import java.lang.reflect.TypeVariable;
import java.util.Collections;

public class MutableTypeParameterElementTest {
    @Factory
    public Object[] contracts() {
        return MutableLocatableContract.contractsFor(MutableTypeParameterElement.class);
    }

    @Test
    public void fromTypeVariable() {
        TypeVariable<?> typeVariable = MutableLocatable.class.getTypeParameters()[0];
        MutableTypeParameterElement actual = MutableTypeParameterElement.fromTypeVariable(typeVariable);
        MutableTypeParameterElement expected = new MutableTypeParameterElement()
            .setSimpleName(typeVariable.getName())
            .setBounds(Collections.<MutableTypeMirror<?>>singletonList(
                new MutableDeclaredType()
                    .setDeclaration(MutableLocatable.class.getName())
                    .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                        new MutableTypeVariable()
                            .setFormalTypeParameter(typeVariable.getName())
                    ))
            ));
        Assert.assertEquals(actual, expected);
    }
}
