package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.InvalidClassException;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeParameterElement;
import com.svbio.cloudkeeper.model.beans.type.MutableTypeMirror;
import com.svbio.cloudkeeper.model.immutable.Location;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class DSLTypeDeclaration extends DSLMixinPluginDeclaration implements BareTypeDeclaration {
    private final SimpleName simpleName;
    @Nullable private final MutableTypeMirror<?> superclass;
    private final List<MutableTypeMirror<?>> interfaces = new ArrayList<>();
    private final Kind kind;
    private final List<MutableTypeParameterElement> typeParameters;

    /**
     * Construct a type declaration from a Java class.
     *
     * @param descriptor plug-in descriptor
     * @throws InvalidClassException if the given class is not a type declaration
     */
    DSLTypeDeclaration(DSLPluginDescriptor descriptor) {
        super(descriptor);

        Class<?> pluginClass = descriptor.getPluginClass();
        simpleName = Shared.simpleNameOfClass(pluginClass);

        @Nullable ExcludedSuperTypes excludedSuperTypesAnnotation
            = descriptor.getClassWithAnnotation().getAnnotation(ExcludedSuperTypes.class);
        List<Class<?>> excludedSuperClasses = excludedSuperTypesAnnotation == null
            ? Collections.<Class<?>>emptyList()
            : Arrays.asList(excludedSuperTypesAnnotation.value());

        @Nullable Type superclassType = pluginClass.getGenericSuperclass();
        superclass = superclassType != null && !isExcluded(excludedSuperClasses, superclassType)
            ? MutableTypeMirror.fromJavaType(superclassType, DSLNestedNameCopyOption.INSTANCE)
            : null;

        Type[] interfaceTypes = pluginClass.getGenericInterfaces();
        for (Type interfaceType: interfaceTypes) {
            if (!isExcluded(excludedSuperClasses, interfaceType)) {
                interfaces.add(MutableTypeMirror.fromJavaType(interfaceType, DSLNestedNameCopyOption.INSTANCE));
            }
        }

        kind = pluginClass.isInterface()
            ? Kind.INTERFACE
            : Kind.CLASS;

        if (pluginClass.isPrimitive() || pluginClass.isArray() || pluginClass.isAnonymousClass()
                || pluginClass.isLocalClass()) {
            throw new InvalidClassException(String.format(
                "Expected Class object that neither represents a primitive type, an array, an anonymous class, nor a "
                    + "local class. However, got %s.", pluginClass
            ));
        }


        // Returns an array of length 0 if the underlying generic declaration declares no type variables.
        TypeVariable<?>[] typeParameterTypes = pluginClass.getTypeParameters();
        if (typeParameterTypes.length > 0) {
            typeParameters = new ArrayList<>(typeParameterTypes.length);
            for (TypeVariable<?> typeParameterType: typeParameterTypes) {
                typeParameters.add(
                    MutableTypeParameterElement.fromTypeVariable(typeParameterType, DSLNestedNameCopyOption.INSTANCE)
                );
            }
        } else {
            typeParameters = Collections.emptyList();
        }
    }

    private static boolean isExcluded(List<Class<?>> excludedClasses, Type type) {
        Class<?> clazz;
        if (type instanceof Class<?>) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            throw new IllegalStateException(String.format(
                "Expected instance of one of %s, but got %s.", Arrays.asList(Class.class, ParameterizedType.class),
                type
            ));
        }

        return excludedClasses.contains(clazz);
    }

    @Override
    public String toString() {
        return BareTypeDeclaration.Default.toString(this);
    }

    @Override
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public BareTypeMirror getSuperclass() {
        return MutableTypeMirror.copyOfTypeMirror(superclass);
    }

    @Override
    public List<? extends BareTypeMirror> getInterfaces() {
        List<MutableTypeMirror<?>> copiedInterfaces = new ArrayList<>(interfaces.size());
        for (MutableTypeMirror<?> interfaceTypeMirror: interfaces) {
            copiedInterfaces.add(MutableTypeMirror.copyOfTypeMirror(interfaceTypeMirror));
        }
        return copiedInterfaces;
    }

    @Override
    public Kind getTypeDeclarationKind() {
        return kind;
    }

    @Override
    public List<? extends BareTypeParameterElement> getTypeParameters() {
        List<MutableTypeParameterElement> copiedTypeParameters = new ArrayList<>(typeParameters.size());
        for (MutableTypeParameterElement typeParameter: typeParameters) {
            copiedTypeParameters.add(MutableTypeParameterElement.copyOf(typeParameter));
        }
        return copiedTypeParameters;
    }

    @Override
    public List<? extends BareTypeDeclaration> getNestedTypeDeclarations() {
        return Collections.emptyList();
    }
}
