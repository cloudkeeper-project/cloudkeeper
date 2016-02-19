package xyz.cloudkeeper.staging;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Metadata for an object in a staging area of type {@link ExternalStagingArea}.
 *
 * <p>Object metadata consists of a list of marshaler plug-ins, each identified by the plug-in name and the bundle
 * identifier. See {@link MutableMarshalerIdentifier}. The list is in the order of unmarshaling: The first marshaler
 * is invoked and unmarshals into its associated type. Then the next marshaler is invoked unmarshaling into its
 * associated type, etc.
 *
 * <p>This class has JAXB annotations. Instances of this class are therefore necessarily mutable.
 */
@XmlRootElement(name = "object-metadata")
public final class MutableObjectMetadata {
    private final List<MutableMarshalerIdentifier> marshalers = new ArrayList<>();

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableObjectMetadata other = (MutableObjectMetadata) otherObject;

        return marshalers.equals(other.marshalers);
    }

    @Override
    public int hashCode() {
        return marshalers.hashCode();
    }

    @XmlElementWrapper(name = "marshalers")
    @XmlElement(name = "marshaler")
    public List<MutableMarshalerIdentifier> getMarshalers() {
        return marshalers;
    }

    /**
     * Sets the list of marshalers.
     *
     * @param marshalers new list of marshalers
     * @return this instance
     */
    public MutableObjectMetadata setMarshalers(List<MutableMarshalerIdentifier> marshalers) {
        Objects.requireNonNull(marshalers);
        List<MutableMarshalerIdentifier> backup = new ArrayList<>(marshalers);
        this.marshalers.clear();
        this.marshalers.addAll(backup);
        return this;
    }
}
