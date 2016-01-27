package com.svbio.cloudkeeper.linker.examples;

import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotation;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationEntry;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeDeclaration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

@Retention(RetentionPolicy.RUNTIME)
public @interface Memory {
    int value();
    String unit();

    final class Beans {
        private Beans() { }

        public static MutableAnnotationTypeDeclaration declaration() {
            return MutableAnnotationTypeDeclaration.fromClass(Memory.class);
        }

        public static MutableAnnotation createAnnotation(int value, String unit) {
            return new MutableAnnotation()
                .setDeclaration(Memory.class.getName())
                .setEntries(Arrays.asList(
                    new MutableAnnotationEntry()
                        .setKey("value")
                        .setValue(value),
                    new MutableAnnotationEntry()
                        .setKey("unit")
                        .setValue(unit)
                ));
        }
    }
}
