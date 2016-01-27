package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.bare.type.BarePrimitiveType;
import com.svbio.cloudkeeper.model.beans.StandardCopyOption;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeElement;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.beans.type.MutablePrimitiveType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class AnnotationTest {
    @AnnotationTypePlugin("Annotation type")
    @interface Requirements {
        int cpuCores() default 2;
        String tag() default "foo";
        double factor();
    }

    @Test
    public void annotationTest() {
        MutableAnnotationTypeDeclaration actual = MutableAnnotationTypeDeclaration.copyOfAnnotationTypeDeclaration(
            (BareAnnotationTypeDeclaration) ModuleFactory.getDefault().loadDeclaration(Requirements.class),
            StandardCopyOption.STRIP_LOCATION
        );
        Collections.sort(actual.getElements(), new Comparator<MutableAnnotationTypeElement>() {
            @Override
            public int compare(MutableAnnotationTypeElement first, MutableAnnotationTypeElement second) {
                return first.getSimpleName().compareTo(second.getSimpleName());
            }
        });

        MutableAnnotationTypeDeclaration expected = new MutableAnnotationTypeDeclaration()
            .setSimpleName(Shared.simpleNameOfClass(Requirements.class))
            .setElements(Arrays.asList(
                new MutableAnnotationTypeElement()
                    .setSimpleName("cpuCores")
                    .setReturnType(
                        new MutablePrimitiveType().setPrimitiveKind(BarePrimitiveType.Kind.INT)
                    )
                    .setDefaultValue(2),
                new MutableAnnotationTypeElement()
                    .setSimpleName("factor")
                    .setReturnType(
                        new MutablePrimitiveType().setPrimitiveKind(BarePrimitiveType.Kind.DOUBLE)
                    ),
                new MutableAnnotationTypeElement()
                    .setSimpleName("tag")
                    .setReturnType(
                        new MutableDeclaredType().setDeclaration(String.class.getName())
                    )
                    .setDefaultValue("foo")
            ));

        Assert.assertEquals(actual, expected);
        Assert.assertEquals(actual.toString(), expected.toString());
    }
}
