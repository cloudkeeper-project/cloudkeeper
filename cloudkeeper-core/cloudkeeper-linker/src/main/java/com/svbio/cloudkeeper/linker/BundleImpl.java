package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BareBundle;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.Version;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeBundle;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.util.BuildInformation;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

final class BundleImpl extends LocatableImpl implements RuntimeBundle, ElementResolver {
    private final long creationTime;
    private final Version cloudKeeperVersion;
    private final URI bundleIdentifier;
    private final Map<Name, PackageImpl> packages;

    BundleImpl(BareBundle original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        @Nullable Date originalCreationTime = original.getCreationTime();
        creationTime = originalCreationTime == null
            ? System.currentTimeMillis()
            : originalCreationTime.getTime();
        @Nullable Version originalCloudKeeperVersion = original.getCloudKeeperVersion();
        cloudKeeperVersion = originalCloudKeeperVersion == null
            ? BuildInformation.PROJECT_VERSION
            : originalCloudKeeperVersion;
        bundleIdentifier = Preconditions.requireNonNull(
            original.getBundleIdentifier(), getCopyContext().newContextForProperty("bundleIdentifier"));
        packages
            = unmodifiableMapOf(original.getPackages(), "packages", PackageImpl::new, PackageImpl::getQualifiedName);
    }

    @Override
    public String toString() {
        return BareBundle.Default.toString(this);
    }

    @Override
    public Date getCreationTime() {
        return new Date(creationTime);
    }

    @Override
    public Version getCloudKeeperVersion() {
        return cloudKeeperVersion;
    }

    @Override
    public URI getBundleIdentifier() {
        return bundleIdentifier;
    }

    @Override
    public ImmutableList<PackageImpl> getPackages() {
        return ImmutableList.copyOf(packages.values());
    }

    @Override
    public <T extends RuntimeElement> T getElement(Class<T> clazz, Name name) {
        return ElementResolver.Default.getElement(clazz, name, packages);
    }

    @Override
    final void collectEnclosed(Collection<AbstractFreezable> freezables) {
        freezables.addAll(packages.values());
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
