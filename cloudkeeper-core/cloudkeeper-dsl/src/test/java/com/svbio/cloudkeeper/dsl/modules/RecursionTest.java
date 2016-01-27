package com.svbio.cloudkeeper.dsl.modules;

import com.svbio.cloudkeeper.dsl.CompositeModule;
import com.svbio.cloudkeeper.dsl.CompositeModulePlugin;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import org.testng.Assert;
import org.testng.annotations.Test;

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
