package xyz.cloudkeeper.model.beans.element;

import xyz.cloudkeeper.model.bare.element.BareBundle;
import xyz.cloudkeeper.model.bare.element.BarePackage;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.beans.MutableLocatable;
import xyz.cloudkeeper.model.immutable.element.Version;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "bundle")
@XmlType(propOrder = { "bundleIdentifier", "cloudKeeperVersion", "creationTime", "packages" })
public final class MutableBundle extends MutableLocatable<MutableBundle> implements BareBundle {
    private static final long serialVersionUID = -2934121807004996084L;

    private long creationTime;
    @Nullable private Version cloudKeeperVersion;
    @Nullable private URI bundleIdentifier;
    private final ArrayList<MutablePackage> packages = new ArrayList<>();

    public MutableBundle() { }

    private MutableBundle(BareBundle original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        @Nullable Date originalCreationTime = original.getCreationTime();
        creationTime = originalCreationTime != null
            ? originalCreationTime.getTime()
            : 0;
        cloudKeeperVersion = original.getCloudKeeperVersion();
        bundleIdentifier = original.getBundleIdentifier();

        for (BarePackage thePackage: original.getPackages()) {
            packages.add(MutablePackage.copyOf(thePackage, copyOptions));
        }
    }

    @Nullable
    public static MutableBundle copyOf(@Nullable BareBundle original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableBundle(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableBundle other = (MutableBundle) otherObject;
        return creationTime == other.creationTime
            && Objects.equals(cloudKeeperVersion, other.cloudKeeperVersion)
            && Objects.equals(bundleIdentifier, other.bundleIdentifier)
            && Objects.equals(packages, other.packages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationTime, cloudKeeperVersion, bundleIdentifier, packages);
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    protected MutableBundle self() {
        return this;
    }

    @XmlElement(name = "creation-time")
    @Override
    @Nullable
    public Date getCreationTime() {
        return creationTime == 0
            ? null
            : new Date(creationTime);
    }

    public MutableBundle setCreationTime(@Nullable Date creationTime) {
        this.creationTime = creationTime != null
            ? creationTime.getTime()
            : 0;
        return this;
    }

    @XmlElement(name = "cloudkeeper-version")
    @Override
    @Nullable
    public Version getCloudKeeperVersion() {
        return cloudKeeperVersion;
    }

    public MutableBundle setCloudKeeperVersion(@Nullable Version cloudKeeperVersion) {
        this.cloudKeeperVersion = cloudKeeperVersion;
        return this;
    }

    @XmlElement(name = "bundle-identifier")
    @Override
    @Nullable
    public URI getBundleIdentifier() {
        return bundleIdentifier;
    }

    public MutableBundle setBundleIdentifier(@Nullable URI bundleIdentifier) {
        this.bundleIdentifier = bundleIdentifier;
        return this;
    }

    @XmlElementWrapper(name = "packages")
    @XmlElementRef
    @Override
    public List<MutablePackage> getPackages() {
        return packages;
    }

    /**
     * Sets the list of packages.
     *
     * @param packages new list of packages
     * @return this instance
     */
    public MutableBundle setPackages(List<MutablePackage> packages) {
        Objects.requireNonNull(packages);
        List<MutablePackage> backup = new ArrayList<>(packages);
        this.packages.clear();
        this.packages.addAll(backup);
        return this;
    }
}
