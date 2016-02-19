package xyz.cloudkeeper.dsl.modules;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.CompositeModule;
import xyz.cloudkeeper.dsl.CompositeModulePlugin;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.dsl.exception.DanglingChildException;
import xyz.cloudkeeper.dsl.exception.RedundantFieldException;
import xyz.cloudkeeper.dsl.exception.UnassignedFieldException;
import xyz.cloudkeeper.model.immutable.Location;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Test that there is a bijection between {@link xyz.cloudkeeper.model.bare.element.module.BareModule} fields and
 * child modules.
 */
public class ModuleFieldsAndChildrenBijectionTest {
    @CompositeModulePlugin("Test module to verify that child modules are assigned to member fields.")
    public abstract static class DanglingChildModule extends CompositeModule<DanglingChildModule> { {
        child(Sum.class);
    } }

    @Test
    public void danglingChildTest() throws Exception {
        try {
            ModuleFactory.getDefault().create(DanglingChildModule.class);
            Assert.fail("Expected exception.");
        } catch (DanglingChildException exception) {
            Location location = exception.getLocation();
            Assert.assertTrue(location.getSystemId().contains(DanglingChildModule.class.getName()));
            Assert.assertEquals(location.getLineNumber(), 23);
        }
    }

    public abstract static class RedundantChildModule extends CompositeModule<RedundantChildModule> {
        public abstract InPort<Integer> someNumber();
        public abstract InPort<Integer> otherNumber();

        Sum child = child(Sum.class).
            firstPort().from(someNumber()).
            secondPort().from(otherNumber());
        Sum otherChild = child;
    }

    @Test
    public void redundantChildTest() throws Exception {
        try {
            ModuleFactory.getDefault().create(RedundantChildModule.class);
            Assert.fail("Expected exception.");
        } catch (RedundantFieldException exception) {
            Assert.assertEquals(
                new HashSet<>(exception.getFields()),
                new HashSet<>(Arrays.asList("child", "otherChild"))
            );
            Location location = exception.getLocation();
            Assert.assertTrue(location.getSystemId().contains(RedundantChildModule.class.getName()));
            Assert.assertEquals(location.getLineNumber(), 42);
        }
    }

    @CompositeModulePlugin("Test module to verify that all Module fields reference have instances.")
    public abstract static class UnassignedChildModule extends CompositeModule<UnassignedChildModule> {
        Sum sum;
    }

    @Test
    public void unassignedFieldTest() throws Exception {
        try {
            ModuleFactory.getDefault().create(UnassignedChildModule.class);
            Assert.fail("Expected exception.");
        } catch (UnassignedFieldException exception) {
            Assert.assertEquals(exception.getField().getName(), "sum");
        }
    }
}
