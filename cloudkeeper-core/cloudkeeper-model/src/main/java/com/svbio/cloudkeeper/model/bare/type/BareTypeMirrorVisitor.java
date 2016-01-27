package com.svbio.cloudkeeper.model.bare.type;

import javax.annotation.Nullable;

/**
 * Visitor of types, in the style of the visitor design pattern.
 *
 * This visitor is similar to {@link javax.lang.model.type.TypeVisitor}.
 *
 * @see javax.lang.model.type.TypeVisitor
 */
public interface BareTypeMirrorVisitor<T, P> {
    /**
     * Visits an array type.
     */
    @Nullable
    T visitArrayType(BareArrayType arrayType, @Nullable P parameter);

    /**
     * Visits a declared type.
     */
    @Nullable
    T visitDeclaredType(BareDeclaredType declaredType, @Nullable P parameter);

    /**
     * Visits a type variable.
     */
    @Nullable
    T visitTypeVariable(BareTypeVariable typeVariable, @Nullable P parameter);

    /**
     * Visits a wildcard type argument.
     */
    @Nullable
    T visitWildcardType(BareWildcardType wildcardType, @Nullable P parameter);

    /**
     * Visits a primitive type.
     */
    @Nullable
    T visitPrimitive(BarePrimitiveType primitiveType, @Nullable P parameter);

    /**
     * Visits a no-type pseudo type.
     */
    @Nullable
    T visitNoType(BareNoType noType, @Nullable P parameter);

    /**
     * Visits a type which is none of the other types.
     */
    @Nullable
    T visitOther(BareTypeMirror type, @Nullable P parameter);
}
