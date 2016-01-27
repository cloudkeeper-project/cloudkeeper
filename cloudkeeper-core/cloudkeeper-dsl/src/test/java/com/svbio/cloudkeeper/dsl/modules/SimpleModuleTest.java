package com.svbio.cloudkeeper.dsl.modules;

import cloudkeeper.serialization.IntegerMarshaler;
import cloudkeeper.serialization.StringMarshaler;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.dsl.SimpleModule;
import com.svbio.cloudkeeper.dsl.SimpleModulePlugin;
import com.svbio.cloudkeeper.model.CloudKeeperSerialization;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.StandardCopyOption;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import org.testng.Assert;
import org.testng.annotations.Test;

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
