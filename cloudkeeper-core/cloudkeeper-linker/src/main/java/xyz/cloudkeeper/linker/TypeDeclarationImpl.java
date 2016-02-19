package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.runtime.element.RuntimePluginDeclarationVisitor;
import xyz.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class TypeDeclarationImpl
        extends PluginDeclarationImpl
        implements RuntimeTypeDeclaration, IParameterizableImpl, TypeElement {
    @Nullable private final TypeMirrorImpl declaredSuperclass;
    private final ImmutableList<TypeMirrorImpl> interfaces;
    private final Kind kind;
    private final ImmutableList<TypeDeclarationImpl> nestedTypeDeclarations;
    private final ImmutableList<TypeParameterElementImpl> typeParameterElements;
    private final Map<SimpleName, ITypeElementImpl> enclosedElementsMap;

    @Nullable private volatile CloudKeeperTypeReflection types;
    @Nullable private volatile TypeDeclarationImpl enclosingTypeDeclaration;
    @Nullable private volatile TypeMirrorImpl superclass;
    @Nullable private volatile Class<?> typeClass;

    // Lazily initialized fields follow.
    @Nullable private DeclaredTypeImpl type;

    TypeDeclarationImpl(BareTypeDeclaration original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);

        CopyContext context = getCopyContext();
        kind = Objects.requireNonNull(original.getTypeDeclarationKind());

        @Nullable BareTypeMirror originalSuperclass = original.getSuperclass();
        CopyContext superclassContext = context.newContextForProperty("superclass");
        declaredSuperclass = originalSuperclass == null
            ? null
            : TypeMirrorImpl.copyOf(originalSuperclass, superclassContext);

        interfaces = immutableListOf(original.getInterfaces(), "interfaces", TypeMirrorImpl::copyOf);

        Map<SimpleName, ITypeElementImpl> newEnclosedElementsMap = new LinkedHashMap<>();

        List<TypeParameterElementImpl> newTypeParameterElements = new ArrayList<>();
        collect(
            original.getTypeParameters(),
            "typeParameters",
            TypeParameterElementImpl::new,
            Arrays.asList(
                mapAccumulator(newEnclosedElementsMap, TypeParameterElementImpl::getSimpleName),
                listAccumulator(newTypeParameterElements)
            )
        );
        typeParameterElements = ImmutableList.copyOf(newTypeParameterElements);

        List<TypeDeclarationImpl> newNestedTypeDeclarations = new ArrayList<>();
        collect(
            original.getNestedTypeDeclarations(),
            "nestedTypeDeclarations",
            TypeDeclarationImpl::new,
            Arrays.asList(
                mapAccumulator(newEnclosedElementsMap, TypeDeclarationImpl::getSimpleName),
                listAccumulator(newNestedTypeDeclarations)
            )
        );
        nestedTypeDeclarations = ImmutableList.copyOf(newNestedTypeDeclarations);

        enclosedElementsMap = Collections.unmodifiableMap(newEnclosedElementsMap);
    }

    @Override
    public TypeDeclarationImpl getSuperAnnotatedConstruct() {
        require(State.FINISHED);
        @Nullable TypeMirrorImpl localSuperclass = superclass;
        assert (localSuperclass instanceof DeclaredTypeImpl) || (localSuperclass instanceof NoTypeImpl);
        return localSuperclass instanceof DeclaredTypeImpl
            ? ((DeclaredTypeImpl) localSuperclass).getDeclaration()
            : null;
    }

    @Override
    public String toString() {
        return BareTypeDeclaration.Default.toString(this);
    }

    @Override
    public ElementKind getKind() {
        return kind == Kind.CLASS
            ? ElementKind.CLASS
            : ElementKind.INTERFACE;
    }

    @Override
    public Set<Modifier> getModifiers() {
        return Collections.emptySet();
    }

    @Override
    public ImmutableList<ITypeElementImpl> getEnclosedElements() {
        return ImmutableList.copyOf(enclosedElementsMap.values());
    }

    @Override
    public <T extends RuntimeElement> T getEnclosedElement(Class<T> clazz, SimpleName simpleName) {
        IElementImpl element = enclosedElementsMap.get(simpleName);
        if (clazz.isInstance(element)) {
            @SuppressWarnings("unchecked")
            T typedElement = (T) element;
            return typedElement;
        } else {
            return null;
        }
    }

    @Override
    @Nonnull
    public ITypeElementImpl getEnclosingElement() {
        require(State.FINISHED);
        @Nullable TypeDeclarationImpl localEnclosingTypeDeclaration = enclosingTypeDeclaration;
        assert localEnclosingTypeDeclaration != null : "must be non-null when finished";
        return localEnclosingTypeDeclaration;
    }

    @Override
    public NestingKind getNestingKind() {
        require(State.FINISHED);
        boolean isTopLevel = enclosingTypeDeclaration == null;
        return isTopLevel
            ? NestingKind.TOP_LEVEL
            : NestingKind.MEMBER;
    }

    @Override
    public DeclaredTypeImpl asType() {
        require(State.FINISHED);
        @Nullable CloudKeeperTypeReflection localTypes = types;
        assert localTypes != null : "must be non-null when finished";

        // No synchronization/volatile needed because reference read/writes are atomic due to JLS §17.7.
        @Nullable DeclaredTypeImpl localType = type;
        if (localType == null) {
            List<TypeMirrorImpl> prototypicalTypeArguments = new ArrayList<>(typeParameterElements.size());
            for (TypeParameterElementImpl typeParameter: typeParameterElements) {
                prototypicalTypeArguments.add(typeParameter.asType());
            }

            @Nullable TypeDeclarationImpl localEnclosingTypeDeclaration = enclosingTypeDeclaration;
            TypeMirrorImpl enclosingType = localEnclosingTypeDeclaration == null
                ? localTypes.getNoType(TypeKind.NONE)
                : localEnclosingTypeDeclaration.asType();
            localType = new DeclaredTypeImpl(localTypes, enclosingType, this, prototypicalTypeArguments);
            type = localType;
        }
        return localType;
    }

    @Override
    @Nullable
    public <T, P> T accept(ElementVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitType(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public Kind getTypeDeclarationKind() {
        return kind;
    }

    @Override
    public TypeMirrorImpl getSuperclass() {
        require(State.FINISHED);
        @Nullable TypeMirrorImpl localSuperClass = superclass;
        assert localSuperClass != null;
        return localSuperClass;
    }

    @Override
    public ImmutableList<TypeMirrorImpl> getInterfaces() {
        return interfaces;
    }

    @Override
    public ImmutableList<TypeParameterElementImpl> getTypeParameters() {
        return typeParameterElements;
    }

    @Override
    public ImmutableList<TypeDeclarationImpl> getNestedTypeDeclarations() {
        return nestedTypeDeclarations;
    }

    @Override
    public Class<?> toClass() {
        require(State.FINISHED);
        @Nullable Class<?> localTypeClass = typeClass;
        if (localTypeClass == null) {
            throw new IllegalStateException(String.format(
                "toClass() called for %s even though the class provider configured in the linker options returned "
                    + "no class.",
                this
            ));
        }
        return localTypeClass;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        if (declaredSuperclass != null) {
            freezables.add(declaredSuperclass);
        }
        freezables.addAll(interfaces);
        freezables.addAll(typeParameterElements);
        freezables.addAll(nestedTypeDeclarations);
    }

    @Override
    void finishFreezable(FinishContext context) throws LinkerException {
        CloudKeeperTypeReflection localTypes = context.getTypes();
        types = localTypes;
        enclosingTypeDeclaration = context.getOptionalEnclosingFreezable(TypeDeclarationImpl.class).orElse(null);
        typeClass = context.resolveJavaClass(this, Object.class);

        PackageImpl enclosingPackage = context.getRequiredEnclosingFreezable(PackageImpl.class);
        if (declaredSuperclass != null) {
            superclass = declaredSuperclass;
        } else if (kind == Kind.INTERFACE
                || (
                    enclosingPackage.getQualifiedName().contentEquals(Object.class.getPackage().getName())
                    && getSimpleName().contentEquals(Object.class.getSimpleName())
                )) {
            // See TypeElement#getSuperclass()
            superclass = new NoTypeImpl(localTypes, TypeKind.NONE);
        } else {
            superclass = context.getObjectType();
        }
    }

    @Override
    void verifyFreezable(VerifyContext context) {
        // Perform pre-computation and cache the result.
        asType();
    }
}
