package com.svbio.cloudkeeper.linker.examples;

import cloudkeeper.serialization.IntegerMarshaler;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableIOPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableLoopModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableParentInToChildInConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableShortCircuitConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSiblingConnection;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationNode;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationRoot;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializedString;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.immutable.element.NoKey;

import java.util.Arrays;
import java.util.Collections;

public final class Fibonacci {
    private Fibonacci() { }

    public static MutableCompositeModule template() {
        // It is OK to share instances, but note that setters in this package do *not* create defensive copies.
        final MutableDeclaredType integer = new MutableDeclaredType()
            .setDeclaration(Integer.class.getName());

        // Values. Omit empty token for first value, but not for second.
        final MutableSerializationRoot zero = new MutableSerializationRoot()
            .setDeclaration(IntegerMarshaler.class.getName())
            .setEntries(Collections.<MutableSerializationNode<?>>singletonList(
                new MutableSerializedString().setString("0")
            ));
        final MutableSerializationRoot one = new MutableSerializationRoot()
            .setKey(NoKey.instance())
            .setDeclaration(IntegerMarshaler.class.getName())
            .setEntries(Collections.<MutableSerializationNode<?>>singletonList(
                new MutableSerializedString().setString("1")
            ));

        return new MutableCompositeModule()
            .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName("n")
                    .setType(integer),
                new MutableOutPort()
                    .setSimpleName("result")
                    .setType(integer)
            ))
            .setModules(Arrays.<MutableModule<?>>asList(
                new MutableInputModule()
                    .setSimpleName("zero")
                    .setOutPortType(integer)
                    .setRaw(zero),
                new MutableInputModule()
                    .setSimpleName("one")
                    .setOutPortType(integer)
                    .setRaw(one),

                new MutableLoopModule()
                    .setSimpleName("loop")
                    .setDeclaredPorts(Arrays.<MutablePort<?>>asList(
                        new MutableIOPort().setSimpleName("count").setType(integer),
                        new MutableIOPort().setSimpleName("last").setType(integer),
                        new MutableIOPort().setSimpleName("secondLast").setType(integer)
                    ))
                    .setModules(Arrays.<MutableModule<?>>asList(
                        new MutableInputModule()
                            .setSimpleName("one")
                            .setOutPortType(integer)
                            .setRaw(one),

                        new MutableProxyModule()
                            .setSimpleName("sum")
                            .setDeclaration(BinarySum.class.getName()),
                        new MutableProxyModule()
                            .setSimpleName("decr")
                            .setDeclaration(Decrease.class.getName()),
                        new MutableProxyModule()
                            .setSimpleName("gt")
                            .setDeclaration(GreaterThan.class.getName())
                    ))
                    .setConnections(Arrays.<MutableConnection<?>>asList(
                        new MutableParentInToChildInConnection()
                            .setFromPort("last")
                            .setToModule("sum").setToPort("num1"),
                        new MutableParentInToChildInConnection()
                            .setFromPort("secondLast")
                            .setToModule("sum").setToPort("num2"),
                        new MutableParentInToChildInConnection()
                            .setFromPort("count")
                            .setToModule("decr").setToPort("num"),
                        new MutableSiblingConnection()
                            .setFromModule("decr").setFromPort("result")
                            .setToModule("gt").setToPort("num1"),
                        new MutableSiblingConnection()
                            .setFromModule("one").setFromPort(BareInputModule.OUT_PORT_NAME)
                            .setToModule("gt").setToPort("num2"),

                        new MutableChildOutToParentOutConnection()
                            .setFromModule("decr").setFromPort("result")
                            .setToPort("count"),
                        new MutableChildOutToParentOutConnection()
                            .setFromModule("sum").setFromPort("result")
                            .setToPort("last"),
                        new MutableShortCircuitConnection()
                            .setFromPort("last")
                            .setToPort("secondLast"),

                        new MutableChildOutToParentOutConnection()
                            .setFromModule("gt").setFromPort("result")
                            .setToPort(BareLoopModule.CONTINUE_PORT_NAME)
                    ))
            ))
            .setConnections(Arrays.<MutableConnection<?>>asList(
                new MutableParentInToChildInConnection()
                    .setFromPort("n")
                    .setToModule("loop").setToPort("count"),
                new MutableSiblingConnection()
                    .setFromModule("one").setFromPort(BareInputModule.OUT_PORT_NAME)
                    .setToModule("loop").setToPort("last"),
                new MutableSiblingConnection()
                    .setFromModule("zero").setFromPort(BareInputModule.OUT_PORT_NAME)
                    .setToModule("loop").setToPort("secondLast"),

                new MutableChildOutToParentOutConnection()
                    .setFromModule("loop").setFromPort("last")
                    .setToPort("result")
            ));
    }

    public static MutableCompositeModuleDeclaration declaration() {
        return new MutableCompositeModuleDeclaration()
            .setSimpleName(Fibonacci.class.getSimpleName())
            .setTemplate(template());
    }
}
