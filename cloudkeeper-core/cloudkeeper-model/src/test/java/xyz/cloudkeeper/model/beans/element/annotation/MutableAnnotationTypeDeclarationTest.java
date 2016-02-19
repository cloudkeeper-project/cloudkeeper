package xyz.cloudkeeper.model.beans.element.annotation;

import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import xyz.cloudkeeper.model.bare.type.BarePrimitiveType;
import xyz.cloudkeeper.model.beans.AnnotationCopyOption;
import xyz.cloudkeeper.model.beans.MutableLocatableContract;
import xyz.cloudkeeper.model.beans.XmlRootElementContract;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.beans.type.MutableDeclaredType;
import xyz.cloudkeeper.model.beans.type.MutablePrimitiveType;
import xyz.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collections;

public class MutableAnnotationTypeDeclarationTest {
    @Factory
    public Object[] contracts() {
        return new Object[] {
            new MutableLocatableContract(MutableAnnotationTypeDeclaration.class),
            new XmlRootElementContract(MutableAnnotationTypeDeclaration.fromClass(MemoryRequirements.class))
        };
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface FooAnnotation {
        byte[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface BarAnnotation {
        String name() default "xyz";
    }

    @FooAnnotation({ 2, 3 })
    @BarAnnotation
    public @interface MemoryRequirements {
        int value() default 1024;
        String unit() default "MB";
    }

    private enum CopyOptions implements AnnotationCopyOption {
        INSTANCE;

        @Override
        public boolean isCloudKeeperAnnotation(Annotation annotation, AnnotatedElement annotatedElement) {
            return annotation.annotationType().getPackage().equals(getClass().getPackage());
        }
    }

    @Test
    public void fromClass() {
        MutableAnnotationTypeDeclaration actual
            = MutableAnnotationTypeDeclaration.fromClass(MemoryRequirements.class, CopyOptions.INSTANCE);
        Collections.sort(actual.getDeclaredAnnotations(), (first, second) -> {
            @Nullable MutableQualifiedNamable firstDeclaration = first.getDeclaration();
            @Nullable MutableQualifiedNamable secondDeclaration = second.getDeclaration();
            assert firstDeclaration != null && secondDeclaration != null;
            @Nullable Name firstName = firstDeclaration.getQualifiedName();
            @Nullable Name secondName = secondDeclaration.getQualifiedName();
            assert firstName != null && secondName != null;
            return firstName.compareTo(secondName);
        });
        Collections.sort(actual.getElements(), (first, second) -> {
            @Nullable Name firstName = first.getSimpleName();
            @Nullable Name secondName = second.getSimpleName();
            assert firstName != null && secondName != null;
            return firstName.compareTo(secondName);
        });
        MutableAnnotationTypeDeclaration expected = new MutableAnnotationTypeDeclaration()
            .setSimpleName(MemoryRequirements.class.getSimpleName())
            .setDeclaredAnnotations(Arrays.asList(
                new MutableAnnotation()
                    .setDeclaration(BarAnnotation.class.getCanonicalName()),
                new MutableAnnotation()
                    .setDeclaration(FooAnnotation.class.getCanonicalName())
                    .setEntries(Collections.singletonList(
                        new MutableAnnotationEntry()
                            .setKey("value")
                            .setValue(new byte[]{(byte) 2, (byte) 3})
                    ))
            ))
            .setElements(Arrays.asList(
                new MutableAnnotationTypeElement()
                    .setSimpleName("unit")
                    .setReturnType(new MutableDeclaredType().setDeclaration(String.class.getName()))
                    .setDefaultValue("MB"),
                new MutableAnnotationTypeElement()
                    .setSimpleName("value")
                    .setReturnType(new MutablePrimitiveType().setPrimitiveKind(BarePrimitiveType.Kind.INT))
                    .setDefaultValue(1024)
            ));
        Assert.assertEquals(actual, expected);
    }
}
