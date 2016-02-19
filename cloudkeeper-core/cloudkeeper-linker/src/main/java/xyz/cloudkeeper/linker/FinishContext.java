package xyz.cloudkeeper.linker;

import cloudkeeper.annotations.CloudKeeperElementReference;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.NotFoundException;
import xyz.cloudkeeper.model.PreprocessingException;
import xyz.cloudkeeper.model.api.Executable;
import xyz.cloudkeeper.model.api.RuntimeStateProvisionException;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotationTypeElement;
import xyz.cloudkeeper.model.bare.element.module.BareInPort;
import xyz.cloudkeeper.model.bare.element.module.BareModule;
import xyz.cloudkeeper.model.bare.element.module.BareOutPort;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.type.TypeKind;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

final class FinishContext {
    private static final Name CLOUD_KEEPER_ELEMENT_REFERENCE_NAME
        = Name.qualifiedName(CloudKeeperElementReference.class.getName());
    private static final Name OBJECT_NAME = Name.qualifiedName(Object.class.getName());
    private final LinkerImpl linker;
    @Nullable private final FinishContext parentContext;

    /**
     * The {@link AbstractFreezable} <em>enclosing</em> the one on which {@link AbstractFreezable#finish} is called with
     * this context as argument. May be null if and only if {@link #parentContext} is null.
     */
    @Nullable private final AbstractFreezable freezable;
    private final ElementResolver elementResolver;
    private final LinkerOptions linkerOptions;

    FinishContext(LinkerImpl linker, ElementResolver elementResolver, LinkerOptions linkerOptions) {
        this(linker, null, null, elementResolver, linkerOptions);
    }

    private FinishContext(LinkerImpl linker, @Nullable FinishContext parentContext,
            @Nullable AbstractFreezable freezable, ElementResolver elementResolver, LinkerOptions linkerOptions) {
        assert (parentContext == null) == (freezable == null);

        this.linker = linker;
        this.parentContext = parentContext;
        this.freezable = freezable;
        this.elementResolver = elementResolver;
        this.linkerOptions = linkerOptions;
    }

    FinishContext newChildContext(AbstractFreezable childFreezable) {
        return new FinishContext(linker, this, childFreezable, elementResolver, linkerOptions);
    }

    /**
     * Returns the innermost enclosing {@link AbstractFreezable} instance of the given type, or {@code null} if there is
     * no such enclosing object and {@code required} is {@code false}.
     *
     * @param clazz Java class of the enclosing {@link AbstractFreezable} instance that is requested
     * @param <T> type of the enclosing object that is requested
     * @return the enclosing element, or {@code null} if there is no such enclosing element and {@code required} is
     *     {@code false}
     * @throws IllegalStateException if {@code required} is true, and no enclosing element of the specified type exists
     */
    <T> Optional<T> getOptionalEnclosingFreezable(Class<T> clazz) {
        @Nullable Object currentEnclosingElement;
        @Nullable FinishContext currentContext = this;
        do {
            currentEnclosingElement = currentContext.freezable;
            if (currentEnclosingElement != null && clazz.isInstance(currentEnclosingElement)) {
                @SuppressWarnings("unchecked")
                T returnValue = (T) currentEnclosingElement;
                return Optional.of(returnValue);
            }
            currentContext = currentContext.parentContext;
        } while (currentContext != null);
        return Optional.empty();
    }

    <T> T getRequiredEnclosingFreezable(Class<T> clazz) {
        return getOptionalEnclosingFreezable(clazz)
            .orElseThrow(() -> new IllegalStateException(String.format("No enclosing freezable of %s.", clazz)));
    }

    /**
     * Returns the type parameter corresponding to the given simple name.
     *
     * <p>This method checks iterates through all parent contexts, starting from the current context. It stops when a
     * context corresponds to a {@link IParameterizableImpl} instance that contains a type parameter with the
     * given name.
     *
     * @param reference simple-name reference to the type parameter
     * @return the type parameter corresponding to the given simple name, or {@code null} if there is no such type
     *     parameter
     * @throws LinkerException
     */
    TypeParameterElementImpl getTypeParameter(SimpleNameReference reference) throws LinkerException {
        @Nullable FinishContext currentContext = this;
        do {
            @Nullable AbstractFreezable currentFreezable = currentContext.freezable;
            if (currentFreezable instanceof IParameterizableImpl) {
                IParameterizableImpl currentParameterizable = (IParameterizableImpl) currentFreezable;
                for (TypeParameterElementImpl typeParameter: currentParameterizable.getTypeParameters()) {
                    if (typeParameter.getSimpleName().contentEquals(reference.getSimpleName())) {
                        return typeParameter;
                    }
                }
            }
            currentContext = currentContext.parentContext;
        } while (currentContext != null);
        return null;
    }

    CloudKeeperTypeReflection getTypes() {
        return linker.getTypes();
    }

    ImmutableList<SerializationDeclarationImpl> getDefaultSerializationDeclarations() {
        return linker.getDefaultSerializationDeclarations();
    }

    private static void requireElement(String kind, @Nullable RuntimeElement element, SimpleNameReference reference)
            throws NotFoundException {
        if (element == null) {
            throw new NotFoundException(kind, reference.getSimpleName().toString(),
                reference.getCopyContext().toLinkerTrace());
        }
    }

    private static void requireElement(String kind, @Nullable RuntimeElement element, NameReference reference)
            throws NotFoundException {
        if (element == null) {
            throw new NotFoundException(kind, reference.getQualifiedName().toString(),
                reference.getCopyContext().toLinkerTrace());
        }
    }

    AnnotationTypeElementImpl getAnnotationTypeElement(SimpleNameReference reference) throws LinkerException {
        AnnotationTypeDeclarationImpl annotationType
            = getRequiredEnclosingFreezable(AnnotationImpl.class).getDeclaration();
        @Nullable AnnotationTypeElementImpl element
            = annotationType.getEnclosedElement(AnnotationTypeElementImpl.class, reference.getSimpleName());
        requireElement(BareAnnotationTypeElement.NAME, element, reference);
        return element;
    }

    IElementImpl getElement(AnnotationValueImpl annotationValue) throws LinkerException {
        AnnotationEntryImpl enclosingEntry = getRequiredEnclosingFreezable(AnnotationEntryImpl.class);
        if (enclosingEntry.getKey().getDeclaredAnnotation(CLOUD_KEEPER_ELEMENT_REFERENCE_NAME) != null) {
            Object value = annotationValue.getValue();
            Preconditions.requireCondition(value instanceof List<?> || value instanceof String,
                annotationValue.getCopyContext(),
                "@%s annotation can only be used on annotation type elements returning String or String[].",
                CloudKeeperElementReference.class.getName()
            );

            if (value instanceof String) {
                @Nullable IElementImpl element
                    = elementResolver.getElement(IElementImpl.class, Name.qualifiedName((String) value));
                if (element == null) {
                    throw new PreprocessingException(
                        String.format("Could not find element with name '%s'.", value),
                        annotationValue.getCopyContext().toLinkerTrace()
                    );
                }
                return element;
            }
        }
        return null;
    }

    /**
     * Returns the {@link Class} object corresponding to the given plugin declaration.
     *
     * <p>The class with name equal to the plugin declaration's name must be instantiable with a public no-argument
     * constructor. Otherwise, a {@link PreprocessingException} is thrown.
     *
     * @param pluginDeclaration class name
     * @param superClass super class, needs to be assignable from the newly loaded class
     * @param <T> type of class to be instantiated
     * @return the {@link Class} object for the given name
     * @throws PreprocessingException if a class with the given name could not be loaded (in which case
     *     {@link PreprocessingException#getCause()} will contain a {@link ClassNotFoundException}) or if
     *     {@code superClass} is not assignable from the loaded class.
     */
    @Nullable
    <T> Class<? extends T> resolveJavaClass(PluginDeclarationImpl pluginDeclaration, Class<T> superClass)
            throws PreprocessingException {
        try {
            Optional<Class<?>> optionalClass
                = linkerOptions.getClassProvider().provideClass(pluginDeclaration.getQualifiedName());
            if (!optionalClass.isPresent()) {
                return null;
            }

            Class<?> untypedClass = optionalClass.get();
            if (!superClass.isAssignableFrom(untypedClass)) {
                throw new PreprocessingException(String.format(
                    "Expected subclass of %s, but got %s.", superClass, untypedClass
                ), pluginDeclaration.getCopyContext().toLinkerTrace());
            }

            @SuppressWarnings("unchecked")
            Class<? extends T> typeClass = (Class<? extends T>) untypedClass;
            return typeClass;
        } catch (ClassNotFoundException | RuntimeStateProvisionException exception) {
            throw new PreprocessingException(String.format(
                "Could not resolve class for plug-in declaration '%s'.", pluginDeclaration.getQualifiedName()
            ), exception, pluginDeclaration.getCopyContext().toLinkerTrace());
        }
    }

    @Nullable
    <T> T instanceOfJavaClass(PluginDeclarationImpl pluginDeclaration, Class<T> superClass)
            throws PreprocessingException {
        @Nullable Class<? extends T> clazz = resolveJavaClass(pluginDeclaration, superClass);
        if (clazz == null) {
            return null;
        }

        if (clazz.isAnonymousClass() || clazz.isLocalClass()) {
            throw new PreprocessingException(String.format(
                "Expected non-anonymous, non-local, non-enum class with modifier public. "
                    + "If class is a member class, also expected modifier static. However, got %s.", clazz
            ), pluginDeclaration.getCopyContext().toLinkerTrace());
        }

        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException
                exception) {
            throw new PreprocessingException(String.format("Could not instantiate %s.", clazz), exception,
                pluginDeclaration.getCopyContext().toLinkerTrace());
        }
    }

    <T extends PluginDeclarationImpl> T getDeclaration(String kind, Class<T> clazz, NameReference reference)
            throws LinkerException {
        @Nullable T declaration = elementResolver.getElement(clazz, reference.getQualifiedName());
        requireElement(kind, declaration, reference);
        return declaration;
    }

    DeclaredTypeImpl getObjectType() {
        @Nullable TypeDeclarationImpl objectDeclaration
            = elementResolver.getElement(TypeDeclarationImpl.class, OBJECT_NAME);
        if (objectDeclaration == null) {
            throw new IllegalStateException(String.format(
                "Could not find required CloudKeeper type declaration for %s.", Object.class.getName()
            ));
        }
        return new DeclaredTypeImpl(linker.getTypes(), new NoTypeImpl(linker.getTypes(), TypeKind.NONE),
            objectDeclaration, ImmutableList.<TypeMirrorImpl>of());
    }

    IInPortImpl getParentInPort(SimpleNameReference fromPortReference) throws NotFoundException {
        ParentModuleImpl parentModule = getRequiredEnclosingFreezable(ParentModuleImpl.class);
        @Nullable IInPortImpl inPort
            = parentModule.getEnclosedElement(IInPortImpl.class, fromPortReference.getSimpleName());
        requireElement(BareInPort.NAME, inPort, fromPortReference);
        return inPort;
    }

    IOutPortImpl getParentOutPort(SimpleNameReference toPortReference) throws NotFoundException {
        ParentModuleImpl parentModule = getRequiredEnclosingFreezable(ParentModuleImpl.class);
        @Nullable IOutPortImpl outPort
            = parentModule.getEnclosedElement(IOutPortImpl.class, toPortReference.getSimpleName());
        requireElement(BareOutPort.NAME, outPort, toPortReference);
        return outPort;
    }

    IOutPortImpl getChildOutPort(SimpleNameReference fromModuleReference, SimpleNameReference fromPortReference)
            throws NotFoundException {
        ParentModuleImpl parentModule = getRequiredEnclosingFreezable(ParentModuleImpl.class);
        @Nullable ModuleImpl childModule
            = parentModule.getEnclosedElement(ModuleImpl.class, fromModuleReference.getSimpleName());
        requireElement(BareModule.NAME, childModule, fromModuleReference);
        @Nullable IOutPortImpl outPort
            = childModule.getEnclosedElement(IOutPortImpl.class, fromPortReference.getSimpleName());
        requireElement(BareOutPort.NAME, outPort, fromPortReference);
        return outPort;
    }

    IInPortImpl getChildInPort(SimpleNameReference toModuleReference, SimpleNameReference toPortReference)
            throws NotFoundException {
        ParentModuleImpl parentModule = getRequiredEnclosingFreezable(ParentModuleImpl.class);
        @Nullable ModuleImpl childModule
            = parentModule.getEnclosedElement(ModuleImpl.class, toModuleReference.getSimpleName());
        requireElement(BareModule.NAME, childModule, toModuleReference);
        @Nullable IInPortImpl inPort
            = childModule.getEnclosedElement(IInPortImpl.class, toPortReference.getSimpleName());
        requireElement(BareInPort.NAME, inPort, toPortReference);
        return inPort;
    }

    IElementImpl resolveElement(NameReference elementReference) throws LinkerException {
        @Nullable IElementImpl element
            = elementResolver.getElement(IElementImpl.class, elementReference.getQualifiedName());
        requireElement("element", element, elementReference);
        return element;
    }

    @Nullable
    Executable getExecutable(SimpleModuleDeclarationImpl declaration) throws LinkerException {
        try {
            return linkerOptions.getExecutableProvider().provideExecutable(declaration.getQualifiedName()).orElse(null);
        } catch (RuntimeStateProvisionException exception) {
            throw new LinkerException(String.format(
                "Could not resolve %s for simple-module declaration '%s'.",
                Executable.class.getSimpleName(), declaration.getQualifiedName()
            ), exception, declaration.getCopyContext().toLinkerTrace());
        }
    }
}
