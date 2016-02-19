package xyz.cloudkeeper.dsl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Super Type Token.
 *
 * @param <T> type parameter
 */
public abstract class TypeToken<T> {
    private final Type javaType;

    protected TypeToken() {
        if (!getClass().getSuperclass().equals(TypeToken.class)) {
            throw new IllegalStateException(String.format("Only direct subclasses of %s allowed.", TypeToken.class));
        } else if (!(getClass().getGenericSuperclass() instanceof ParameterizedType)) {
            throw new IllegalStateException(String.format("Invalid use of %s without type parameter.",
                TypeToken.class));
        }

        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        Type[] actualTypeArguments = genericSuperclass.getActualTypeArguments();
        if (actualTypeArguments.length != 1) {
            throw new IllegalStateException(String.format("%s requires exactly one type parameter.", TypeToken.class));
        }

        this.javaType = actualTypeArguments[0];
    }

    private TypeToken(Type javaType) {
        this.javaType = javaType;
    }

    public final Type getJavaType() {
        return javaType;
    }

    private static final class TypeTokenImpl<U> extends TypeToken<U> {
        TypeTokenImpl(Type javaType) {
            super(javaType);
        }
    }

    public static <T> TypeToken<T> of(Class<T> clazz) {
        return new TypeTokenImpl<>(clazz);
    }

    public static TypeToken<?> of(Type type) {
        return new TypeTokenImpl<Object>(type);
    }
}
