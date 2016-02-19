package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.type.BareArrayType;
import xyz.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import xyz.cloudkeeper.model.runtime.type.RuntimeArrayType;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;
import java.util.Objects;

final class ArrayTypeImpl extends TypeMirrorImpl implements RuntimeArrayType, ArrayType {
    private final TypeMirrorImpl componentType;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    ArrayTypeImpl(CloudKeeperTypeReflection types, TypeMirrorImpl componentType) {
        super(State.PRECOMPUTED, types);
        this.componentType = Objects.requireNonNull(componentType);
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    ArrayTypeImpl(BareArrayType original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        CopyContext componentTypeContext = getCopyContext().newContextForProperty("componentType");
        componentType = TypeMirrorImpl.copyOf(original.getComponentType(), componentTypeContext);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return componentType.equals(((ArrayTypeImpl) otherObject).componentType);
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode() + componentType.hashCode();
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitArray(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitArrayType(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        return componentType.asTypeDeclaration();
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ARRAY;
    }

    @Override
    public TypeMirrorImpl getComponentType() {
        return componentType;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.add(componentType);
    }

    @Override
    void finishTypeMirror(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
