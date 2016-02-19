package xyz.cloudkeeper.model.bare.element.annotation;

import xyz.cloudkeeper.model.bare.element.BareElement;
import xyz.cloudkeeper.model.bare.element.BareSimpleNameable;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;

import javax.annotation.Nullable;

/**
 * Annotation type element of a CloudKeeper annotation declaration.
 *
 * CloudKeeper annotation declarations correspond to Java Annotation Types as specified by §9.6 of the Java Language
 * Specification (JLS). Correspondingly, this interface models Annotation Type Elements (§9.6.1).
 *
 * This interface is conceptually similar to {@link javax.lang.model.element.ExecutableElement}.
 */
public interface BareAnnotationTypeElement extends BareSimpleNameable, BareElement {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "annotation type element";

    /**
     * Default simple name of annotation type elements.
     */
    String DEFAULT_SIMPLE_NAME = "value";

    /**
     * Returns the type of the annotation type element.
     *
     * <p>Java restrictions on the allowed types are documented in JLS §9.6.1. CloudKeeper imposes additional
     * constraints that the type may only be:
     * <ul><li>
     *     a primitive type
     * </li><li>
     *     {@link String}
     * </li><li>
     *     an array type whose component type is one of the preceding types.
     * </li></ul>
     * The precludes nested arrays.
     *
     * <p>If this method may return {@code null} if the type can be inferred from the default value.
     *
     * @see javax.lang.model.element.ExecutableElement#getReturnType()
     * @see java.lang.reflect.Method#getReturnType()
     */
    @Nullable
    BareTypeMirror getReturnType();

    /**
     * Returns the default value of this annotation type element.
     *
     * @see javax.lang.model.element.ExecutableElement#getDefaultValue()
     * @see java.lang.reflect.Method#getDefaultValue()
     */
    @Nullable
    BareAnnotationValue getDefaultValue();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareAnnotationTypeElement#toString()}.
         */
        public static String toHumanReadableString(BareAnnotationTypeElement instance) {
            return String.format("%s '%s'", NAME, instance.getSimpleName());
        }
    }
}
