package xyz.cloudkeeper.model.beans.element;

import xyz.cloudkeeper.model.bare.element.BarePackage;
import xyz.cloudkeeper.model.bare.element.BarePluginDeclaration;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "package")
@XmlType(propOrder = { "qualifiedName", "declaredAnnotations", "declarations" })
public final class MutablePackage extends MutableAnnotatedConstruct<MutablePackage> implements BarePackage {
    private static final long serialVersionUID = -6710390715369542139L;

    @Nullable private Name qualifiedName;
    private final ArrayList<MutablePluginDeclaration<?>> declarations = new ArrayList<>();

    public MutablePackage() { }

    private MutablePackage(Package original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        qualifiedName = Name.qualifiedName(original.getName());
    }

    private MutablePackage(BarePackage original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        qualifiedName = original.getQualifiedName();
        for (BarePluginDeclaration declaration: original.getDeclarations()) {
            declarations.add(MutablePluginDeclaration.copyOfPluginDeclaration(declaration, copyOptions));
        }
    }

    public static MutablePackage fromPackage(Package original, CopyOption... copyOptions) {
        return new MutablePackage(original, copyOptions);
    }

    @Nullable
    public static MutablePackage copyOf(@Nullable BarePackage original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutablePackage(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutablePackage other = (MutablePackage) otherObject;
        return Objects.equals(qualifiedName, other.qualifiedName)
            && Objects.equals(declarations, other.declarations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName, declarations);
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    protected MutablePackage self() {
        return this;
    }

    @XmlElement(name = "qualified-name")
    @Override
    @Nullable
    public Name getQualifiedName() {
        return qualifiedName;
    }

    /**
     * Sets the qualified name of this package.
     *
     * @param qualifiedName the qualified name
     * @return this object
     */
    public MutablePackage setQualifiedName(@Nullable Name qualifiedName) {
        this.qualifiedName = qualifiedName;
        return this;
    }

    /**
     * Sets the qualified name of this package.
     *
     * @param qualifiedName the qualified name
     * @return this object
     */
    public MutablePackage setQualifiedName(String qualifiedName) {
        return setQualifiedName(Name.qualifiedName(qualifiedName));
    }

    @XmlElementWrapper(name = "declarations")
    @XmlElementRef
    @Override
    public List<MutablePluginDeclaration<?>> getDeclarations() {
        return declarations;
    }

    /**
     * Sets the list of declarations.
     *
     * @param declarations new list of declarations
     * @return this instance
     *
     * @throws NullPointerException if the new list of declarations is {@code null}
     */
    public MutablePackage setDeclarations(List<MutablePluginDeclaration<?>> declarations) {
        Objects.requireNonNull(declarations);
        List<MutablePluginDeclaration<?>> backup = new ArrayList<>(declarations);
        this.declarations.clear();
        this.declarations.addAll(backup);
        return this;
    }
}
