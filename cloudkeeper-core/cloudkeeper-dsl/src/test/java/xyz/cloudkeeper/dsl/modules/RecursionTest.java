package xyz.cloudkeeper.dsl.modules;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.CompositeModule;
import xyz.cloudkeeper.dsl.CompositeModulePlugin;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareProxyModule;

public class RecursionTest {
    @CompositeModulePlugin("Recursive module")
    public abstract static class RecursionModule extends CompositeModule<RecursionModule> {
        public abstract InPort<Integer> number();
        public abstract OutPort<Integer> result();

        RecursionModule recursionTest = child(RecursionModule.class).
            number().from(number());

        { result().from(recursionTest.result()); }
    }

    @Test
    public void recursionTest() throws Exception {
        RecursionModule recursionModule = ModuleFactory.getDefault().create(RecursionModule.class);

        Assert.assertEquals(recursionModule.getModules().size(), 1);
        BareModule childRecursionTest = recursionModule.getModules().get(0);
        Assert.assertTrue(childRecursionTest instanceof BareProxyModule);
        Assert.assertEquals(
            ((BareProxyModule) childRecursionTest).getDeclaration().getQualifiedName().toString(),
            RecursionModule.class.getName()
        );
    }
}
