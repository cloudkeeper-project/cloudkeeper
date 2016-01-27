package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import net.florianschoppmann.java.type.IntersectionType;

import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

final class IntersectionTypeImpl extends TypeMirrorImpl implements IntersectionType {
    private final ImmutableList<TypeMirrorImpl> bounds;
    @Nullable private ImmutableList<TypeDeclarationImpl> typeDeclarations;

    /**
     * Constructor.
     *
     * This class does not have a correspondence in {@link com.svbio.cloudkeeper.model.bare.type}. Therefore, it is
     * assembled from valid {@link TypeMirrorImpl} instances, which have been
     * frozen elsewhere. Instances constructed using this constructor therefore do not have to be frozen.
     */
    IntersectionTypeImpl(CloudKeeperTypeReflection types, List<TypeMirrorImpl> bounds) {
        super(State.PRECOMPUTED, types);
        this.bounds = ImmutableList.copyOf(bounds);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return bounds.equals(((IntersectionTypeImpl) otherObject).bounds);
    }

    @Override
    public int hashCode() {
        return bounds.hashCode();
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitUnknown(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitOther(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
        @Nullable ImmutableList<TypeDeclarationImpl> localTypeDeclarations = typeDeclarations;
        if (localTypeDeclarations == null) {
            LinkedHashSet<TypeDeclarationImpl> set = new LinkedHashSet<>();
            for (TypeMirrorImpl bound: bounds) {
                set.addAll(bound.asTypeDeclaration());
            }
            localTypeDeclarations = ImmutableList.copyOf(set);
            typeDeclarations = localTypeDeclarations;
        }
        return localTypeDeclarations;
    }

    @Override
    public boolean isIntersectionType() {
        return true;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.OTHER;
    }

    @Override
    public ImmutableList<TypeMirrorImpl> getBounds() {
        return bounds;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void finishTypeMirror(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) {
        asTypeDeclaration();
    }
}
