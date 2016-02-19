package xyz.cloudkeeper.model.beans;

import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Abstract base class of all bean-style classes in the package and subpackages.
 *
 * <p>This class provides a property {@code location} that contains the source-code location of this object, or
 * {@code null} if not available.
 */
@XmlTransient
public abstract class MutableLocatable<D extends MutableLocatable<D>> implements BareLocatable, Serializable {
    private static final long serialVersionUID = -8068367861372683749L;

    @Nullable private Location location;

    protected MutableLocatable() { }

    protected MutableLocatable(BareLocatable original, CopyOption[] copyOptions) {
        Objects.requireNonNull(original, "Copy constructor called with null argument.");
        location = Arrays.asList(copyOptions).contains(StandardCopyOption.STRIP_LOCATION)
            ? null
            : original.getLocation();
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        return otherObject != null && getClass() == otherObject.getClass()
            && Objects.equals(location, ((MutableLocatable<?>) otherObject).location);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    protected abstract D self();

    @XmlTransient
    @Override
    @Nullable
    public Location getLocation() {
        return location;
    }

    public D setLocation(@Nullable Location location) {
        this.location = location;
        return self();
    }

    /**
     * Returns whether the CloudKeeper plug-in declaration corresponding to the given class should be treated as a
     * nested declaration.
     *
     * @param clazz class object that defines a CloudKeeper plug-in declaration
     * @param copyOptions copy options
     * @return whether the CloudKeeper plug-in declaration should be treated as a nested declaration
     *
     * @see NestedNameCopyOption#isNested(Class)
     */
    protected static boolean isNested(Class<?> clazz, CopyOption[] copyOptions) {
        if (clazz.getEnclosingClass() == null) {
            return false;
        }
        for (CopyOption copyOption: copyOptions) {
            if (copyOption instanceof NestedNameCopyOption) {
                return ((NestedNameCopyOption) copyOption).isNested(clazz);
            }
        }
        return true;
    }

    /**
     * Returns the name that should be used for the CloudKeeper plug-in declaration corresponding to the given class.
     *
     * @param clazz class object that defines a CloudKeeper plug-in declaration
     * @param copyOptions copy options
     * @return the name that should be used for the CloudKeeper plug-in declaration corresponding to the given class
     * @throws NullPointerException if any argument is null
     */
    protected static Name nameForClass(Class<?> clazz, CopyOption[] copyOptions) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(copyOptions);

        @Nullable Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass != null) {
            if (isNested(clazz, copyOptions)) {
                return nameForClass(enclosingClass, copyOptions).join(SimpleName.identifier(clazz.getSimpleName()));
            }
        }
        return Name.qualifiedName(clazz.getName());
    }
}
