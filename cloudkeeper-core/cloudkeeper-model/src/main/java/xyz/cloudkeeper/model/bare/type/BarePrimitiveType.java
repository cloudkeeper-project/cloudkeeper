package xyz.cloudkeeper.model.bare.type;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Primitive type.
 *
 * <p>This interface corresponds to {@link javax.lang.model.type.PrimitiveType}, and both interfaces can be implemented
 * with covariant return types.
 *
 * <p>It is a requirement that {@link #toString()} returns a string representation according to the contract of this
 * interface (since this is a primitive interface, no default implementation is provided).
 */
public interface BarePrimitiveType extends BareTypeMirror {
    /**
     * Concrete primitive type.
     */
    enum Kind {
        BOOLEAN(boolean.class),
        CHAR(char.class),
        BYTE(byte.class),
        SHORT(short.class),
        INT(int.class),
        LONG(long.class),
        FLOAT(float.class),
        DOUBLE(double.class);

        private final Class<?> primitiveClass;

        Kind(Class<?> primitiveClass) {
            this.primitiveClass = primitiveClass;
        }

        public Class<?> getPrimitiveClass() {
            return primitiveClass;
        }

        private static final Map<Class<?>, Kind> JAVA_CLASS_MAP;
        static {
            Map<Class<?>, Kind> newMap = new HashMap<>();
            for (Kind primitiveType: Kind.values()) {
                newMap.put(primitiveType.primitiveClass, primitiveType);
            }
            JAVA_CLASS_MAP = Collections.unmodifiableMap(newMap);
        }
    }

    /**
     * Returns the concrete primitive type represented by this object.
     */
    @Nullable
    Kind getPrimitiveKind();

    /**
     * Returns a string representation of this element reference.
     *
     * <p>The returned string has to be {@code boolean}, {@code char}, {@code byte}, {@code short}, {@code int},
     * {@code long}, {@code float}, or {@code double.}
     */
    @Override
    String toString();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareDeclaredType#toString()}.
         */
        public static String toString(BarePrimitiveType instance) {
            @Nullable Kind kind = instance.getPrimitiveKind();
            return kind != null
                ? kind.getPrimitiveClass().getName()
                : "(null)";
        }
    }
}
