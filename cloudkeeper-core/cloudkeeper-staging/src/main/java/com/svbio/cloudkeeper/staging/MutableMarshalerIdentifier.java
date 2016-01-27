package com.svbio.cloudkeeper.staging;

import com.svbio.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URI;
import java.util.Objects;

/**
 * Identifier of a marshaler plug-in.
 *
 * <p>Instances of this class contain the plug-in name and the bundle identifier.
 *
 * <p>This class has JAXB annotations. Instances of this class are therefore necessarily mutable.
 *
 * @see MutableObjectMetadata
 */
@XmlType(propOrder = { "name", "bundleIdentifier" })
public final class MutableMarshalerIdentifier {
    private Name name;
    private URI bundleIdentifier;

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableMarshalerIdentifier other = (MutableMarshalerIdentifier) otherObject;
        return Objects.equals(name, other.name)
            && Objects.equals(bundleIdentifier, other.bundleIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bundleIdentifier);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, bundleIdentifier);
    }

    public Name getName() {
        return name;
    }

    public MutableMarshalerIdentifier setName(Name name) {
        this.name = name;
        return this;
    }

    public MutableMarshalerIdentifier setName(String name) {
        return setName(Name.qualifiedName(name));
    }

    @XmlElement(name = "bundle-identifier")
    public URI getBundleIdentifier() {
        return bundleIdentifier;
    }

    public MutableMarshalerIdentifier setBundleIdentifier(URI bundleIdentifier) {
        this.bundleIdentifier = bundleIdentifier;
        return this;
    }
}
