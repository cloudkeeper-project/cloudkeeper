package xyz.cloudkeeper.dsl.modules;

import cloudkeeper.serialization.IntegerMarshaler;
import cloudkeeper.serialization.StringMarshaler;
import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.dsl.ModuleFactory;
import xyz.cloudkeeper.dsl.SimpleModule;
import xyz.cloudkeeper.dsl.SimpleModulePlugin;
import xyz.cloudkeeper.model.CloudKeeperSerialization;
import xyz.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import xyz.cloudkeeper.model.beans.StandardCopyOption;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import xyz.cloudkeeper.model.beans.element.module.MutableInPort;
import xyz.cloudkeeper.model.beans.element.module.MutableOutPort;
import xyz.cloudkeeper.model.beans.element.module.MutablePort;
import xyz.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;

import java.util.Arrays;
import java.util.Collections;

public class SimpleModuleTest {
    @SimpleModulePlugin("Simple Module")
    public abstract static class SomeModule extends SimpleModule<SomeModule> implements Runnable {
        public abstract InPort<Integer> inNumber();

        @CloudKeeperSerialization({ IntegerMarshaler.class, StringMarshaler.class })
        public abstract OutPort<Integer> result();

        @Override
        public void run() {
            int inNumber = inNumber().get();

            result().set(2 * inNumber);
        }
    }

    @Test
    public void simpleModule() {
        MutableSimpleModuleDeclaration actual = MutableSimpleModuleDeclaration.copyOfSimpleModuleDeclaration(
            (BareSimpleModuleDeclaration) ModuleFactory.getDefault().loadDeclaration(SomeModule.class),
            StandardCopyOption.STRIP_LOCATION
        );

        SomeModule someModule = ModuleFactory.getDefault().create(SomeModule.class);
        MutableSimpleModuleDeclaration expected = new MutableSimpleModuleDeclaration()
            .setSimpleName(SomeModule.class.getName().substring(SomeModule.class.getPackage().getName().length() + 1))
            .setPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName(someModule.inNumber().getSimpleName())
                    .setType(new MutableDeclaredType().setDeclaration(Integer.class.getName())),
                new MutableOutPort()
                    .setSimpleName(someModule.result().getSimpleName())
                    .setType(new MutableDeclaredType().setDeclaration(Integer.class.getName()))
                    .setDeclaredAnnotations(Collections.singletonList(
                        new MutableAnnotation()
                            .setDeclaration(
                                cloudkeeper.annotations.CloudKeeperSerialization.class.getName()
                            )
                            .setEntries(Collections.singletonList(
                                new MutableAnnotationEntry()
                                    .setKey("value")
                                    .setValue(new String[]{
                                        IntegerMarshaler.class.getName(),
                                        StringMarshaler.class.getName()
                                    })
                            ))
                    ))
            ));

        // They should be equal!
        Assert.assertEquals(actual, expected);
    }
}
