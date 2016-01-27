package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.type.BarePrimitiveType;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.runtime.type.RuntimePrimitiveType;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

final class PrimitiveTypeImpl extends TypeMirrorImpl implements RuntimePrimitiveType, PrimitiveType {
    private final Kind primitiveKind;
    private final TypeKind typeKind;

    private static final Map<Kind, TypeKind> KIND_MAP;

    static {
        EnumMap<Kind, TypeKind> map = new EnumMap<>(Kind.class);
        map.put(Kind.BOOLEAN, TypeKind.BOOLEAN);
        map.put(Kind.CHAR, TypeKind.CHAR);
        map.put(Kind.BYTE, TypeKind.BYTE);
        map.put(Kind.SHORT, TypeKind.SHORT);
        map.put(Kind.INT, TypeKind.INT);
        map.put(Kind.LONG, TypeKind.LONG);
        map.put(Kind.FLOAT, TypeKind.FLOAT);
        map.put(Kind.DOUBLE, TypeKind.DOUBLE);

        assert map.size() == Kind.values().length;
        KIND_MAP = Collections.unmodifiableMap(map);
    }

    PrimitiveTypeImpl(CloudKeeperTypeReflection types, Kind primitiveKind) {
        super(State.PRECOMPUTED, types);
        this.primitiveKind = primitiveKind;
        @Nullable TypeKind localTypeKind = KIND_MAP.get(primitiveKind);
        assert localTypeKind != null;
        typeKind = localTypeKind;
    }

    PrimitiveTypeImpl(BarePrimitiveType original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        CopyContext context = getCopyContext();
        primitiveKind
            = Preconditions.requireNonNull(original.getPrimitiveKind(), context.newContextForProperty("primitiveKind"));
        @Nullable TypeKind localTypeKind = KIND_MAP.get(primitiveKind);
        assert localTypeKind != null;
        typeKind = localTypeKind;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return primitiveKind == ((PrimitiveTypeImpl) otherObject).primitiveKind;
    }

    @Override
    public int hashCode() {
        return primitiveKind.hashCode();
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        return ImmutableList.of();
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitPrimitive(this, parameter);
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitPrimitive(this, parameter);
    }

    @Override
    public Kind getPrimitiveKind() {
        return primitiveKind;
    }

    @Override
    public TypeKind getKind() {
        return typeKind;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void finishTypeMirror(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
