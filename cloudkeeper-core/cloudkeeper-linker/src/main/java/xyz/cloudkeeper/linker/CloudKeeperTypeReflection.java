package xyz.cloudkeeper.linker;

import net.florianschoppmann.java.type.AbstractTypes;
import xyz.cloudkeeper.model.bare.type.BarePrimitiveType;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.runtime.element.module.TypeRelationship;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static xyz.cloudkeeper.model.runtime.element.module.TypeRelationship.APPLY_TO_ALL;
import static xyz.cloudkeeper.model.runtime.element.module.TypeRelationship.INCOMPATIBLE;
import static xyz.cloudkeeper.model.runtime.element.module.TypeRelationship.MERGE;

final class CloudKeeperTypeReflection extends AbstractTypes {
    private final Map<TypeKind, PrimitiveTypeImpl> primitiveTypeKinds;

    private boolean completed = false;
    private Map<TypeKind, TypeDeclarationImpl> boxedTypes;
    private TypeDeclarationImpl objectElement;
    private TypeDeclarationImpl serializableElement;
    private TypeDeclarationImpl clonableElement;
    private TypeDeclarationImpl collectionElement;
    private NoTypeImpl voidType;
    private NoTypeImpl noneType;
    private NullTypeImpl nullType;


    /**
     * Constructor.
     *
     * <p>Constructing the CloudKeeper type utilities and the system repository poses a chicken-and-egg problem: Type
     * declarations in the system repository need the type utilities, and the type utilities need a reference to several
     * fundamental types (that is, types mentioned by the Java Language Specification, such as {@link Object}).
     *
     * <p>Therefore, constructing this instance is necessarily a multi-step process. Specifically, before the newly
     * constructed instance can be used, {@link #complete(ElementResolver)} needs to be called to make the instance
     * effectively immutable.
     */
    CloudKeeperTypeReflection() {
        EnumMap<TypeKind, PrimitiveTypeImpl> primitiveTypeKindsMap = new EnumMap<>(TypeKind.class);

        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.BOOLEAN, TypeKind.BOOLEAN);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.CHAR, TypeKind.CHAR);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.BYTE, TypeKind.BYTE);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.SHORT, TypeKind.SHORT);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.INT, TypeKind.INT);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.LONG, TypeKind.LONG);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.FLOAT, TypeKind.FLOAT);
        addPrimitiveType(primitiveTypeKindsMap, BarePrimitiveType.Kind.DOUBLE, TypeKind.DOUBLE);
        primitiveTypeKinds = Collections.unmodifiableMap(primitiveTypeKindsMap);
    }

    private void addPrimitiveType(Map<TypeKind, PrimitiveTypeImpl> primitiveTypeKinds,
            BarePrimitiveType.Kind kind, TypeKind typeKind) {
        PrimitiveTypeImpl primitiveType = new PrimitiveTypeImpl(this, kind);
        primitiveTypeKinds.put(typeKind, primitiveType);
    }

    /**
     * Finishes construction of this instance and makes it effectively immutable.
     *
     * @param elementResolver the repository that contains the required type declarations
     */
    void complete(ElementResolver elementResolver) {
        assert !completed;

        EnumMap<TypeKind, TypeDeclarationImpl> boxedTypesEnumMap = new EnumMap<>(TypeKind.class);
        addBoxedType(boxedTypesEnumMap, TypeKind.BOOLEAN, Boolean.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.CHAR, Character.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.BYTE, Byte.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.SHORT, Short.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.INT, Integer.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.LONG, Long.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.FLOAT, Float.class, elementResolver);
        addBoxedType(boxedTypesEnumMap, TypeKind.DOUBLE, Double.class, elementResolver);
        boxedTypes = Collections.unmodifiableMap(boxedTypesEnumMap);

        objectElement = Objects.requireNonNull(
            elementResolver.getElement(TypeDeclarationImpl.class, Name.qualifiedName(Object.class.getName())));
        serializableElement = Objects.requireNonNull(
            elementResolver.getElement(TypeDeclarationImpl.class, Name.qualifiedName(Serializable.class.getName())));
        clonableElement = Objects.requireNonNull(
            elementResolver.getElement(TypeDeclarationImpl.class, Name.qualifiedName(Cloneable.class.getName())));
        collectionElement = Objects.requireNonNull(
            elementResolver.getElement(TypeDeclarationImpl.class, Name.qualifiedName(Collection.class.getName())));

        voidType = new NoTypeImpl(this, TypeKind.VOID);
        noneType = new NoTypeImpl(this, TypeKind.NONE);
        nullType = new NullTypeImpl(this);

        completed = true;
    }

    private static void addBoxedType(Map<TypeKind, TypeDeclarationImpl> boxedTypes, TypeKind typeKind,
            Class<?> clazz, ElementResolver elementResolver) {
        boxedTypes.put(
            typeKind,
            elementResolver.getElement(TypeDeclarationImpl.class, Name.qualifiedName(clazz.getName()))
        );
    }

    @Override
    protected TypeMirrorImpl typeMirror(Type type) {
        assert completed;
        if (Object.class.equals(type)) {
            return objectElement.asType();
        } else if (Serializable.class.equals(type)) {
            return serializableElement.asType();
        } else if (Cloneable.class.equals(type)) {
            return clonableElement.asType();
        } else {
            throw new UnsupportedOperationException(String.format(
                "Expected one of %s, but got %s.",
                Arrays.asList(Object.class, Serializable.class, Cloneable.class), type
            ));
        }
    }

    @Override
    protected void requireValidType(@Nullable TypeMirror type) {
        if (!(type instanceof TypeMirrorImpl) && type != null) {
            throw new IllegalArgumentException(String.format(
                "Expected instance of %s, but got instance of %s.", TypeMirrorImpl.class, type.getClass()
            ));
        }
    }

    @Override
    protected void requireValidElement(Element element) {
        Objects.requireNonNull(element);
        if (!(element instanceof ITypeElementImpl)) {
            throw new IllegalArgumentException(String.format(
                "Expected instance of %s, but got instance of %s.",
                ITypeElementImpl.class, element.getClass()
            ));
        }
    }

    private static List<TypeMirrorImpl> toList(TypeMirror... types) {
        List<TypeMirrorImpl> list = new ArrayList<>(types.length);
        for (TypeMirror typeArgument: types) {
            list.add((TypeMirrorImpl) typeArgument);
        }
        return list;
    }

    @Override
    public TypeElement boxedClass(PrimitiveType primitiveType) {
        assert completed;
        Objects.requireNonNull(primitiveType);
        requireValidType(primitiveType);

        return boxedTypes.get(primitiveType.getKind());
    }

    @Override
    public PrimitiveType unboxedType(TypeMirror type) {
        assert completed;
        requireValidType(type);

        if (type.getKind() != TypeKind.DECLARED) {
            throw new IllegalArgumentException(String.format("Expected boxed type, but got %s.", type));
        }

        Name name = ((DeclaredTypeImpl) type).asElement().getQualifiedName();
        if (name.contentEquals(Double.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.DOUBLE);
        } else if (name.contentEquals(Float.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.FLOAT);
        } else if (name.contentEquals(Long.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.LONG);
        } else if (name.contentEquals(Integer.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.INT);
        } else if (name.contentEquals(Short.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.SHORT);
        } else if (name.contentEquals(Byte.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.BYTE);
        } else if (name.contentEquals(Character.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.CHAR);
        } else if (name.contentEquals(Boolean.class.getName())) {
            return primitiveTypeKinds.get(TypeKind.BOOLEAN);
        } else {
            throw new IllegalArgumentException(String.format("Expected boxed type, but got %s.", type));
        }
    }

    @Override
    public PrimitiveTypeImpl getPrimitiveType(TypeKind kind) {
        assert completed;
        Objects.requireNonNull(kind);
        @Nullable PrimitiveTypeImpl primitiveType = primitiveTypeKinds.get(kind);
        if (primitiveType == null) {
            throw new IllegalArgumentException(String.format("Expected primitive kind, but got %s.", kind));
        }
        return primitiveType;
    }

    @Override
    public NullTypeImpl getNullType() {
        assert completed;
        return nullType;
    }

    @Override
    public NoTypeImpl getNoType(TypeKind kind) {
        assert completed;
        Objects.requireNonNull(kind);
        if (kind == TypeKind.VOID) {
            return voidType;
        } else if (kind == TypeKind.NONE) {
            return noneType;
        } else {
            throw new IllegalArgumentException(String.format("Expected one of %s, but got %s.",
                Arrays.asList(TypeKind.VOID, TypeKind.NONE), kind));
        }
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        assert completed;
        requireValidType(Objects.requireNonNull(componentType));

        return new ArrayTypeImpl(this, (TypeMirrorImpl) componentType);
    }

    @Override
    public IntersectionTypeImpl getIntersectionType(TypeMirror... bounds) {
        assert completed;
        Objects.requireNonNull(bounds);
        if (bounds.length == 0) {
            throw new IllegalArgumentException("Expected at least one bound.");
        }
        requireValidTypes(bounds);

        List<TypeMirrorImpl> newBounds = toList(bounds);
        return new IntersectionTypeImpl(this, newBounds);
    }

    @Override
    public WildcardTypeImpl getWildcardType(@Nullable TypeMirror extendsBound, @Nullable TypeMirror superBound) {
        assert completed;
        requireValidType(extendsBound);
        requireValidType(superBound);

        return new WildcardTypeImpl(this, (TypeMirrorImpl) extendsBound, (TypeMirrorImpl) superBound);
    }

    @Override
    public DeclaredType getDeclaredType(@Nullable DeclaredType containing, TypeElement typeElement,
            TypeMirror... typeArguments) {
        assert completed;
        requireValidType(containing);
        requireValidElement(typeElement);
        requireValidTypes(typeArguments);

        TypeMirrorImpl newContainingType = containing == null
            ? noneType
            : (TypeMirrorImpl) containing;
        return new DeclaredTypeImpl(this, newContainingType, (TypeDeclarationImpl) typeElement,
            toList(typeArguments));
    }

    @Override
    public DeclaredTypeImpl getDeclaredType(TypeElement typeElement, TypeMirror... typeArguments) {
        assert completed;
        requireValidElement(typeElement);
        requireValidTypes(typeArguments);

        return new DeclaredTypeImpl(this, noneType, (TypeDeclarationImpl) typeElement,
            toList(typeArguments));
    }

    @Override
    protected void setTypeVariableBounds(TypeVariable typeVariable, TypeMirror upperBound, TypeMirror lowerBound) {
        assert completed;
        requireValidType(Objects.requireNonNull(typeVariable));
        requireValidType(Objects.requireNonNull(upperBound));
        requireValidType(Objects.requireNonNull(lowerBound));

        ((TypeVariableImpl) typeVariable).setUpperAndLowerBounds(
            (TypeMirrorImpl) upperBound,
            (TypeMirrorImpl) lowerBound
        );
    }

    @Override
    protected TypeVariableImpl createTypeVariable(TypeParameterElement typeParameter,
            @Nullable WildcardType capturedTypeArgument) {
        assert completed;
        requireValidElement(Objects.requireNonNull(typeParameter));
        requireValidType(capturedTypeArgument);

        return new TypeVariableImpl(
            this,
            (TypeParameterElementImpl) typeParameter,
            (WildcardTypeImpl) capturedTypeArgument
        );
    }

    @Override
    @Nullable
    protected WildcardType capturedTypeArgument(TypeVariable typeVariable) {
        assert completed;
        requireValidType(Objects.requireNonNull(typeVariable));

        return ((TypeVariableImpl) typeVariable).getCapturedTypeArgument();
    }

    private static UnsupportedOperationException unsupportedException() {
        return new UnsupportedOperationException(
            "Operation not necessary for CloudKeeper and therefore not currently supported."
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Java assignability of types is irrelevant to CloudKeeper. Use {@link #isSubtype(TypeMirror, TypeMirror)}
     * instead.
     * @throws UnsupportedOperationException whenever this method is called
     */
    @Override
    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        throw unsupportedException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>CloudKeeper does not support method-signature queries.
     * @throws UnsupportedOperationException whenever this method is called
     */
    @Override
    public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
        throw unsupportedException();
    }

    /**
     * {@inheritDoc}
     *
     * Currently unsupported by CloudKeeper.
     * @throws UnsupportedOperationException whenever this method is called
     */
    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        throw unsupportedException();
    }

    /**
     * {@inheritDoc}
     *
     * Currently not supported. Use {@link #resolveActualTypeArguments(TypeElement, TypeMirror)} instead.
     * @throws UnsupportedOperationException whenever this method is called
     */
    @Override
    public TypeMirror asMemberOf(DeclaredType containing, Element element) {
        throw unsupportedException();
    }

    TypeRelationship relationship(TypeMirror fromType, TypeMirror toType) {
        assert completed;
        // First check for apply-to-all connection.
        @Nullable List<? extends TypeMirror> actualTypeParametersOnSource
            = resolveActualTypeArguments(collectionElement, fromType);
        if (actualTypeParametersOnSource != null && isSubtype(actualTypeParametersOnSource.get(0), toType)) {
            return APPLY_TO_ALL;
        }

        // Now check for merge connection.
        @Nullable List<? extends TypeMirror> actualTypeParametersOnTarget
            = resolveActualTypeArguments(collectionElement, toType);
        if (actualTypeParametersOnTarget != null && isSubtype(fromType, actualTypeParametersOnTarget.get(0))) {
            return MERGE;
        }

        // Finally, check for simple connection.
        if (isSubtype(fromType, toType)) {
            return TypeRelationship.SIMPLE;
        }

        return INCOMPATIBLE;
    }
}
