package com.svbio.cloudkeeper.model.beans.execution;

import cloudkeeper.serialization.IntegerMarshaler;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.beans.MutableLocatableContract;
import com.svbio.cloudkeeper.model.beans.XmlRootElementContract;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSiblingConnection;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationNode;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationRoot;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializedString;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import org.testng.annotations.Factory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MutableExecutableTest {
    @Factory
    public Object[] contracts() {
        List<Object> contractsList = new ArrayList<>();
        contractsList.addAll(Arrays.asList(MutableLocatableContract.contractsFor(MutableExecutable.class)));
        contractsList.add(new XmlRootElementContract(
            new MutableExecutable()
                .setModule(
                    new MutableCompositeModule()
                        .setModules(Arrays.<MutableModule<?>>asList(
                            new MutableInputModule()
                                .setSimpleName("zero")
                                .setOutPortType(new MutableDeclaredType().setDeclaration(Integer.class.getName()))
                                .setRaw(
                                    new MutableSerializationRoot()
                                        .setDeclaration(IntegerMarshaler.class.getName())
                                        .setEntries(Collections.<MutableSerializationNode<?>>singletonList(
                                            new MutableSerializedString().setString("0")
                                        ))
                                ),
                            new MutableProxyModule()
                                .setSimpleName("sum")
                                .setDeclaration("test.Sum")
                        ))
                        .setConnections(Collections.<MutableConnection<?>>singletonList(
                            new MutableSiblingConnection()
                                .setFromModule("zero").setFromPort(BareInputModule.OUT_PORT_NAME)
                                .setToModule("sum").setToPort("num1")
                        ))
                )
                .setBundleIdentifiers(Collections.singletonList(
                    URI.create("x-maven:com.svbio.cloudkeeper.examples.bundles:simple:ckbundle.zip:1.0.0-SNAPSHOT")
                ))
        ));
        return contractsList.toArray();
    }
}
