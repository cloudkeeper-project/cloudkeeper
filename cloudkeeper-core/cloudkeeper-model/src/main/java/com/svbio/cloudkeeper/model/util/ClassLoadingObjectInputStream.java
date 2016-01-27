package com.svbio.cloudkeeper.model.util;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Objects;

/**
 * Subclass of {@link ObjectInputStream} that uses a custom class loader for {@link #resolveClass(ObjectStreamClass)}.
 */
public final class ClassLoadingObjectInputStream extends ObjectInputStream {
    private final ClassLoader classLoader;

    private static final HashMap<String, Class<?>> PRIMITIVE_CLASSES = new HashMap<>(8, 1.0F);
    static {
        PRIMITIVE_CLASSES.put(boolean.class.getName(), boolean.class);
        PRIMITIVE_CLASSES.put(byte.class.getName(), byte.class);
        PRIMITIVE_CLASSES.put(char.class.getName(), char.class);
        PRIMITIVE_CLASSES.put(short.class.getName(), short.class);
        PRIMITIVE_CLASSES.put(int.class.getName(), int.class);
        PRIMITIVE_CLASSES.put(long.class.getName(), long.class);
        PRIMITIVE_CLASSES.put(float.class.getName(), float.class);
        PRIMITIVE_CLASSES.put(double.class.getName(), double.class);
        PRIMITIVE_CLASSES.put(void.class.getName(), void.class);
    }

    public ClassLoadingObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
        super(Objects.requireNonNull(in));
        this.classLoader = Objects.requireNonNull(classLoader);
    }

    /**
     * Returns the class loader that this instance was created with.
     *
     * @return the class loader
     * @see #ClassLoadingObjectInputStream(InputStream, ClassLoader)
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the class equivalent to the specified stream class description, using this instance's {@link ClassLoader}
     * to load the class.
     *
     * <p>This method obeys the contracts defined by {@code ObjectInputStream}. Specifically, this method returns the
     * result of calling {@code Class.forName(descriptor.getName(), false, getClassLoader())}. If this call results in a
     * {@link ClassNotFoundException} and the name of the passed {@link ObjectStreamClass} instance is the Java language
     * keyword for a primitive type or void, then the {@link Class} object representing that primitive type or void will
     * be returned (e.g., an {@link ObjectStreamClass} with the name {@code "int"} will be resolved to
     * {@link Integer#TYPE}). Otherwise, the {@link ClassNotFoundException} will be thrown to the caller of this method.
     *
     * @param descriptor descriptor for a serialized class
     * @return the {@link Class} object corresponding to {@code descriptor}
     * @throws IOException any of the usual Input/Output exceptions.
     * @throws ClassNotFoundException if class of a serialized object cannot be found
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass descriptor) throws IOException, ClassNotFoundException {
        String name = descriptor.getName();
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException exception) {
            @Nullable Class<?> primitiveClass = PRIMITIVE_CLASSES.get(name);
            if (primitiveClass != null) {
                return primitiveClass;
            } else {
                throw exception;
            }
        }
    }
}
