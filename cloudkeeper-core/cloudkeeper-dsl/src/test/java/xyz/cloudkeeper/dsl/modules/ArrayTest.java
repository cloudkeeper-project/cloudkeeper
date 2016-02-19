package xyz.cloudkeeper.dsl.modules;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;
import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import xyz.cloudkeeper.model.bare.type.BareDeclaredType;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;

import java.util.Collection;

public class ArrayTest {
    @SimpleModulePlugin("Test module to verify that array type are detected properly.")
    public abstract static class ArrayModule extends SimpleModule<ArrayModule> {
        public abstract InPort<Collection<Collection<Integer>>> arrayInPort();
    }

    @Test
    public void test() throws Exception {
        ArrayModule arrayModule = ModuleFactory.getDefault().create(ArrayModule.class);

        Assert.assertEquals(arrayModule.getLocation().getLineNumber(), 23);

        BareSimpleModuleDeclaration declaration
            = (BareSimpleModuleDeclaration) ModuleFactory.getDefault().loadDeclaration(ArrayModule.class);
        int declarationLineNumber = ((BareLocatable) declaration).getLocation().getLineNumber();
        Assert.assertTrue(17 <= declarationLineNumber && declarationLineNumber <= 19);

        MutableTypeMirror<?> portType = MutableTypeMirror.copyOfTypeMirror(arrayModule.arrayInPort().getType());
        Assert.assertEquals(
            ((BareDeclaredType) portType).getDeclaration().getQualifiedName().toString(),
            Collection.class.getName()
        );
        Assert.assertEquals((
                (BareDeclaredType) (
                    (BareDeclaredType) (
                        (BareDeclaredType) portType
                    ).getTypeArguments().get(0)
                ).getTypeArguments().get(0)
            ).getDeclaration().getQualifiedName().toString(),
            Integer.class.getName()
        );
    }
}
