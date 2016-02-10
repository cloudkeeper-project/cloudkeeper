package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.type.BareArrayType;
import com.svbio.cloudkeeper.model.bare.type.BareDeclaredType;
import com.svbio.cloudkeeper.model.bare.type.BareNoType;
import com.svbio.cloudkeeper.model.bare.type.BarePrimitiveType;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.bare.type.BareTypeVariable;
import com.svbio.cloudkeeper.model.bare.type.BareWildcardType;
import com.svbio.cloudkeeper.model.runtime.element.module.TypeRelationship;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import com.svbio.cloudkeeper.model.util.ImmutableList;
import net.florianschoppmann.java.type.AnnotatedConstruct;

import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

abstract class TypeMirrorImpl extends LocatableImpl implements RuntimeTypeMirror, AnnotatedConstruct {
    @Nullable private volatile CloudKeeperTypeReflection types;

    /**
     * Constructor for type-mirror instances.
     *
     * <p>If {@code initialState} is less than {@link State#PRECOMPUTED}, this instance may be frozen using
     * {@link #markAsCompleted()}, but not through {@link #complete(FinishContext, VerifyContext)} or any of the related
     * lifecycle methods. Correspondingly, this constructor does not provide a {@link CopyContext}.
     *
     * @param initialState initial lifecycle state of the new instance
     * @param types type utilities
     */
    TypeMirrorImpl(State initialState, CloudKeeperTypeReflection types) {
        super(initialState, null);
        this.types = Objects.requireNonNull(types);
    }

    TypeMirrorImpl(BareTypeMirror original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
    }

    /**
     * Visitor that creates a runtime representation of a {@link BareTypeMirror} instance.
     *
     * <p>This visitor only calls methods in {@link CloudKeeperTypeReflection} that are
     * safe to be called <em>before</em> it becomes effectively immutable.
     */
    private enum CopyVisitor implements BareTypeMirrorVisitor<Try<? extends TypeMirrorImpl>, CopyContext> {
        INSTANCE;

        @Override
        public Try<ArrayTypeImpl> visitArrayType(BareArrayType original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new ArrayTypeImpl(original, parentContext));
        }

        @Override
        public Try<DeclaredTypeImpl> visitDeclaredType(BareDeclaredType original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new DeclaredTypeImpl(original, parentContext));
        }

        @Override
        public Try<PrimitiveTypeImpl> visitPrimitive(BarePrimitiveType original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new PrimitiveTypeImpl(original, parentContext));
        }

        @Override
        public Try<TypeVariableImpl> visitTypeVariable(BareTypeVariable original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new TypeVariableImpl(original, parentContext));
        }

        @Override
        public Try<WildcardTypeImpl> visitWildcardType(BareWildcardType original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new WildcardTypeImpl(original, parentContext));
        }

        @Override
        public Try<NoTypeImpl> visitNoType(BareNoType original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            return Try.run(() -> new NoTypeImpl(parentContext));
        }

        @Override
        @Nullable
        public Try<TypeMirrorImpl> visitOther(BareTypeMirror original, @Nullable CopyContext parentContext) {
            assert parentContext != null;
            try {
                Preconditions.requireCondition(false, parentContext.newContextForChild(original),
                    "Type %s is neither a declared type, a type variable, a wildcard type argument, nor a no-type "
                        + "pseudo type.", original);
                return null;
            } catch (LinkerException exception) {
                return Try.failure(exception);
            }
        }
    }

    /**
     * Returns a new linked and verified type mirror of the given bare type mirror; or {@code null} if the first
     * argument is null.
     *
     * @param original original type mirror
     * @param parentContext parent copy context
     * @return the linked and verified type mirror (in unfinished state)
     */
    @Nullable
    static TypeMirrorImpl optionalCopyOf(@Nullable BareTypeMirror original, CopyContext parentContext)
            throws LinkerException {
        return original == null
            ? null
            : copyOf(original, parentContext);
    }

    static TypeMirrorImpl copyOf(@Nullable BareTypeMirror original, CopyContext parentContext) throws LinkerException {
        Preconditions.requireNonNull(original, parentContext);
        @Nullable Try<? extends TypeMirrorImpl> copy = original.accept(CopyVisitor.INSTANCE, parentContext);
        assert copy != null;
        return copy.get();
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return types == ((TypeMirrorImpl) otherObject).types;
    }

    @Override
    public abstract int hashCode();

    @Override
    public final String toString() {
        return getTypes().toString(this);
    }

    static UnsupportedOperationException unsupportedException() {
        return new UnsupportedOperationException(String.format(
            "Annotations not currently supported by %s and subclasses.", TypeMirrorImpl.class
        ));
    }

    @Override
    public final List<? extends AnnotationMirror> getAnnotationMirrors() {
        throw unsupportedException();
    }

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        throw unsupportedException();
    }

    @Override
    public final <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        throw unsupportedException();
    }

    final CloudKeeperTypeReflection getTypes() {
        require(State.FINISHED);
        @Nullable CloudKeeperTypeReflection localTypes = types;
        assert localTypes != null : "must be non-null when finished";
        return localTypes;
    }

    @Override
    public abstract ImmutableList<TypeDeclarationImpl> asTypeDeclaration();

    @Override
    public final boolean isLinkableFrom(RuntimeTypeMirror type) {
        return getTypes().isSubtype(type, this);
    }

    @Override
    public final TypeRelationship relationshipTo(RuntimeTypeMirror otherType) {
        return getTypes().relationship(this, otherType);
    }

    @Override
    final void preProcessFreezable(FinishContext context) { }

    @Override
    final void finishFreezable(FinishContext context) throws LinkerException {
        types = context.getTypes();
        finishTypeMirror(context);
    }

    abstract void finishTypeMirror(FinishContext context) throws LinkerException;
}
