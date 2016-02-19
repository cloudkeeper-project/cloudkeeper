package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.type.BareNoType;
import xyz.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.runtime.type.RuntimeNoType;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;
import java.util.Objects;

final class NoTypeImpl extends TypeMirrorImpl implements RuntimeNoType, NoType {
    private final TypeKind typeKind;

    /**
     * Constructor for effectively immutable instances (already frozen at construction time).
     */
    NoTypeImpl(CloudKeeperTypeReflection types, TypeKind typeKind) {
        super(State.PRECOMPUTED, types);
        this.typeKind = Objects.requireNonNull(typeKind);
    }

    private enum BareNoTypeImpl implements BareNoType, Immutable {
        NONE;

        @Override
        @Nullable
        public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
            return visitor.visitNoType(this, parameter);
        }

        @Override
        @Nullable
        public Location getLocation() {
            return null;
        }
    }

    /**
     * Constructor. Instance needs to be explicitly frozen before use.
     *
     * <p>Since the {@link BareNoType} does not model different type kinds, this constructor is similar to other
     * copy constructors in this package, but it does not take an {@code original} argument. The kind of the newly
     * constructed object is always {@link TypeKind#NONE}.
     *
     * @param parentContext parent copy context
     */
    NoTypeImpl(CopyContext parentContext) throws LinkerException {
        super(BareNoTypeImpl.NONE, parentContext);
        typeKind = TypeKind.NONE;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return typeKind == ((NoTypeImpl) otherObject).typeKind;
    }

    @Override
    public int hashCode() {
        return typeKind.hashCode();
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitNoType(this, parameter);
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitNoType(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        return ImmutableList.of();
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
