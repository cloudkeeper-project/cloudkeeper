package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.StandardCopyOption;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeParameterElement;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeVariable;
import com.svbio.cloudkeeper.model.beans.type.MutableWildcardType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TypesTest {
    @TypePlugin("Simple tagging interface")
    public interface Testable { }

    // Let's make up something bizarre
    @TypePlugin("Interface with bizarre type parameters")
    public interface Foo<
        U extends Collection<? extends Testable> & Comparable<? super T>,
        T extends Comparable<? extends Testable>,
        V extends Foo<U, T, V>
    > extends Comparable<V> { }

    @Test
    public void bizarreTest() {
        MutableTypeDeclaration actualTypeDeclaration = MutableTypeDeclaration.copyOfTypeDeclaration(
            (BareTypeDeclaration) ModuleFactory.getDefault().loadDeclaration(Foo.class),
            StandardCopyOption.STRIP_LOCATION
        );
        MutableTypeDeclaration expectedTypeDeclaration = new MutableTypeDeclaration()
            .setTypeDeclarationKind(BareTypeDeclaration.Kind.INTERFACE)
            .setSimpleName(Shared.simpleNameOfClass(Foo.class))
            .setInterfaces(Collections.<MutableTypeMirror<?>>singletonList(
                new MutableDeclaredType()
                    .setDeclaration(Comparable.class.getName())
                    .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                        new MutableTypeVariable()
                            .setFormalTypeParameter("V")
                    ))
            ))
            .setTypeParameters(Arrays.asList(
                new MutableTypeParameterElement()
                    .setSimpleName("U")
                    .setBounds(Arrays.<MutableTypeMirror<?>>asList(
                        new MutableDeclaredType()
                            .setDeclaration(Collection.class.getName())
                            .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                                new MutableWildcardType()
                                    .setExtendsBound(
                                        new MutableDeclaredType().setDeclaration(Testable.class.getName())
                                    )
                            )),
                        new MutableDeclaredType()
                            .setDeclaration(Comparable.class.getName())
                            .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                                new MutableWildcardType()
                                    .setExtendsBound(
                                        new MutableDeclaredType().setDeclaration(Object.class.getName())
                                    )
                                    .setSuperBound(
                                        new MutableTypeVariable().setFormalTypeParameter("T")
                                    )
                            ))
                    )),
                new MutableTypeParameterElement()
                    .setSimpleName("T")
                    .setBounds(Collections.<MutableTypeMirror<?>>singletonList(
                        new MutableDeclaredType()
                            .setDeclaration(Comparable.class.getName())
                            .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                                new MutableWildcardType()
                                    .setExtendsBound(
                                        new MutableDeclaredType().setDeclaration(Testable.class.getName())
                                    )
                            ))
                    )),
                new MutableTypeParameterElement()
                    .setSimpleName("V")
                    .setBounds(Collections.<MutableTypeMirror<?>>singletonList(
                        new MutableDeclaredType()
                            .setDeclaration(Foo.class.getName())
                            .setTypeArguments(Arrays.<MutableTypeMirror<?>>asList(
                                new MutableTypeVariable().setFormalTypeParameter("U"),
                                new MutableTypeVariable().setFormalTypeParameter("T"),
                                new MutableTypeVariable().setFormalTypeParameter("V")
                            ))
                    ))
            ));

        Assert.assertEquals(actualTypeDeclaration, expectedTypeDeclaration);
        Assert.assertEquals(actualTypeDeclaration.toString(), expectedTypeDeclaration.toString());
    }

    @Test
    public void mixinTest() {
        MutableTypeDeclaration actual = MutableTypeDeclaration.copyOfTypeDeclaration(
            (BareTypeDeclaration) ModuleFactory.getDefault().loadDeclaration(List.class),
            StandardCopyOption.STRIP_LOCATION
        );
        MutableTypeDeclaration expected = new MutableTypeDeclaration()
            .setSimpleName(List.class.getSimpleName())
            .setTypeDeclarationKind(BareTypeDeclaration.Kind.INTERFACE)
            .setTypeParameters(Collections.singletonList(
                new MutableTypeParameterElement()
                    .setSimpleName(List.class.getTypeParameters()[0].getName())
            ));
        Assert.assertEquals(actual, expected);
    }
}
