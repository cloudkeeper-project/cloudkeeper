package xyz.cloudkeeper.dsl;

import org.testng.Assert;
import org.testng.annotations.Test;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.bare.type.BarePrimitiveType;
import xyz.cloudkeeper.model.beans.StandardCopyOption;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeDeclaration;
import xyz.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeElement;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;
import xyz.cloudkeeper.model.beans.type.MutablePrimitiveType;

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
