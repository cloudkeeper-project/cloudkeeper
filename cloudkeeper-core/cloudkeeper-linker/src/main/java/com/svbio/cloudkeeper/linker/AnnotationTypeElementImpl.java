package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationValue;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.annotation.RuntimeAnnotationTypeElement;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.util.Collection;

final class AnnotationTypeElementImpl
        extends AnnotatedConstructImpl
        implements RuntimeAnnotationTypeElement, IElementImpl {
    private final SimpleName simpleName;
    private final TypeMirrorImpl returnType;
    @Nullable private final TypeMirrorImpl typeOfDefaultValue;
    @Nullable private final AnnotationValueImpl defaultValue;

    private volatile AnnotationTypeDeclarationImpl enclosingElement;
    private volatile Name qualifiedName;

    AnnotationTypeElementImpl(BareAnnotationTypeElement original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        simpleName
            = Preconditions.requireNonNull(original.getSimpleName(), context.newContextForProperty("simpleName"));

        // defaultValue may be null.
        @Nullable BareAnnotationValue originalDefaultValue = original.getDefaultValue();
        defaultValue = originalDefaultValue == null
            ? null
            : new AnnotationValueImpl(originalDefaultValue, context.newContextForProperty("defaultValue"));
        returnType
            = TypeMirrorImpl.copyOf(original.getReturnType(), context.newContextForProperty("returnType"));

        if (defaultValue != null) {
            MutableTypeMirror<?> mutableType
                = MutableTypeMirror.fromJavaType(unbox(defaultValue.toNativeValue().getClass()));
            typeOfDefaultValue = TypeMirrorImpl.optionalCopyOf(mutableType, context);
        } else {
            typeOfDefaultValue = null;
        }
    }

    private static Class<?> unbox(Class<?> clazz) {
        if (Double.class.equals(clazz)) {
            return double.class;
        } else if (Float.class.equals(clazz)) {
            return float.class;
        } else if (Long.class.equals(clazz)) {
            return long.class;
        } else if (Integer.class.equals(clazz)) {
            return int.class;
        } else if (Short.class.equals(clazz)) {
            return short.class;
        } else if (Byte.class.equals(clazz)) {
            return byte.class;
        } else if (Character.class.equals(clazz)) {
            return char.class;
        } else if (Boolean.class.equals(clazz)) {
            return boolean.class;
        } else {
            return clazz;
        }
    }

    @Override
    public String toString() {
        return BareAnnotationTypeElement.Default.toHumanReadableString(this);
    }

    @Override
    public IElementImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    public AnnotationTypeDeclarationImpl getEnclosingElement() {
        return enclosingElement;
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        return null;
    }

    @Override
    public ImmutableList<? extends IElementImpl> getEnclosedElements() {
        return ImmutableList.of();
    }

    @Override
    public Name getQualifiedName() {
        require(State.FINISHED);
        return qualifiedName;
    }

    @Override
    public SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public TypeMirrorImpl getReturnType() {
        return returnType;
    }

    @Override
    @Nullable
    public AnnotationValueImpl getDefaultValue() {
        return defaultValue;
    }

    @Override
    void finishFreezable(FinishContext context) throws LinkerException {
        enclosingElement = context.getRequiredEnclosingFreezable(AnnotationTypeDeclarationImpl.class);
        qualifiedName = enclosingElement.getQualifiedName().join(simpleName);
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.add(returnType);
        if (typeOfDefaultValue != null) {
            freezables.add(typeOfDefaultValue);
        }
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) throws LinkerException {
        if (typeOfDefaultValue != null) {
            Preconditions.requireCondition(
                returnType.equals(typeOfDefaultValue),
                getCopyContext(),
                "Explicit return type of annotation element and type of default value do not match."
            );
        }
    }
}
