package com.svbio.cloudkeeper.model.beans;

/**
 * Functional option for controlling whether a nested class should also be included in the CloudKeeper model.
 *
 * <p>An instance of this interface may be passed to {@code from...} static factory methods that construct
 * {@link com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration} instances from Java core reflection
 * objects.
 *
 * <p>By default (without this option), nested classes will only be translated into plug-in declarations if they are
 * public.
 */
public interface IncludeNestedCopyOption extends CopyOption {
    /**
     * Returns whether the given class should be included in the CloudKeeper model.
     *
     * @param clazz class object, {@link Class#getEnclosingClass()} is guaranteed to be non-null
     * @return whether the given nested class should be included in the CloudKeeper model
     */
    boolean shouldInclude(Class<?> clazz);
}
