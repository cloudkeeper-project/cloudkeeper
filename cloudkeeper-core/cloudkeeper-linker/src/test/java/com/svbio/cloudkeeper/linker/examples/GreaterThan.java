package com.svbio.cloudkeeper.linker.examples;

import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;

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
