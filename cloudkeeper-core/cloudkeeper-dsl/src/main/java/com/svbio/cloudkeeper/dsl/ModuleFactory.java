package com.svbio.cloudkeeper.dsl;

import cloudkeeper.annotations.CloudKeeperSerialization;
import com.svbio.cloudkeeper.dsl.exception.DSLException;
import com.svbio.cloudkeeper.dsl.exception.InvalidClassException;
import com.svbio.cloudkeeper.model.api.ModuleConnector;
import com.svbio.cloudkeeper.model.bare.element.BarePackage;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclaration;
import com.svbio.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationEntry;
import com.svbio.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareParentModule;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BareProxyModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationDeclaration;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import com.svbio.cloudkeeper.model.bare.type.BareArrayType;
import com.svbio.cloudkeeper.model.bare.type.BareDeclaredType;
import com.svbio.cloudkeeper.model.bare.type.BareNoType;
import com.svbio.cloudkeeper.model.bare.type.BarePrimitiveType;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirrorVisitor;
import com.svbio.cloudkeeper.model.bare.type.BareTypeVariable;
import com.svbio.cloudkeeper.model.bare.type.BareWildcardType;
import com.svbio.cloudkeeper.model.beans.SystemBundle;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.beans.element.MutablePackage;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;
import com.svbio.cloudkeeper.model.beans.element.annotation.MutableAnnotationTypeDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.serialization.MutableSerializationDeclaration;
import com.svbio.cloudkeeper.model.beans.element.type.MutableTypeDeclaration;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.util.BuildInformation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ModuleFactory {
    private final ProxyClassLoader proxyClassLoader;

    private static final ModuleFactory DEFAULT_FACTORY = new ModuleFactory(ClassLoader.getSystemClassLoader());

    /**
     * Constructs a module factory with the given parent class loader.
     *
     * Since module classes are subclassed dynamically (and loaded by a class loader owned by this instance), it is
     * under some circumstances necessary to specify the proper parent class loader. In particular, if the module
     * classes are loaded dynamically (for instance, through a {@link java.net.URLClassLoader}), then a parent class
     * loader needs to be specified that knows of these dynamically loaded classes). This could, of course, be the
     * {@link java.net.URLClassLoader} instance itself.
     *
     * @param parentClassLoader parent class loader
     * @throws NullPointerException if the argument is null
     */
    public ModuleFactory(ClassLoader parentClassLoader) {
        Objects.requireNonNull(parentClassLoader);
        try {
            proxyClassLoader
                = new ProxyClassLoader(parentClassLoader, Module.class.getDeclaredMethod("getPort", String.class));
        } catch (NoSuchMethodException exception) {
            // If getting the Method reference fails, there is no reason to proceed.
            throw new IllegalStateException(exception);
        }
    }

    /**
     * Returns the default module factory that has the system class loader as parent class loader.
     *
     * <p>The returned module factory is equivalent (barring caches) to a module factory created with
     * {@link #ModuleFactory(ClassLoader)} with {@code ClassLoader.getSystemClassLoader()} as argument.
     */
    public static ModuleFactory getDefault() {
        return DEFAULT_FACTORY;
    }

    /**
     * Returns the proxy {@link Class} instance for the given abstract module class.
     *
     * {@link Class} instances are cached internally, so there is no need for the caller to implement caching.
     *
     * @param moduleClass the abstract module class
     * @return the (dynamically generated) {@link Class} instance that implements public abstract methods.
     * @throws InvalidClassException if the given class is not a valid module class
     */
    @SuppressWarnings("unchecked")
    <T extends Module<?>> Class<? extends T> getProxyClass(Class<T> moduleClass) {
        // Explicitly check that will be able to obey the type bounds
        if (!Module.class.isAssignableFrom(moduleClass)) {
            throw new InvalidClassException(String.format("Expected subclass of %s but got %s.",
                Module.class, moduleClass));
        }

        try {
            proxyClassLoader.defineProxyForClass(moduleClass);
            return (Class<? extends T>) proxyClassLoader.loadClass(
                ProxyClassLoader.getProxyNameFromName(moduleClass.getName())
            );
        } catch (IOException | ClassNotFoundException exception) {
            // Unexpected for the following reasons:
            // - An IOException means that the class file for moduleClass could not be read.
            // - A ClassNotFoundException means that defineProxyForClass() succeeded, so a ClassNotFoundException
            //   exception should never occur here.
            throw new IllegalStateException("Unexpected exception.", exception);
        }
    }

    <T extends Module<?>> T createInternal(
        Class<T> moduleClass,
        ModuleConnector moduleConnector
    ) {
        assert moduleClass != null;

        if (!Module.class.isAssignableFrom(moduleClass)) {
            throw new IllegalArgumentException(String.format(
                "Expected instance of %s, but got %s.", Module.class, moduleClass
            ));
        } else if (moduleConnector != null && !SimpleModule.class.isAssignableFrom(moduleClass)) {
            throw new IllegalArgumentException(String.format(
                "Expected instance of %s because module connector is non-null, but got %s.",
                SimpleModule.class, moduleClass
            ));
        }

        final Class<? extends T> proxyClass = getProxyClass(moduleClass);
        final T instance;
        try {
            ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.set(
                new ModuleCreationArguments(this, null, moduleClass, Locatables.getCallingStackTraceElement(),
                    moduleConnector)
            );
            instance = Instantiator.instantiate(proxyClass);
        } finally {
            ModuleCreationArguments.CHILD_CREATION_THREAD_LOCAL.remove();
        }

        instance.finishConstruction();
        instance.freeze();

        return instance;
    }

    /**
     * Instantiate a module from a module declaration.
     *
     * @param moduleClass abstract module class that contains the module definition
     * @param moduleConnector The module connector to use if {@code moduleClass} is a {@link SimpleModule}.
     * @param <T> type of the abstract class that contains the module definition
     * @return an instance of the module class
     * @throws DSLException if the module is invalid due to incorrect usage of the DSL
     */
    public <T extends Module<T>> T createWithModuleConnector(Class<T> moduleClass, ModuleConnector moduleConnector) {
        Objects.requireNonNull(moduleConnector);

        return createInternal(moduleClass, moduleConnector);
    }

    /**
     * Instantiate a module from a module declaration.
     */
    public <T extends Module<T>> T create(Class<T> moduleClass) {
        return createInternal(moduleClass, null);
    }

    private static <T> Class<T> requirePluginClass(Class<?> pluginClass, Class<T> requiredClass) {
        if (!requiredClass.isAssignableFrom(pluginClass)) {
            throw new InvalidClassException(String.format(
                "Expected subclass of %s, but got %s.", requiredClass, pluginClass
            ));
        }

        @SuppressWarnings("unchecked")
        Class<T> typedClass = (Class<T>) pluginClass;
        return typedClass;
    }

    private <T extends CompositeModule<T>> DSLCompositeModuleDeclaration newCompositeModuleDeclaration(
            Class<?> pluginClass) {
        requirePluginClass(pluginClass, CompositeModule.class);

        @SuppressWarnings("unchecked")
        Class<T> typedClass = (Class<T>) pluginClass;
        return new DSLCompositeModuleDeclaration(typedClass, this);
    }

    private <T extends SimpleModule<T>> DSLSimpleModuleDeclaration newSimpleModuleDeclaration(Class<?> pluginClass) {
        requirePluginClass(pluginClass, SimpleModule.class);

        @SuppressWarnings("unchecked")
        Class<T> typedClass = (Class<T>) pluginClass;
        return new DSLSimpleModuleDeclaration(typedClass, this);
    }

    /**
     * Creates and returns a plug-in declaration from the given class.
     *
     * <p>If the given class object has a class name that starts with {@code cloudkeeper.mixins.}, then the class object
     * is considered to be a mixin class.
     *
     * @param clazz Java class representing the plug-in declaration. This may also be a mixin class for a
     *     serialization or type declaration.
     * @return CloudKeeper plug-in declaration
     */
    public MutablePluginDeclaration<?> loadDeclaration(Class<?> clazz) {
        DSLPluginDescriptor descriptor = DSLPluginDescriptor.forClass(clazz, proxyClassLoader);
        Class<?> pluginClass = descriptor.getPluginClass();
        Class<? extends Annotation> annotationType = descriptor.getAnnotationType();

        if (AnnotationTypePlugin.class.equals(annotationType)) {
            return MutableAnnotationTypeDeclaration.copyOfAnnotationTypeDeclaration(
                new DSLAnnotationTypeDeclaration(requirePluginClass(pluginClass, Annotation.class))
            );
        } else if (CompositeModulePlugin.class.equals(annotationType)) {
            return MutableCompositeModuleDeclaration.copyOfCompositeModuleDeclaration(
                newCompositeModuleDeclaration(pluginClass)
            );
        } else if (SimpleModulePlugin.class.equals(annotationType)) {
            return MutableSimpleModuleDeclaration.copyOfSimpleModuleDeclaration(
                newSimpleModuleDeclaration(pluginClass)
            );
        } else if (TypePlugin.class.equals(annotationType)) {
            return MutableTypeDeclaration.copyOfTypeDeclaration(new DSLTypeDeclaration(descriptor));
        } else if (SerializationPlugin.class.equals(annotationType)) {
            return MutableSerializationDeclaration.copyOfSerializationDeclaration(
                new DSLSerializationDeclaration(descriptor)
            );
        }

        throw new InvalidClassException(String.format("No plug-in type annotation on %s.", pluginClass));
    }

    /**
     * Returns the {@link Class} object for the given name.
     *
     * <p>This method loads the class with the binary name returned by {@link Name#getBinaryName()}.
     *
     * @param name qualified name of the class to be loaded
     * @return the {@link Class} object for the given name
     * @throws ClassNotFoundException if a class with the given name cannot be found
     */
    public Class<?> loadClass(Name name) throws ClassNotFoundException {
        return Class.forName(name.getBinaryName().toString(), false, proxyClassLoader);
    }

    /**
     * Returns the descriptor of a DSL plug-in declaration.
     *
     * @param name binary name of the class that represents a plug-in declaration (this can be a plug-in class or a
     *     mixin class)
     * @return the DSL plug-in descriptor
     * @throws ClassNotFoundException if a class with the given name cannot be found
     */
    public DSLPluginDescriptor loadPluginDescriptor(String name) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(name, false, proxyClassLoader);
        return DSLPluginDescriptor.forClass(clazz, proxyClassLoader);
    }

    /**
     * Implementation of {@link DeclarationProvider#getDeclaration} after it is guaranteed that {@code pluginName} does
     * not refer to a system plugin.
     */
    private MutablePluginDeclaration<?> loadDeclaration(Name name) throws ClassNotFoundException {
        return loadDeclaration(Class.forName(name.getBinaryName().toString(), false, proxyClassLoader));
    }

    /**
     * Creates and returns an immutable bundle that includes all (transitively) referenced declarations of the given
     * module.
     *
     * @param module module whose transitive declarations will be included
     * @return repository that includes all (transitively) referenced declarations of the given module
     * @throws NullPointerException if an argument is null
     * @throws ClassNotFoundException if a reference to a declaration was discovered, for which no corresponding Java
     *    class exists (providing a definition)
     * @throws InvalidClassException if a class defining a (transitively) referenced plug-in does not have an expected
     *     name or the expected annotation ({@link AnnotationTypePlugin}, {@link CompositeModulePlugin}, etc.).
     */
    public <T extends Module<T>> MutableBundle createBundle(Module<T> module) throws ClassNotFoundException {
        Objects.requireNonNull(module);

        MutableBundle systemBundle = SystemBundle.newSystemBundle();
        final Set<Name> topLevelDeclarations = new HashSet<>();
        for (BarePackage systemPackage: systemBundle.getPackages()) {
            @Nullable Name packageName = systemPackage.getQualifiedName();
            assert packageName != null : "Incomplete package found in system bundle.";
            for (BarePluginDeclaration declaration: systemPackage.getDeclarations()) {
                @Nullable SimpleName declarationName = declaration.getSimpleName();
                assert declarationName != null : "Incomplete declaration found in system bundle.";
                topLevelDeclarations.add(packageName.join(declarationName));
            }
        }

        DeclarationProvider declarationProvider = new DeclarationProvider() {
            @Override
            public MutablePluginDeclaration<?> getDeclaration(Name name) throws ClassNotFoundException {
                return topLevelDeclarations.contains(name)
                    ? null
                    : loadDeclaration(name);
            }
        };
        return new MutableBundle()
            .setCreationTime(new Date())
            .setCloudKeeperVersion(BuildInformation.PROJECT_VERSION)
            .setBundleIdentifier(module.toBundleIdentifier())
            .setPackages(getTransitiveClosure(module, declarationProvider));
    }

    static final class TransitiveDeclarationsContext {
        private final Deque<BareModule> moduleQueue;
        private final Deque<Name> declarationQueue;
        private final Deque<BareTypeMirror> portTypeQueue;
        private final BareModuleVisitor<Void, Void> moduleVisitor = new ModuleVisitor();
        private final BareTypeMirrorVisitor<Void, Void> portTypeVisitor = new TypeVisitor();
        private final BareModuleDeclarationVisitor<Void, Void> moduleDeclarationVisitor
            = new ModuleDeclarationVisitor();
        private final BarePluginDeclarationVisitor<Void, Name> declarationVisitor = new DeclarationVisitor();

        TransitiveDeclarationsContext(Collection<? extends BareModule> modules, Collection<Name> declarations,
                Collection<? extends BareTypeMirror> portTypes) {
            moduleQueue = new ArrayDeque<>(modules);
            declarationQueue = new ArrayDeque<>(declarations);
            portTypeQueue = new ArrayDeque<>(portTypes);
        }

        private void consumeQueues() {
            while (!moduleQueue.isEmpty()) {
                BareModule module = moduleQueue.remove();
                scanAnnotations(module.getDeclaredAnnotations());
                module.accept(moduleVisitor, null);
            }
            // Note that a port type can never add a module. Hence moduleQueue.isEmpty() remains an invariant until
            // the end of the outer while-loop.
            while (!portTypeQueue.isEmpty()) {
                BareTypeMirror portType = portTypeQueue.remove();
                portType.accept(portTypeVisitor, null);
            }
        }

        boolean hasDeclaration() {
            consumeQueues();
            return !declarationQueue.isEmpty();
        }

        Name removeDeclarationName() {
            consumeQueues();
            return declarationQueue.remove();
        }

        void addDeclaration(BarePluginDeclaration declaration, Name qualifiedName) {
            scanAnnotations(declaration.getDeclaredAnnotations());
            declaration.accept(declarationVisitor, qualifiedName);
        }

        void scanAnnotations(List<? extends BareAnnotation> annotations) {
            if (annotations != null) {
                for (BareAnnotation annotation: annotations) {
                    declarationQueue.add(annotation.getDeclaration().getQualifiedName());
                    if (CloudKeeperSerialization.class.getName()
                            .equals(annotation.getDeclaration().getQualifiedName().toString())) {
                        for (BareAnnotationEntry element: annotation.getEntries()) {
                            if ("value".equals(element.getKey().getSimpleName().toString())) {
                                for (String reference: (String[]) element.getValue().toNativeValue()) {
                                    declarationQueue.add(Name.qualifiedName(reference));
                                }
                            }
                        }
                    }
                }
            }
        }

        static void addToQueue(Deque<Name> queue, BareQualifiedNameable element) {
            if (element != null && element.getQualifiedName() != null) {
                queue.add(element.getQualifiedName());
            }
        }

        static <T> void addToQueue(Deque<? super T> queue, T element) {
            if (element != null) {
                queue.add(element);
            }
        }

        static <T> void addAllToQueue(Deque<? super T> queue, Collection<T> collection) {
            if (collection != null) {
                queue.addAll(collection);
            }
        }

        final class ModuleVisitor implements BareModuleVisitor<Void, Void> {
            private void addPorts(BareParentModule parentModule) {
                for (BarePort port: parentModule.getDeclaredPorts()) {
                    scanAnnotations(port.getDeclaredAnnotations());
                    portTypeQueue.add(port.getType());
                }
            }

            @Override
            public Void visitInputModule(BareInputModule module, Void ignored) {
                portTypeQueue.add(module.getOutPortType());
                return null;
            }

            @Override
            public Void visitCompositeModule(BareCompositeModule module, Void ignored) {
                addPorts(module);
                moduleQueue.addAll(module.getModules());
                return null;
            }

            @Override
            public Void visitLoopModule(BareLoopModule module, Void ignored) {
                addPorts(module);
                addAllToQueue(moduleQueue, module.getModules());
                return null;
            }

            @Override
            public Void visitLinkedModule(BareProxyModule module, Void ignored) {
                // Port types will be added when declaration is added
                addToQueue(declarationQueue, module.getDeclaration().getQualifiedName());
                return null;
            }
        }

        final class ModuleDeclarationVisitor implements BareModuleDeclarationVisitor<Void, Void> {
            @Override
            public Void visit(BareCompositeModuleDeclaration declaration, Void ignored) {
                addToQueue(moduleQueue, declaration.getTemplate());
                return null;
            }

            @Override
            public Void visit(BareSimpleModuleDeclaration declaration, Void ignored) {
                for (BarePort port: declaration.getPorts()) {
                    addToQueue(portTypeQueue, port.getType());
                }
                return null;
            }
        }

        final class TypeVisitor implements BareTypeMirrorVisitor<Void, Void> {
            @Override
            public Void visitArrayType(BareArrayType arrayType, Void parameter) {
                addToQueue(portTypeQueue, arrayType.getComponentType());
                return null;
            }

            @Override
            public Void visitDeclaredType(BareDeclaredType declaredType, Void parameter) {
                addToQueue(declarationQueue, declaredType.getDeclaration());
                addAllToQueue(portTypeQueue, declaredType.getTypeArguments());
                return null;
            }

            @Override
            public Void visitPrimitive(BarePrimitiveType primitiveType, Void parameter) {
                // Nothing to do here.
                return null;
            }

            @Override
            public Void visitTypeVariable(BareTypeVariable typeVariable, Void parameter) {
                // Nothing to do here. The formal type parameter is dealt with by some enclosing declaration
                return null;
            }

            @Override
            public Void visitWildcardType(BareWildcardType wildcardType, Void parameter) {
                addToQueue(portTypeQueue, wildcardType.getExtendsBound());
                addToQueue(portTypeQueue, wildcardType.getSuperBound());
                return null;
            }

            @Override
            public Void visitNoType(BareNoType noType, Void parameter) {
                // Nothing to do here.
                return null;
            }

            @Override
            public Void visitOther(BareTypeMirror type, Void parameter) {
                // Nothing to do here.
                return null;
            }
        }

        final class DeclarationVisitor implements BarePluginDeclarationVisitor<Void, Name> {
            @Override
            public Void visit(BareModuleDeclaration declaration, Name ignored) {
                declaration.accept(moduleDeclarationVisitor, null);
                return null;
            }

            @Override
            public Void visit(BareTypeDeclaration declaration, Name qualifiedName) {
                addToQueue(portTypeQueue, declaration.getSuperclass());
                addAllToQueue(portTypeQueue, declaration.getInterfaces());

                List<? extends BareTypeParameterElement> typeParameters = declaration.getTypeParameters();
                if (typeParameters != null) {
                    for (BareTypeParameterElement typeParameter: typeParameters) {
                        addAllToQueue(portTypeQueue, typeParameter.getBounds());
                    }
                }

                return null;
            }

            @Override
            public Void visit(BareAnnotationTypeDeclaration declaration, Name ignored) {
                // Currently, an annotation type cannot reference other declarations.
                return null;
            }

            @Override
            public Void visit(BareSerializationDeclaration declaration, Name ignored) {
                // Currently, a serialization declaration cannot reference other declarations.
                return null;
            }
        }
    }

    interface DeclarationProvider {
        /**
         * Returns the plug-in declaration that provides the given fully qualified name, or {@code null} if the given
         * name should be ignored.
         *
         * <p>Note that if {@code name} is {@code A.B} and {@code B} is a nested declaration in {@code A}, this
         * method returns a plug-in declaration with name {@code B}.
         *
         * @param name fully qualified name of (possibly nested) plug-in declaration
         * @return the top-level plug-in declaration
         * @throws ClassNotFoundException if the qualified name cannot be ignored and a class with the given name
         *     could not be found
         * @throws InvalidClassException
         */
        @Nullable
        MutablePluginDeclaration<?> getDeclaration(Name name) throws ClassNotFoundException;
    }

    /**
     * Returns a set of plug-in declarations that contains all plug-in declaration transitively referenced by the given
     * module.
     */
    private static List<MutablePackage> getTransitiveClosure(BareModule module,
            DeclarationProvider provider) throws ClassNotFoundException {
        TransitiveDeclarationsContext context = new TransitiveDeclarationsContext(
            Collections.singleton(module),
            Collections.<Name>emptyList(),
            Collections.<BareTypeMirror>emptyList()
        );
        Set<Name> visitedDeclarationNames = new HashSet<>();
        List<MutablePackage> packages = new ArrayList<>();
        Map<Name, MutablePackage> packageMap = new HashMap<>();

        while (context.hasDeclaration()) {
            Name qualifiedName = context.removeDeclarationName();
            Name packageName = qualifiedName.getPackageName();
            Name topLevelName
                = qualifiedName.getPackageName().join(qualifiedName.asList().get(packageName.asList().size()));
            if (!visitedDeclarationNames.contains(topLevelName)) {
                @Nullable MutablePluginDeclaration<?> topLevelDeclaration = provider.getDeclaration(topLevelName);
                if (topLevelDeclaration == null) {
                    continue;
                }

                @Nullable MutablePackage thePackage = packageMap.get(packageName);
                if (thePackage == null) {
                    thePackage = new MutablePackage().setQualifiedName(packageName);
                    packages.add(thePackage);
                    packageMap.put(packageName, thePackage);
                }

                thePackage.getDeclarations().add(topLevelDeclaration);
                visitedDeclarationNames.add(topLevelName);
                // need to scan transitive references of new declaration
                context.addDeclaration(topLevelDeclaration, topLevelName);
            }
        }
        return packages;
    }
}
