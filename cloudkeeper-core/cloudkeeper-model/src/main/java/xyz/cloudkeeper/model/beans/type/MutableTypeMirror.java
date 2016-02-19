package xyz.cloudkeeper.model.beans.type;

import xyz.cloudkeeper.model.bare.type.BareArrayType;
import xyz.cloudkeeper.model.bare.type.BareDeclaredType;
import xyz.cloudkeeper.model.bare.type.BareNoType;
import xyz.cloudkeeper.model.bare.type.BarePrimitiveType;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;
import xyz.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import xyz.cloudkeeper.model.bare.type.BareTypeVariable;
import xyz.cloudkeeper.model.bare.type.BareWildcardType;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.MutableLocatable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Objects;

@XmlSeeAlso({
    MutableArrayType.class, MutableDeclaredType.class, MutablePrimitiveType.class, MutableTypeVariable.class,
    MutableWildcardType.class
})
public abstract class MutableTypeMirror<D extends MutableTypeMirror<D>>
        extends MutableLocatable<D>
        implements BareTypeMirror {
    private static final long serialVersionUID = 2365605986635985608L;

    MutableTypeMirror() { }

    MutableTypeMirror(BareTypeMirror original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    private enum CopyVisitor implements BareTypeMirrorVisitor<MutableTypeMirror<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableTypeMirror<?> visitArrayType(BareArrayType arrayType, @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableArrayType.copyOfArrayType(arrayType, copyOptions);
        }

        @Override
        public MutableTypeMirror<?> visitDeclaredType(BareDeclaredType declaredType,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableDeclaredType.copyOfDeclaredType(declaredType, copyOptions);
        }

        @Override
        public MutableTypeMirror<?> visitPrimitive(BarePrimitiveType primitiveType,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutablePrimitiveType.copyOfPrimitiveType(primitiveType, copyOptions);
        }

        @Override
        public MutableTypeMirror<?> visitTypeVariable(BareTypeVariable typeVariable,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableTypeVariable.copyOfTypeVariable(typeVariable, copyOptions);
        }

        @Override
        public MutableTypeMirror<?> visitWildcardType(BareWildcardType wildcardType,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableWildcardType.copyOfWildcardType(wildcardType, copyOptions);
        }

        @Override
        public MutableTypeMirror<?> visitNoType(BareNoType noType, @Nullable CopyOption[] copyOptions) {
            // In this package, "no type" is represented by null, and not an instance of a no-type pseudo type
            return null;
        }

        @Override
        public MutableTypeMirror<?> visitOther(BareTypeMirror type, @Nullable CopyOption[] copyOptions) {
            return null;
        }
    }

    @Nullable
    public static MutableTypeMirror<?> copyOfTypeMirror(@Nullable BareTypeMirror bareType, CopyOption... copyOptions) {
        return bareType == null
            ? null
            : bareType.accept(CopyVisitor.INSTANCE, copyOptions);
    }

    /**
     * Creates a port type from a Java type and returns the CloudKeeper port-type representation.
     *
     * @param original Java type
     * @return the CloudKeeper port-type representation of the Java type
     * @throws NullPointerException if {@code original} or any of its elements is null
     * @throws IllegalArgumentException if {@code original} contains an element that is not an instance of either
     *     {@link Class}, {@link TypeVariable}, {@link ParameterizedType}, or {@link WildcardType}
     */
    public static MutableTypeMirror<?> fromJavaType(Type original, CopyOption... copyOptions) {
        Objects.requireNonNull(original);

        if (original instanceof Class<?>) {
            Class<?> clazz = (Class<?>) original;
            if (clazz.isPrimitive()) {
                return MutablePrimitiveType.fromClass(clazz, copyOptions);
            } else if (clazz.isArray()) {
                return new MutableArrayType()
                    .setComponentType(fromJavaType(clazz.getComponentType()));
            }
            return MutableDeclaredType.fromType(original, copyOptions);
        } else if (original instanceof TypeVariable<?>) {
            return MutableTypeVariable.fromTypeVariable((TypeVariable<?>) original, copyOptions);
        } else if (original instanceof ParameterizedType) {
            return MutableDeclaredType.fromType(original, copyOptions);
        } else if (original instanceof WildcardType) {
            return MutableWildcardType.fromWildcardType((WildcardType) original, copyOptions);
        } else if (original instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) original;
            return new MutableArrayType()
                .setComponentType(fromJavaType(genericArrayType.getGenericComponentType(), copyOptions));
        } else {
            throw new IllegalArgumentException(String.format(
                "Expected one of %s, but got %s.",
                Arrays.asList(Class.class, GenericArrayType.class, ParameterizedType.class, TypeVariable.class,
                    WildcardType.class),
                original.getClass()
            ), null);
        }
    }
}
