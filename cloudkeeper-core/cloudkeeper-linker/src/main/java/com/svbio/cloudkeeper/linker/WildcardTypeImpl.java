package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;
import com.svbio.cloudkeeper.model.bare.type.BareDeclaredType;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.bare.type.BareWildcardType;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeWildcardType;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

final class WildcardTypeImpl extends TypeMirrorImpl implements RuntimeWildcardType, WildcardType {
    @Nullable private final TypeMirrorImpl extendsBound;
    @Nullable private final TypeMirrorImpl superBound;

    @Nullable private ImmutableList<TypeDeclarationImpl> typeDeclarations;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    WildcardTypeImpl(CloudKeeperTypeReflection types, @Nullable TypeMirrorImpl extendsBound,
            @Nullable TypeMirrorImpl superBound) {
        super(State.PRECOMPUTED, types);

        // Since PortType instances are immutable, we can just copy the reference. Note that the bounds may be null,
        // as specified by javax.lang.model.type.WildcardType
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    WildcardTypeImpl(BareWildcardType original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        @Nullable BareTypeMirror originalExtendsBound = original.getExtendsBound();
        // Normalize extends bound if necessary. If Object is given as extends bound, replace by null.
        if (originalExtendsBound instanceof BareDeclaredType) {
            @Nullable BareQualifiedNameable typeDeclaration
                = ((BareDeclaredType) originalExtendsBound).getDeclaration();
            if (typeDeclaration != null) {
                @Nullable Name name = typeDeclaration.getQualifiedName();
                if (name != null && Object.class.getName().equals(name.toString())) {
                    originalExtendsBound = null;
                }
            }
        }
        extendsBound = TypeMirrorImpl.optionalCopyOf(
            originalExtendsBound, context.newContextForProperty("extendsBound"));
        superBound = TypeMirrorImpl.optionalCopyOf(
            original.getSuperBound(), context.newContextForProperty("superBound"));
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        WildcardTypeImpl other = (WildcardTypeImpl) otherObject;
        return Objects.equals(extendsBound, other.extendsBound)
            && Objects.equals(superBound, other.superBound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extendsBound, superBound);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitWildcard(this, parameter);
    }

    @Override
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitWildcardType(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        require(State.FINISHED);
        // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
        @Nullable ImmutableList<TypeDeclarationImpl> localTypeDeclarations = typeDeclarations;
        if (localTypeDeclarations == null) {
            LinkedHashSet<TypeDeclarationImpl> set = new LinkedHashSet<>();
            if (extendsBound != null) {
                set.addAll(extendsBound.asTypeDeclaration());
            }
            if (superBound != null) {
                set.addAll(superBound.asTypeDeclaration());
            }
            localTypeDeclarations = ImmutableList.copyOf(set);
            typeDeclarations = localTypeDeclarations;
        }
        return localTypeDeclarations;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.WILDCARD;
    }

    @Override
    @Nullable
    public TypeMirrorImpl getExtendsBound() {
        return extendsBound;
    }

    @Override
    @Nullable
    public TypeMirrorImpl getSuperBound() {
        return superBound;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        if (extendsBound != null) {
            freezables.add(extendsBound);
        }
        if (superBound != null) {
            freezables.add(superBound);
        }
    }

    @Override
    void finishTypeMirror(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) {
        asTypeDeclaration();
    }
}
