package xyz.cloudkeeper.linker.examples;

import xyz.cloudkeeper.model.beans.element.module.MutableInPort;
import xyz.cloudkeeper.model.beans.element.module.MutableOutPort;
import xyz.cloudkeeper.model.beans.element.module.MutablePort;
import xyz.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;

import java.util.Arrays;

public final class GreaterThan {
    private GreaterThan() { }

    public static MutableSimpleModuleDeclaration declaration() {
        return new MutableSimpleModuleDeclaration()
            .setSimpleName(GreaterThan.class.getSimpleName())
            .setPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName("num1")
                    .setType(MutableDeclaredType.fromType(Integer.class)),
                new MutableInPort()
                    .setSimpleName("num2")
                    .setType(MutableDeclaredType.fromType(Integer.class)),
                new MutableOutPort()
                    .setSimpleName("result")
                    .setType(new MutableDeclaredType().setDeclaration(Boolean.class.getName()))
            ));
    }
}
