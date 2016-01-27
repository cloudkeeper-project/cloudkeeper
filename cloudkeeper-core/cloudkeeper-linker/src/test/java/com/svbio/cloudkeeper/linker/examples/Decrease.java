package com.svbio.cloudkeeper.linker.examples;

import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;

import java.util.Arrays;

public class Decrease {
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
