package com.svbio.cloudkeeper.model.beans;

/**
 * Functional option for controlling whether a nested class should also be treated as a nested class in the CloudKeeper
 * model.
 *
 * <p>An instance of this interface may be passed to {@code from...} static factory methods that construct
 * {@link MutableLocatable} instances from Java core reflection objects.
 *
 * <p>By default (without this option), plug-in declarations are always considered nested if
 * {@link Class#getEnclosingClass()} is non-null.
 */
public interface NestedNameCopyOption extends CopyOption {
    /**
     * Returns whether the given class should be treated as a nested class in the CloudKeeper model.
     *
     * <p>If this method returns {@code false}, the given class will be treated as top-level class with simple name
     * {@code clazz.getName().substring(clazz.getPackage().getName().length() + 1)}. That is, the binary name, without
     * the package name, will be used as simple name in the CloudKeeper model. If this method returns {@code true},
     * {@code clazz.getSimpleName()} will be used as simple name in the CloudKeeper model.
     *
     * @param clazz class object, both this class as well as its enclosing class (returned by
     *     {@link Class#getEnclosingClass()}) are guaranteed non-null
     * @return whether the given class should be treated as a nested class in the CloudKeeper model
     */
    boolean isNested(Class<?> clazz);
}
