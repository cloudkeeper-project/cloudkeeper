package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.bare.type.BareDeclaredType;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeDeclaredType;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

final class DeclaredTypeImpl extends TypeMirrorImpl implements RuntimeDeclaredType, DeclaredType {
    private final TypeMirrorImpl enclosingType;
    @Nullable private final NameReference typeDeclarationReference;
    private final ImmutableList<TypeMirrorImpl> typeArguments;

    private TypeDeclarationImpl declaration;
    @Nullable private ImmutableList<TypeDeclarationImpl> typeDeclarations;

    DeclaredTypeImpl(CloudKeeperTypeReflection types, TypeMirrorImpl enclosingType,
        TypeDeclarationImpl rawType, List<TypeMirrorImpl> typeArguments) {
        super(State.PRECOMPUTED, types);

        Objects.requireNonNull(enclosingType);
        Objects.requireNonNull(rawType);
        Objects.requireNonNull(typeArguments);

        typeDeclarationReference = null;
        this.enclosingType = enclosingType;
        declaration = rawType;
        this.typeArguments = ImmutableList.copyOf(typeArguments);
    }

    DeclaredTypeImpl(BareDeclaredType original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        typeDeclarationReference
            = new NameReference(original.getDeclaration(), context.newContextForProperty("declaration"));

        @Nullable BareTypeMirror originalEnclosingType = original.getEnclosingType();
        CopyContext enclosingTypeContext = context.newContextForProperty("enclosingType");
        enclosingType = originalEnclosingType == null
            ? new NoTypeImpl(enclosingTypeContext)
            : TypeMirrorImpl.copyOf(originalEnclosingType, enclosingTypeContext);

        typeArguments = immutableListOf(original.getTypeArguments(), "typeArguments", TypeMirrorImpl::copyOf);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        require(State.FINISHED);
        DeclaredTypeImpl other = (DeclaredTypeImpl) otherObject;
        return declaration.equals(other.declaration)
            && typeArguments.equals(other.typeArguments);
    }

    @Override
    public int hashCode() {
        require(State.FINISHED);
        return Objects.hash(declaration, typeArguments);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitDeclaredType(this, parameter);
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitDeclared(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        require(State.FINISHED);

        // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
        @Nullable ImmutableList<TypeDeclarationImpl> localTypeDeclarations = typeDeclarations;
        if (localTypeDeclarations == null) {
            LinkedHashSet<TypeDeclarationImpl> set = new LinkedHashSet<>();
            set.add(declaration);
            for (TypeMirrorImpl typeArgument: typeArguments) {
                set.addAll(typeArgument.asTypeDeclaration());
            }
            localTypeDeclarations = ImmutableList.copyOf(set);
            typeDeclarations = localTypeDeclarations;
        }
        return localTypeDeclarations;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.DECLARED;
    }

    @Override
    public TypeDeclarationImpl getDeclaration() {
        require(State.FINISHED);
        return declaration;
    }

    @Override
    public TypeDeclarationImpl asElement() {
        require(State.FINISHED);
        return declaration;
    }

    @Override
    public ImmutableList<TypeMirrorImpl> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public TypeMirrorImpl getEnclosingType() {
        return enclosingType;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        assert typeDeclarationReference != null;
        freezables.add(typeDeclarationReference);
        freezables.add(enclosingType);
        freezables.addAll(typeArguments);
    }

    @Override
    void finishTypeMirror(FinishContext context) throws LinkerException {
        assert typeDeclarationReference != null;
        declaration
            = context.getDeclaration(BareTypeDeclaration.NAME, TypeDeclarationImpl.class, typeDeclarationReference);
    }

    @Override
    void verifyFreezable(VerifyContext context) {
        asTypeDeclaration();
    }
}
