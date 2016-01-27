package com.svbio.cloudkeeper.model.beans.type;

import com.svbio.cloudkeeper.model.bare.type.BareDeclaredType;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableQualifiedNamable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "declared-type")
@XmlType(propOrder = {"enclosingType", "declaration", "typeArguments"})
public final class MutableDeclaredType extends MutableTypeMirror<MutableDeclaredType> implements BareDeclaredType {
    private static final long serialVersionUID = 5928040914608039129L;

    @Nullable private MutableDeclaredType enclosingType;
    @Nullable private MutableQualifiedNamable declaration;
    private final ArrayList<MutableTypeMirror<?>> typeArguments = new ArrayList<>();

    public MutableDeclaredType() { }

    private MutableDeclaredType(Type original, CopyOption[] copyOptions) {
        Objects.requireNonNull(original);
        if (original instanceof Class<?>) {
            declaration = new MutableQualifiedNamable()
                .setQualifiedName(nameForClass((Class<?>) original, copyOptions));
        } else if (original instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) original;
            @Nullable Type ownerType = parameterizedType.getOwnerType();
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            enclosingType = ownerType != null && isNested(rawType, copyOptions)
                ? (MutableDeclaredType) fromJavaType(ownerType, copyOptions)
                : null;
            declaration = new MutableQualifiedNamable()
                .setQualifiedName(nameForClass((Class<?>) parameterizedType.getRawType(), copyOptions));
            for (Type typeArgument: Arrays.asList(parameterizedType.getActualTypeArguments())) {
                typeArguments.add(fromJavaType(typeArgument, copyOptions));
            }
        } else {
            throw new IllegalArgumentException(String.format(
                "Expected instance of one of %s, but got %s.", Arrays.asList(Class.class, ParameterizedType.class),
                original
            ));
        }
    }

    public static MutableDeclaredType fromType(Type original, CopyOption... copyOptions) {
        return new MutableDeclaredType(original, copyOptions);
    }

    private MutableDeclaredType(BareDeclaredType original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        enclosingType
            = (MutableDeclaredType) MutableTypeMirror.copyOfTypeMirror(original.getEnclosingType(), copyOptions);
        declaration = MutableQualifiedNamable.copyOf(original.getDeclaration(), copyOptions);
        for (BareTypeMirror typeArgument: original.getTypeArguments()) {
            typeArguments.add(copyOfTypeMirror(typeArgument, copyOptions));
        }
    }

    @Nullable
    public static MutableDeclaredType copyOfDeclaredType(@Nullable BareDeclaredType original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableDeclaredType(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableDeclaredType other = (MutableDeclaredType) otherObject;
        return Objects.equals(enclosingType, other.enclosingType)
            && Objects.equals(declaration, other.declaration)
            && Objects.equals(typeArguments, other.typeArguments);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(enclosingType, declaration, typeArguments);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitDeclaredType(this, parameter);
    }

    @Override
    protected MutableDeclaredType self() {
        return this;
    }

    @XmlElement(name = "enclosing-type")
    @Override
    @Nullable
    public MutableDeclaredType getEnclosingType() {
        return enclosingType;
    }

    public MutableDeclaredType setEnclosingType(@Nullable MutableDeclaredType enclosingType) {
        this.enclosingType = enclosingType;
        return this;
    }

    @XmlAttribute(name = "ref")
    @Override
    @Nullable
    public MutableQualifiedNamable getDeclaration() {
        return declaration;
    }

    public MutableDeclaredType setDeclaration(@Nullable MutableQualifiedNamable declaration) {
        this.declaration = declaration;
        return this;
    }

    public MutableDeclaredType setDeclaration(String declarationName) {
        return setDeclaration(
            new MutableQualifiedNamable().setQualifiedName(declarationName)
        );
    }

    @XmlElementWrapper(name = "type-arguments")
    @XmlElementRef
    @Override
    public List<MutableTypeMirror<?>> getTypeArguments() {
        return typeArguments;
    }

    public MutableDeclaredType setTypeArguments(List<MutableTypeMirror<?>> typeArguments) {
        Objects.requireNonNull(typeArguments);
        List<MutableTypeMirror<?>> backup = new ArrayList<>(typeArguments);
        this.typeArguments.clear();
        this.typeArguments.addAll(backup);
        return this;
    }

    @Override
    public String toString() {
        return BareDeclaredType.Default.toString(this);
    }
}
