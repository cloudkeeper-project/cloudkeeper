package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeParameterElement;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Type-parameter element.
 *
 * <p>While Java 8 supports annotations on type parameters, CloudKeeper currently does not support them. Therefore,
 * {@link #getAnnotationMirrors()} and {@link #getAnnotation(Class)} always return default values.
 */
final class TypeParameterElementImpl
        extends AnnotatedConstructImpl
        implements RuntimeTypeParameterElement, ITypeElementImpl, TypeParameterElement {
    private final SimpleName simpleName;
    private final ImmutableList<TypeMirrorImpl> bounds;

    @Nullable private volatile IParameterizableImpl genericDeclaration;
    @Nullable private volatile Name qualifiedName;
    @Nullable private volatile TypeVariableImpl type;

    TypeParameterElementImpl(BareTypeParameterElement original, CopyContext parentContext)
            throws LinkerException {
        super(original, parentContext);
        simpleName = Preconditions.requireNonNull(
            original.getSimpleName(), getCopyContext().newContextForProperty("simpleName"));
        bounds = immutableListOf(original.getBounds(), "bounds", TypeMirrorImpl::copyOf);
    }

    @Override
    public String toString() {
        return BareTypeParameterElement.Default.toHumanReadableString(this);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> visitor, @Nullable P parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }

    @Override
    @Nullable
    public IElementImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    public ImmutableList<? extends ITypeElementImpl> getEnclosedElements() {
        return ImmutableList.of();
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        return null;
    }

    @Override
    public Name getQualifiedName() {
        require(State.FINISHED);
        @Nullable Name localQualifiedName = qualifiedName;
        assert localQualifiedName != null : "must be non-null when finished";
        return localQualifiedName;
    }

    @Override
    @Nonnull
    public SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public ImmutableList<TypeMirrorImpl> getBounds() {
        return bounds;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.TYPE_PARAMETER;
    }

    @Override
    public IParameterizableImpl getGenericElement() {
        require(State.FINISHED);
        @Nullable IParameterizableImpl localGenericDeclaration = genericDeclaration;
        assert localGenericDeclaration != null : "must be non-null when finished";
        return localGenericDeclaration;
    }

    @Override
    public IParameterizableImpl getEnclosingElement() {
        return getGenericElement();
    }

    @Override
    public TypeVariableImpl asType() {
        require(State.FINISHED);
        @Nullable TypeVariableImpl localType = type;
        assert localType != null : "must be non-null when finished";
        return localType;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    protected final void collectEnclosedByAnnotatedConstruct(
        Collection<AbstractFreezable> freezables) {
        freezables.addAll(bounds);
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) throws LinkerException {
        IParameterizableImpl localGenericDeclaration
            = context.getRequiredEnclosingFreezable(IParameterizableImpl.class);
        genericDeclaration = localGenericDeclaration;
        qualifiedName = localGenericDeclaration.getQualifiedName().join(simpleName);

        CloudKeeperTypeReflection types = context.getTypes();
        TypeMirrorImpl extendsBound;
        if (bounds.isEmpty()) {
            extendsBound = types.typeMirror(Object.class);
        } else if (bounds.size() == 1) {
            extendsBound = bounds.get(0);
        } else {
            extendsBound = new IntersectionTypeImpl(types, bounds);
        }
        type = new TypeVariableImpl(types, this, null)
            .setUpperAndLowerBounds(extendsBound, types.getNullType());
    }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
