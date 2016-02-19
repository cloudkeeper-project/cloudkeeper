package com.svbio.cloudkeeper.linker;

import cloudkeeper.annotations.CloudKeeperSerialization;
import cloudkeeper.serialization.IntegerMarshaler;
import com.svbio.cloudkeeper.linker.examples.BinarySum;
import com.svbio.cloudkeeper.linker.examples.Decrease;
import com.svbio.cloudkeeper.linker.examples.Fibonacci;
import com.svbio.cloudkeeper.linker.examples.GreaterThan;
import com.svbio.cloudkeeper.linker.examples.MapMixin;
import com.svbio.cloudkeeper.linker.examples.Memory;
import com.svbio.cloudkeeper.linker.examples.SortedMapMarshaler;
import com.svbio.cloudkeeper.linker.examples.SortedMapMixin;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.beans.element.MutablePackage;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import com.svbio.cloudkeeper.model.beans.execution.MutableElementTarget;
import com.svbio.cloudkeeper.model.beans.execution.MutableExecutable;
import com.svbio.cloudkeeper.model.beans.execution.MutableExecutionTraceTarget;
import com.svbio.cloudkeeper.model.beans.execution.MutableOverride;
import com.svbio.cloudkeeper.model.beans.execution.MutableOverrideTarget;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class Examples {
    private Examples() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    public static MutableBundle simpleBundle() {
        return new MutableBundle()
            .setBundleIdentifier(
                URI.create("x-maven:com.svbio.cloudkeeper.examples.bundles:simple:ckbundle.zip:1.0.0-SNAPSHOT")
            )
            .setCreationTime(new Date(1388577600000L))
            .setPackages(Arrays.asList(
                new MutablePackage()
                    .setQualifiedName("java.util")
                    .setDeclarations(Arrays.<MutablePluginDeclaration<?>>asList(
                        SortedMapMixin.declaration(),
                        MapMixin.declaration()
                    )),
                new MutablePackage()
                    .setQualifiedName(BinarySum.class.getPackage().getName())
                    .setDeclarations(Arrays.<MutablePluginDeclaration<?>>asList(
                        BinarySum.declaration(),
                        Decrease.declaration(),
                        GreaterThan.declaration(),

                        Memory.Beans.declaration(),

                        SortedMapMarshaler.Beans.declaration()
                    ))
            ));
    }

    public static MutableBundle fibonacciBundle() {
        return new MutableBundle()
            .setBundleIdentifier(
                URI.create("x-maven:com.svbio.cloudkeeper.examples.bundles:fibonacci:ckbundle.zip:"
                    + "0.0.1-SNAPSHOT")
            )
            .setCreationTime(new Date(1388577600000L))
            .setPackages(Collections.singletonList(
                new MutablePackage()
                    .setQualifiedName(Fibonacci.class.getPackage().getName())
                    .setDeclarations(Collections.<MutablePluginDeclaration<?>>singletonList(
                        Fibonacci.declaration()
                    ))
            ));
    }

    public static List<MutableOverride> fibonacciOverrides() {
        MutableAnnotation integerSerializationAnnotation = new MutableAnnotation()
            .setDeclaration(CloudKeeperSerialization.class.getName())
            .setEntries(Collections.singletonList(
                new MutableAnnotationEntry()
                    .setKey("value")
                    .setValue(new String[]{IntegerMarshaler.class.getName()})
            ));

        return Arrays.asList(
            new MutableOverride()
                .setTargets(Collections.<MutableOverrideTarget<?>>singletonList(
                    new MutableElementTarget()
                        .setElement("com.svbio.cloudkeeper.examples.modules.Decrease.num")
                ))
                .setDeclaredAnnotations(Collections.singletonList(integerSerializationAnnotation)),
            new MutableOverride()
                .setTargets(Collections.<MutableOverrideTarget<?>>singletonList(
                    new MutableExecutionTraceTarget()
                        .setExecutionTrace("/loop/sum:in:num1")
                ))
                .setDeclaredAnnotations(Collections.singletonList(integerSerializationAnnotation))
        );
    }

    public static MutableExecutable fibonacciExecutable() {
        return new MutableExecutable()
            .setModule(Fibonacci.template())
            .setBundleIdentifiers(Collections.singletonList(
                URI.create("x-maven:com.svbio.cloudkeeper.examples.bundles:simple:ckbundle.zip:1.0.0-SNAPSHOT")
            ))
            .setOverrides(fibonacciOverrides());
    }
}
