package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import xyz.cloudkeeper.model.bare.type.BareTypeVariable;
import xyz.cloudkeeper.model.runtime.type.RuntimeTypeVariable;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

final class TypeVariableImpl extends TypeMirrorImpl implements RuntimeTypeVariable, TypeVariable {
    @Nullable private final SimpleNameReference simpleNameReference;
    @Nullable private final WildcardTypeImpl capturedTypeArgument;
    @Nullable private volatile TypeParameterElementImpl formalTypeParameter;

    // Lazily initialized fields follow.
    @Nullable private volatile TypeMirrorImpl upperBound;
    @Nullable private volatile TypeMirrorImpl lowerBound;
    @Nullable private volatile ImmutableList<TypeDeclarationImpl> typeDeclarations;

    /**
     * Constructor for instances that must be frozen explicitly by calling
     * {@link #setUpperAndLowerBounds(TypeMirrorImpl, TypeMirrorImpl)}.
     *
     * @param types {@link CloudKeeperTypeReflection} instance
     * @param formalTypeParameter formal type parameter that the new type variable references
     * @param capturedTypeArgument the wildcard argument that captured during capture conversion, may be null if this
     *     type variable is not created during capture conversion
     */
    TypeVariableImpl(CloudKeeperTypeReflection types, TypeParameterElementImpl formalTypeParameter,
            @Nullable WildcardTypeImpl capturedTypeArgument) {
        super(State.CREATED, types);
        simpleNameReference = null;
        this.capturedTypeArgument = capturedTypeArgument;
        this.formalTypeParameter = Objects.requireNonNull(formalTypeParameter);
    }

    /**
     * Copy constructor (from unverified instance). Instance needs to be explicitly frozen before use.
     */
    TypeVariableImpl(BareTypeVariable original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        simpleNameReference = new SimpleNameReference(
            original.getFormalTypeParameter(), getCopyContext().newContextForProperty("formalTypeParameter"));
        capturedTypeArgument = null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        require(State.FINISHED);
        @Nullable TypeParameterElementImpl localFormalTypeParameter = formalTypeParameter;
        assert localFormalTypeParameter != null : "must be non-null when finished";

        TypeVariableImpl other = (TypeVariableImpl) otherObject;
        return localFormalTypeParameter.equals(other.formalTypeParameter)
            && getUpperBound().equals(other.getUpperBound())
            && getLowerBound().equals(other.getLowerBound());
    }

    @Override
    public int hashCode() {
        return Objects.hash(formalTypeParameter, getUpperBound(), getLowerBound());
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitTypeVariable(this, parameter);
    }

    @Override
    @Nullable
    public <R, P> R accept(TypeVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitTypeVariable(this, parameter);
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> asTypeDeclaration() {
        require(State.FINISHED);
        @Nullable ImmutableList<TypeDeclarationImpl> localTypeDeclarations = typeDeclarations;
        if (localTypeDeclarations == null) {
            // Preliminarily set typeDeclarations to a non-null value, in order to avoid infinite recursion in
            // case this type variable contains a reference to itself (which may, e.g., happen with recursive
            // bounds).
            typeDeclarations = ImmutableList.of();

            LinkedHashSet<TypeDeclarationImpl> set = new LinkedHashSet<>();
            set.addAll(getUpperBound().asTypeDeclaration());
            set.addAll(getLowerBound().asTypeDeclaration());
            localTypeDeclarations = ImmutableList.copyOf(set);
            typeDeclarations = localTypeDeclarations;
        }
        return localTypeDeclarations;
    }

    /**
     * Sets the upper and lower bound of a fresh type variable constructed with
     * {@link #TypeVariableImpl(CloudKeeperTypeReflection, TypeParameterElementImpl, WildcardTypeImpl)}.
     *
     * This method finishes the construction of the type variable, so that it may be properly used as
     * {@link RuntimeTypeVariable} instance subsequently.
     *
     * @param upperBound upper bounds
     * @param lowerBound lower bounds
     */
    TypeVariableImpl setUpperAndLowerBounds(TypeMirrorImpl upperBound, TypeMirrorImpl lowerBound) {
        requireNot(State.LINKED);
        Objects.requireNonNull(upperBound);
        Objects.requireNonNull(lowerBound);

        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        markAsCompleted();
        return this;
    }

    @Override
    @Nonnull
    public TypeParameterElementImpl getFormalTypeParameter() {
        require(State.FINISHED);
        @Nullable TypeParameterElementImpl localFormalTypeParameter = formalTypeParameter;
        assert localFormalTypeParameter != null : "must be non-null when finished";
        return localFormalTypeParameter;
    }

    @Override
    public TypeMirrorImpl getUpperBound() {
        require(State.FINISHED);

        @Nullable TypeMirrorImpl localUpperBound = upperBound;
        if (localUpperBound == null) {
            @Nullable TypeParameterElementImpl localFormalTypeParameter = formalTypeParameter;
            assert localFormalTypeParameter != null : "must be non-null when finished";
            localUpperBound = localFormalTypeParameter.asType().getUpperBound();
            upperBound = localUpperBound;
        }
        return localUpperBound;
    }

    @Override
    public TypeMirrorImpl getLowerBound() {
        require(State.FINISHED);

        @Nullable TypeMirrorImpl localLowerBound = lowerBound;
        if (localLowerBound == null) {
            @Nullable TypeParameterElementImpl localFormalTypeParameter = formalTypeParameter;
            assert localFormalTypeParameter != null : "must be non-null when finished";
            localLowerBound = localFormalTypeParameter.asType().getLowerBound();
            lowerBound = localLowerBound;
        }
        return localLowerBound;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Nullable
    WildcardTypeImpl getCapturedTypeArgument() {
        return capturedTypeArgument;
    }

    @Override
    public TypeParameterElementImpl asElement() {
        return getFormalTypeParameter();
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void finishTypeMirror(FinishContext context) throws LinkerException {
        assert simpleNameReference != null : "must be non-null when finish is called";
        formalTypeParameter = context.getTypeParameter(simpleNameReference);
    }

    @Override
    void verifyFreezable(VerifyContext context) {
        asTypeDeclaration();
    }
}
