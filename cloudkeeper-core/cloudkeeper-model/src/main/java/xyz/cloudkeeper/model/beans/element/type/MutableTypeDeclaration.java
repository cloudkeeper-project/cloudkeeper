package xyz.cloudkeeper.model.beans.element.type;

import xyz.cloudkeeper.model.bare.element.BarePluginDeclarationVisitor;
import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import xyz.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.IncludeNestedCopyOption;
import xyz.cloudkeeper.model.beans.element.MutablePluginDeclaration;
import xyz.cloudkeeper.model.beans.type.MutableTypeMirror;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "type-declaration")
@XmlType(propOrder = { "typeDeclarationKind", "superclass", "interfaces", "typeParameters", "nestedTypeDeclarations" })
public final class MutableTypeDeclaration
        extends MutablePluginDeclaration<MutableTypeDeclaration>
        implements BareTypeDeclaration {
    private static final long serialVersionUID = 5124255998141402494L;

    @Nullable private MutableTypeMirror<?> superclass;
    private final ArrayList<MutableTypeMirror<?>> interfaces = new ArrayList<>();
    private Kind kind = Kind.CLASS;
    private final ArrayList<MutableTypeParameterElement> typeParameters = new ArrayList<>();
    private final ArrayList<MutableTypeDeclaration> nestedTypeDeclarations = new ArrayList<>();

    public MutableTypeDeclaration() { }

    /**
     * Copy constructor from a native Java class/interface.
     *
     * <p>This constructor also copies Java annotations. See
     * {@link xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct#MutableAnnotatedConstruct(java.lang.reflect.AnnotatedElement, CopyOption[])}
     * for details.
     *
     * @param original native Java type
     */
    private MutableTypeDeclaration(Class<?> original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        Objects.requireNonNull(original);

        if (original.isPrimitive() || original.isArray() || original.isAnonymousClass() || original.isLocalClass()) {
            throw new IllegalArgumentException(String.format(
                "Expected Class object that neither represents a primitive type, an array, an anonymous class, nor a "
                    + "local class. However, got %s.", original
            ));
        }

        @Nullable Type originalSuperclass = original.getGenericSuperclass();
        // If originalSuperclass represents the Object class or an interface, then originalSuperclass == null.
        superclass = originalSuperclass == null
            ? null
            : MutableTypeMirror.fromJavaType(originalSuperclass, copyOptions);

        for (Type originalInterface: original.getGenericInterfaces()) {
            interfaces.add(MutableTypeMirror.fromJavaType(originalInterface, copyOptions));
        }

        kind = original.isInterface()
            ? Kind.INTERFACE
            : Kind.CLASS;

        for (TypeVariable<?> typeParameter: original.getTypeParameters()) {
            typeParameters.add(MutableTypeParameterElement.fromTypeVariable(typeParameter, copyOptions));
        }

        Class<?>[] originalNestedClasses = original.getDeclaredClasses();
        for (Class<?> nestedClass: originalNestedClasses) {
            // Do not include classes that are anonymous or local
            if (shouldIncludeNested(nestedClass, copyOptions)
                    && !nestedClass.isAnonymousClass() && !nestedClass.isLocalClass()) {
                nestedTypeDeclarations.add(new MutableTypeDeclaration(nestedClass, copyOptions));
            }
        }
    }

    public static MutableTypeDeclaration fromClass(Class<?> original, CopyOption... copyOptions) {
        return new MutableTypeDeclaration(original, copyOptions);
    }

    private MutableTypeDeclaration(BareTypeDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        superclass = MutableTypeMirror.copyOfTypeMirror(original.getSuperclass(), copyOptions);

        for (BareTypeMirror interfaceType: original.getInterfaces()) {
            interfaces.add(MutableTypeMirror.copyOfTypeMirror(interfaceType, copyOptions));
        }

        kind = Objects.requireNonNull(original.getTypeDeclarationKind());

        for (BareTypeParameterElement typeParameter: original.getTypeParameters()) {
            typeParameters.add(MutableTypeParameterElement.copyOf(typeParameter, copyOptions));
        }

        for (BareTypeDeclaration nestedTypeDeclaration: original.getNestedTypeDeclarations()) {
            nestedTypeDeclarations.add(new MutableTypeDeclaration(nestedTypeDeclaration, copyOptions));
        }
    }

    @Nullable
    public static MutableTypeDeclaration copyOfTypeDeclaration(@Nullable BareTypeDeclaration original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableTypeDeclaration(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        MutableTypeDeclaration other = (MutableTypeDeclaration) otherObject;
        return Objects.equals(superclass, other.superclass)
            && Objects.equals(interfaces, other.interfaces)
            && Objects.equals(kind, other.kind)
            && Objects.equals(typeParameters, other.typeParameters)
            && Objects.equals(nestedTypeDeclarations, other.nestedTypeDeclarations);
    }

    @Override
    protected MutableTypeDeclaration self() {
        return this;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode()
            + Objects.hash(superclass, interfaces, kind, typeParameters, nestedTypeDeclarations);
    }

    @Override
    public String toString() {
        return BareTypeDeclaration.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BarePluginDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @XmlElementRef
    @Override
    @Nullable
    public MutableTypeMirror<?> getSuperclass() {
        return superclass;
    }

    public MutableTypeDeclaration setSuperclass(@Nullable MutableTypeMirror<?> superclass) {
        this.superclass = superclass;
        return this;
    }

    @XmlElement(name = "kind")
    @Override
    public Kind getTypeDeclarationKind() {
        return kind;
    }

    /**
     * Sets the concrete kind of this type declaration.
     *
     * @param kind concrete kind of this type declaration
     * @return this type declaration
     */
    public MutableTypeDeclaration setTypeDeclarationKind(Kind kind) {
        Objects.requireNonNull(kind);
        this.kind = kind;
        return this;
    }

    @XmlElementWrapper(name = "type-parameters")
    @XmlElement(name = "type-parameter")
    @Override
    public List<MutableTypeParameterElement> getTypeParameters() {
        return typeParameters;
    }

    public MutableTypeDeclaration setTypeParameters(List<MutableTypeParameterElement> typeParameters) {
        Objects.requireNonNull(typeParameters);
        List<MutableTypeParameterElement> backup = new ArrayList<>(typeParameters);
        this.typeParameters.clear();
        this.typeParameters.addAll(backup);
        return this;
    }

    @XmlElementWrapper(name = "interfaces")
    @XmlElementRef
    @Override
    public List<MutableTypeMirror<?>> getInterfaces() {
        return interfaces;
    }

    public MutableTypeDeclaration setInterfaces(List<MutableTypeMirror<?>> interfaces) {
        Objects.requireNonNull(interfaces);
        List<MutableTypeMirror<?>> backup = new ArrayList<>(interfaces);
        this.interfaces.clear();
        this.interfaces.addAll(backup);
        return this;
    }

    @XmlElementWrapper(name = "nested-type-declarations")
    @XmlElementRef
    @Override
    public List<MutableTypeDeclaration> getNestedTypeDeclarations() {
        return nestedTypeDeclarations;
    }

    public MutableTypeDeclaration setNestedTypeDeclarations(List<MutableTypeDeclaration> nestedTypeDeclarations) {
        Objects.requireNonNull(nestedTypeDeclarations);
        List<MutableTypeDeclaration> backup = new ArrayList<>(nestedTypeDeclarations);
        this.nestedTypeDeclarations.clear();
        this.nestedTypeDeclarations.addAll(backup);
        return this;
    }

    /**
     * Returns whether the CloudKeeper plug-in declaration corresponding to the given nested class should be included
     * in the CloudKeeper model.
     *
     * @param clazz nested class
     * @param copyOptions copy options
     * @return whether the given nested class should be included in the CloudKeeper model
     *
     * @see IncludeNestedCopyOption#shouldInclude(Class)
     */
    static boolean shouldIncludeNested(Class<?> clazz, CopyOption[] copyOptions) {
        for (CopyOption copyOption: copyOptions) {
            if (copyOption instanceof IncludeNestedCopyOption) {
                return ((IncludeNestedCopyOption) copyOption).shouldInclude(clazz);
            }
        }
        return Modifier.isPublic(clazz.getModifiers());
    }
}
