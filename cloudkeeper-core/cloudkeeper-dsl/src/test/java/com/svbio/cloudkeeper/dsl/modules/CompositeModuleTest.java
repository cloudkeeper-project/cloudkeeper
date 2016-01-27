package com.svbio.cloudkeeper.dsl.modules;

import cloudkeeper.serialization.IntegerMarshaler;
import cloudkeeper.serialization.SerializableMarshaler;
import com.svbio.cloudkeeper.dsl.CompositeModule;
import com.svbio.cloudkeeper.dsl.CompositeModulePlugin;
import com.svbio.cloudkeeper.dsl.InputModule;
import com.svbio.cloudkeeper.dsl.ModuleFactory;
import com.svbio.cloudkeeper.model.CloudKeeperSerialization;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.beans.StandardCopyOption;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class CompositeModuleTest {
    @CompositeModulePlugin("Simple composite with an input module")
    public abstract static class CompositeWithInput extends CompositeModule<CompositeWithInput> {
        public abstract InPort<Collection<Integer>> number();
        public abstract OutPort<Integer> list();

        @CloudKeeperSerialization({ IntegerMarshaler.class, SerializableMarshaler.class })
        InputModule<Integer> one = value(42);

        { list().from(one); }
    }

    @Test
    public void testCompositeModule() throws Exception {
        // Create DSL and "manual" representation of same declaration
        MutableCompositeModuleDeclaration actual = MutableCompositeModuleDeclaration.copyOfCompositeModuleDeclaration(
            (BareCompositeModuleDeclaration) ModuleFactory.getDefault().loadDeclaration(CompositeWithInput.class),
            StandardCopyOption.STRIP_LOCATION
        );

        CompositeWithInput dslModule = ModuleFactory.getDefault().create(CompositeWithInput.class);
        MutableCompositeModuleDeclaration expected = new MutableCompositeModuleDeclaration()
            .setSimpleName(
                CompositeWithInput.class.getName().substring(
                    CompositeWithInput.class.getPackage().getName().length() + 1
                )
            )
            .setTemplate(
                new MutableCompositeModule()
                    .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                        new MutableInPort()
                            .setSimpleName(dslModule.number().getSimpleName())
                            .setType(
                                new MutableDeclaredType()
                                    .setDeclaration(Collection.class.getName())
                                    .setTypeArguments(Collections.<MutableTypeMirror<?>>singletonList(
                                        new MutableDeclaredType().setDeclaration(Integer.class.getName())
                                    ))
                            ),
                        new MutableOutPort()
                            .setSimpleName(dslModule.list().getSimpleName())
                            .setType(
                                new MutableDeclaredType().setDeclaration(Integer.class.getName())
                            )
                    ))
                    .setModules(Collections.<MutableModule<?>>singletonList(
                        new MutableInputModule()
                            .setSimpleName(dslModule.one.getSimpleName())
                            .setValue(42)
                            .setOutPortType(new MutableDeclaredType().setDeclaration(Integer.class.getName()))
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
                                                SerializableMarshaler.class.getName()
                                            })
                                    ))
                            ))
                    ))
                    .setConnections(Collections.<MutableConnection<?>>singletonList(
                        new MutableChildOutToParentOutConnection()
                            .setFromPort(BareInputModule.OUT_PORT_NAME)
                            .setFromModule(dslModule.one.getSimpleName().toString())
                            .setToPort(dslModule.list().getSimpleName().toString())
                    ))
            );

        // They should be equal!
        Assert.assertEquals(actual, expected);
    }
}
