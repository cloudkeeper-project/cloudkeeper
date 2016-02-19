package xyz.cloudkeeper.linker.examples;

import cloudkeeper.annotations.CloudKeeperSerialization;
import cloudkeeper.serialization.IntegerMarshaler;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import xyz.cloudkeeper.model.beans.element.module.MutableInPort;
import xyz.cloudkeeper.model.beans.element.module.MutableOutPort;
import xyz.cloudkeeper.model.beans.element.module.MutablePort;
import xyz.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;

import java.util.Arrays;
import java.util.Collections;

@Memory(value = 128, unit = "m")
public final class BinarySum {
    private BinarySum() { }

    public static MutableSimpleModuleDeclaration declaration() {
        return new MutableSimpleModuleDeclaration()
            .setSimpleName(BinarySum.class.getSimpleName())
            .setPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName("num1")
                    .setType(MutableDeclaredType.fromType(Integer.class)),
                new MutableInPort()
                    .setSimpleName("num2")
                    .setType(MutableDeclaredType.fromType(Integer.class)),
                new MutableOutPort()
                    .setSimpleName("result")
                    .setType(MutableDeclaredType.fromType(Integer.class))
                    .setDeclaredAnnotations(Collections.singletonList(
                        new MutableAnnotation()
                            .setDeclaration(CloudKeeperSerialization.class.getName())
                            .setEntries(Collections.singletonList(
                                new MutableAnnotationEntry()
                                    .setKey("value")
                                    .setValue(new String[]{
                                        IntegerMarshaler.class.getName()
                                    })
                            ))
                    ))
            ))
            .setDeclaredAnnotations(Collections.singletonList(
                MutableAnnotation.fromAnnotation(BinarySum.class.getAnnotation(Memory.class))
            ));
    }
}
