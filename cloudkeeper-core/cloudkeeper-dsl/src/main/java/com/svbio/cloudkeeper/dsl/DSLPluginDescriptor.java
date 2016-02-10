package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.InvalidClassException;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Descriptor of a DSL plug-in declaration.
 *
 * <p>An instance of this class provides the {@link Class} instances (as specified by the DSL) that make up a
 * CloudKeeper plug-in declaration.
 */
public final class DSLPluginDescriptor {
    /**
     * Prefix of mix-in classes that provide annotations.
     */
    public static final String MIXIN_PREFIX = "cloudkeeper.mixins.";

    private static final List<Class<? extends Annotation>> PLUGIN_ANNOTATIONS = Collections.unmodifiableList(
        Arrays.asList(AnnotationTypePlugin.class, CompositeModulePlugin.class, SerializationPlugin.class,
            SimpleModulePlugin.class, TypePlugin.class)
    );

    private final Class<?> pluginClass;
    private final Class<?> classWithAnnotation;
    private final Class<? extends Annotation> annotationType;

    private DSLPluginDescriptor(Class<?> pluginClass, Class<?> classWithAnnotation,
            Class<? extends Annotation> annotationType) {
        this.pluginClass = pluginClass;
        this.classWithAnnotation = classWithAnnotation;
        this.annotationType = annotationType;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        DSLPluginDescriptor other = (DSLPluginDescriptor) otherObject;
        return pluginClass.equals(other.pluginClass)
            && classWithAnnotation.equals(other.classWithAnnotation)
            && annotationType.equals(other.annotationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginClass, classWithAnnotation, annotationType);
    }

    /**
     * Returns the CloudKeeper plugin annotation present on the given class.
     *
     * @param clazz class instance that represents a plug-in declaration (this can be a plug-in class or a mixin class)
     * @return CloudKeeper plugin annotation present on the given class
     * @throws InvalidClassException if multiple plug-in annotations are present
     */
    @Nullable
    private static Class<? extends Annotation> getAnnotationType(Class<?> clazz) {
        @Nullable Class<? extends Annotation> annotationType = null;
        for (Class<? extends Annotation> pluginAnnotation: PLUGIN_ANNOTATIONS) {
            if (clazz.isAnnotationPresent(pluginAnnotation)) {
                if (annotationType != null) {
                    throw new InvalidClassException(String.format(
                        "Multiple plug-in type annotations (at least @%s and @%s) on %s.",
                        annotationType.getSimpleName(), pluginAnnotation.getSimpleName(), clazz
                    ));
                }
                annotationType = pluginAnnotation;
            }
        }
        return annotationType;
    }

    /**
     * Returns the descriptor of the DSL plug-in declaration identified by the given {@link Class} instance.
     *
     * <p>The given {@link Class instance} {@code clazz} may either be the class that has the same name as the
     * CloudKeeper plug-in declaration (and which would be returned by {@link #getPluginClass()}), or it may be the
     * class that has the annotations for the CloudKeeper plug-in declaration (and which would be returned by
     * {@link #getClassWithAnnotation()}).
     *
     * Specifically, this method determines properties of the descriptor as follows:
     * <ul><li>
     *     {@link #getPluginClass()}: If the name of {@code clazz} starts with {@link #MIXIN_PREFIX}, that prefix is
     *     stripped from the name and the class with the resulting name is used. Otherwise, just {@code clazz} is used.
     * </li><li>
     *     {@link #getClassWithAnnotation()}: If the name of {@code clazz} starts with {@link #MIXIN_PREFIX}, or if
     *     {@code clazz} has a CloudKeeper plugin annotation (such as {@link AnnotationTypePlugin},
     *     {@link CompositeModulePlugin}, etc.), then {@code clazz} is used. Otherwise, {@link #MIXIN_PREFIX} is
     *     prepended to the name of {@code clazz}, and the class with the resulting name is used.
     * </li><li>
     *     {@link #getAnnotationType()}: The CloudKeeper plugin annotation present on {@link #getClassWithAnnotation()}.
     * </li></ul>
     *
     * <p>If this method succeeds, it is guaranteed that at most CloudKeeper plugin annotation is present on the class
     * that {@link #getClassWithAnnotation()} returns. Correspondingly, {@link #getAnnotationType()} is well-defined.
     * Otherwise, an {@link InvalidClassException} exception is thrown.
     *
     * @param clazz class instance that represents a plug-in declaration (this can be a plug-in class or a mixin class)
     * @param classLoader class loader for loading classes with generated names (see description)
     * @return class that has the annotations for the plug-in declaration represented by {@code clazz}
     * @throws InvalidClassException if a class as specified above could not be found or loaded
     */
    public static DSLPluginDescriptor forClass(Class<?> clazz, ClassLoader classLoader) {
        String className = clazz.getName();
        Class<?> pluginClass;
        Class<?> classWithAnnotation;
        @Nullable Class<? extends Annotation> annotationType = getAnnotationType(clazz);
        if (className.startsWith(MIXIN_PREFIX)) {
            try {
                pluginClass = Class.forName(className.substring(MIXIN_PREFIX.length()), true, classLoader);
            } catch (ClassNotFoundException exception) {
                throw new InvalidClassException(String.format(
                    "Could not load plugin class corresponding to mixin %s.", clazz
                ), exception);
            }
            classWithAnnotation = clazz;
        } else {
            pluginClass = clazz;
            if (annotationType != null) {
                classWithAnnotation = clazz;
            } else {
                try {
                    classWithAnnotation = Class.forName(MIXIN_PREFIX + clazz.getName(), true, classLoader);
                } catch (ClassNotFoundException exception) {
                    throw new InvalidClassException(String.format(
                        "Could not load mixin class corresponding to plugin %s.", clazz
                    ), exception);
                }

                annotationType = getAnnotationType(classWithAnnotation);
            }
        }

        if (annotationType == null) {
            throw new InvalidClassException(String.format("No plug-in type annotation on %s.", classWithAnnotation));
        }

        if (!pluginClass.equals(classWithAnnotation)
                && !(TypePlugin.class.equals(annotationType) || SerializationPlugin.class.equals(annotationType))) {
            throw new InvalidClassException(String.format(
                "Mixin annotations not supported for @%s declarations.", annotationType.getSimpleName()));
        }

        return new DSLPluginDescriptor(pluginClass, classWithAnnotation, annotationType);
    }

    /**
     * Returns the Java {@link Class} instance that has the same name as the CloudKeeper plug-in declaration.
     */
    public Class<?> getPluginClass() {
        return pluginClass;
    }

    /**
     * Returns the Java {@link Class} instance that has the annotations for the CloudKeeper plug-in declaration.
     *
     * <p>The returned {@link Class} instance may be the same as {@link #getPluginClass()}, or it may be a class
     * whose name is the concatenation of {@link #MIXIN_PREFIX} and the class name of {@link #getPluginClass()}.
     */
    public Class<?> getClassWithAnnotation() {
        return classWithAnnotation;
    }

    /**
     * Returns the annotation type (such as {@link AnnotationTypePlugin}, {@link CompositeModulePlugin}, etc.)
     * present on {@link #getClassWithAnnotation()}.
     */
    public Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }
}
