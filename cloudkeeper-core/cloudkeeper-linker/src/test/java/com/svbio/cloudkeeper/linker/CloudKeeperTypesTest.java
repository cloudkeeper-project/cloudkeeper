package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.beans.IncludeNestedCopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.beans.element.MutablePackage;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration;
import net.florianschoppmann.java.type.AbstractTypes;
import net.florianschoppmann.java.type.AbstractTypesContract;
import net.florianschoppmann.java.type.AbstractTypesProvider;
import org.testng.Assert;
import org.testng.annotations.Factory;

import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CloudKeeperTypesTest {
    @Factory
    public Object[] createTests() {
        Provider provider = new Provider();
        return new Object[] {
            new AbstractTypesContract(provider)
        };
    }

    private enum CopyOptions implements IncludeNestedCopyOption {
        INSTANCE;

        @Override
        public boolean shouldInclude(Class<?> clazz) {
            return true;
        }
    }

    private static final class Provider implements AbstractTypesProvider {
        @Override
        public void preContract() { }

        private static MutablePackage getOrCreatePackage(Package javaPackage, Map<Name, MutablePackage> packageMap) {
            Name qualifiedName = Name.qualifiedName(javaPackage.getName());
            MutablePackage thePackage = packageMap.get(qualifiedName);
            if (thePackage == null) {
                thePackage = MutablePackage.fromPackage(javaPackage);
                packageMap.put(qualifiedName, thePackage);
            }
            return thePackage;
        }

        /**
         * Visits a {@link Type} and adds type declarations for all transitively referenced {@link Class} objects.
         *
         * @param type the Java reflection type; may be null, in which case this method is a no-op
         * @param visitedElements {@link Class} or {@link TypeVariable} objects that have already been visited; visiting
         *     an element the second time is a no-op
         * @param systemRepository the linked CloudKeeper system repository
         * @param packageMap map of packages that this method will append to
         */
        private static void visitType(Type type, Set<Type> visitedElements, RuntimeRepository systemRepository,
                Map<Name, MutablePackage> packageMap) {
            if (type == null || visitedElements.contains(type)) {
                return;
            }

            if (type instanceof Class<?>) {
                Class<?> clazz = (Class<?>) type;
                visitedElements.add(clazz);

                if (clazz.isArray()) {
                    visitType(clazz.getComponentType(), visitedElements, systemRepository, packageMap);
                } else if (systemRepository.getElement(
                        RuntimeTypeDeclaration.class, Name.qualifiedName(clazz.getCanonicalName())) == null) {
                    // Only create type declaration if is is not already contained in the system repository.
                    if (clazz.isMemberClass()) {
                        visitType(clazz.getEnclosingClass(), visitedElements, systemRepository, packageMap);
                    } else {
                        getOrCreatePackage(clazz.getPackage(), packageMap)
                            .getDeclarations()
                            .add(MutableTypeDeclaration.fromClass(clazz, CopyOptions.INSTANCE));
                    }
                    visitType(clazz.getGenericSuperclass(), visitedElements, systemRepository, packageMap);
                    for (Type interfaceType: clazz.getGenericInterfaces()) {
                        visitType(interfaceType, visitedElements, systemRepository, packageMap);
                    }
                    for (Class<?> nestedClass: clazz.getDeclaredClasses()) {
                        if (!nestedClass.isAnonymousClass() && !nestedClass.isLocalClass()) {
                            visitType(nestedClass, visitedElements, systemRepository, packageMap);
                        }
                    }
                }
            } else if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                visitType(parameterizedType.getRawType(), visitedElements, systemRepository, packageMap);
                visitType(parameterizedType.getOwnerType(), visitedElements, systemRepository, packageMap);
                for (Type argumentType: parameterizedType.getActualTypeArguments()) {
                    visitType(argumentType, visitedElements, systemRepository, packageMap);
                }
            } else if (type instanceof TypeVariable<?>) {
                TypeVariable<?> typeVariable = (TypeVariable<?>) type;
                visitedElements.add(typeVariable);
                GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                Assert.assertTrue(genericDeclaration instanceof Class<?>,
                    "No support for constructors or methods as generic declarations.");
                visitType((Type) genericDeclaration, visitedElements, systemRepository, packageMap);
                for (Type boundType: typeVariable.getBounds()) {
                    visitType(boundType, visitedElements, systemRepository, packageMap);
                }
            } else if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) type;
                for (Type boundType: wildcardType.getLowerBounds()) {
                    visitType(boundType, visitedElements, systemRepository, packageMap);
                }
                for (Type boundType: wildcardType.getUpperBounds()) {
                    visitType(boundType, visitedElements, systemRepository, packageMap);
                }
            } else if (type instanceof GenericArrayType) {
                GenericArrayType arrayType = (GenericArrayType) type;
                visitType(arrayType.getGenericComponentType(), visitedElements, systemRepository, packageMap);
            } else {
                Assert.fail(String.format("Unexpected type: %s", type));
            }
        }

        @Override
        public AbstractTypes getTypes(Map<Class<?>, TypeElement> classTypeElementMap) {
            try {
                LinkerImpl linker = LinkerImpl.getInstance();
                RepositoryImpl systemRepository = linker.createRepository(
                    Collections.<BareBundle>emptyList(), LinkerOptions.nonExecutable());

                Set<Type> visitedElements = new HashSet<>();
                Map<Name, MutablePackage> packageMap = new LinkedHashMap<>();
                for (Class<?> clazz: classTypeElementMap.keySet()) {
                    visitType(clazz, visitedElements, systemRepository, packageMap);
                }

                MutableBundle mutableRepository = new MutableBundle()
                    .setBundleIdentifier(URI.create("x-test:" + getClass().getName()))
                    .setPackages(new ArrayList<>(packageMap.values()));
                RepositoryImpl repository = linker.createRepository(
                    Collections.singletonList(mutableRepository), LinkerOptions.nonExecutable());
                for (Map.Entry<Class<?>, TypeElement> entry: classTypeElementMap.entrySet()) {
                    @Nullable TypeElement typeElement = repository.getElement(
                        TypeDeclarationImpl.class, Name.qualifiedName(entry.getKey().getCanonicalName()));
                    Assert.assertNotNull(typeElement, String.format("Missing type element for %s.", entry.getKey()));
                    entry.setValue(typeElement);
                }

                return linker.getTypes();
            } catch (LinkerException exception) {
                Assert.fail("Test setup failed.", exception);
                return null;
            }
        }
    }
}
