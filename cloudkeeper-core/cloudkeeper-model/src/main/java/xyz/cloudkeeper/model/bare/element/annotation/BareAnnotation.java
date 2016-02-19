package xyz.cloudkeeper.model.bare.element.annotation;

import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.bare.element.BareQualifiedNameable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Annotation for a language construct.
 *
 * <p>CloudKeeper annotations are equivalent to Java Annotations as specified by §9.7 of the Java Language Specification
 * (JLS).
 *
 * <p>This interface models a bare Java annotation and can be seen as a bare correspondence of
 * {@link javax.lang.model.element.AnnotationMirror}, which itself mirrors the core reflection interface
 * {@link java.lang.annotation.Annotation}.
 *
 * @see javax.lang.model.element.AnnotationMirror
 * @see java.lang.annotation.Annotation
 */
public interface BareAnnotation extends BareLocatable {
    /**
     * Returns the annotation type declaration.
     *
     * <p>Note that this method is similar to but yet different from
     * {@link javax.lang.model.element.AnnotationMirror#getAnnotationType()}, which returns a type and not an element.
     *
     * @see javax.lang.model.element.AnnotationMirror#getAnnotationType()
     * @see java.lang.annotation.Annotation#annotationType()
     */
    @Nullable
    BareQualifiedNameable getDeclaration();

    /**
     * Returns the list of directly present annotation entries.
     *
     * @see javax.lang.model.element.AnnotationMirror#getElementValues()
     * @see Class#getMethods()
     */
    List<? extends BareAnnotationEntry> getEntries();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareAnnotation#toString()}.
         */
        public static String toString(BareAnnotation instance) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('@');
            @Nullable BareQualifiedNameable declaration = instance.getDeclaration();
            stringBuilder.append(declaration == null
                ? "(null)"
                : declaration.getQualifiedName()
            );
            boolean first = true;
            List<? extends BareAnnotationEntry> entries = instance.getEntries();
            for (BareAnnotationEntry element: entries) {
                if (first) {
                    stringBuilder.append('(');
                    first = false;
                } else {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(element);
            }
            if (!first) {
                stringBuilder.append(')');
            }
            return stringBuilder.toString();
        }
    }
}
