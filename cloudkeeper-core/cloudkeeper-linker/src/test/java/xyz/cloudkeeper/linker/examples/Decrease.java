package xyz.cloudkeeper.linker.examples;

import xyz.cloudkeeper.model.beans.element.module.MutableInPort;
import xyz.cloudkeeper.model.beans.element.module.MutableOutPort;
import xyz.cloudkeeper.model.beans.element.module.MutablePort;
import xyz.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;

import java.util.Arrays;

public final class Decrease {
    private Decrease() { }

    public static MutableSimpleModuleDeclaration declaration() {
        return new MutableSimpleModuleDeclaration()
            .setSimpleName(Decrease.class.getSimpleName())
            .setPorts(Arrays.<MutablePort<?>>asList(
                new MutableInPort()
                    .setSimpleName("num")
                    .setType(MutableDeclaredType.fromType(Integer.class)),
                new MutableOutPort()
                    .setSimpleName("result")
                    .setType(MutableDeclaredType.fromType(Integer.class))
            ));
    }
}
