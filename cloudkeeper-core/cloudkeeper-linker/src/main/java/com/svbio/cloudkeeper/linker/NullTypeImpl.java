package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;

final class NullTypeImpl extends TypeMirrorImpl implements NullType {
    NullTypeImpl(CloudKeeperTypeReflection types) {
        super(State.PRECOMPUTED, types);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        return super.equals(otherObject);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitOther(this, parameter);
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitNull(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        return ImmutableList.of();
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.NULL;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void finishTypeMirror(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
