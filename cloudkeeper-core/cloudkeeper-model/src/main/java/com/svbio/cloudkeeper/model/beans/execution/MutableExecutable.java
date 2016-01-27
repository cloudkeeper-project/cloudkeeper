package com.svbio.cloudkeeper.model.beans.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareExecutable;
import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "executable")
public final class MutableExecutable extends MutableLocatable<MutableExecutable> implements BareExecutable {
    private static final long serialVersionUID = -7484744163036502856L;

    @Nullable private MutableModule<?> module;
    private final ArrayList<MutableOverride> overrides = new ArrayList<>();
    private final ArrayList<URI> dependencies = new ArrayList<>();

    public MutableExecutable() { }

    private MutableExecutable(BareExecutable original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        module = MutableModule.copyOfModule(original.getModule(), copyOptions);

        for (BareOverride override: original.getOverrides()) {
            overrides.add(MutableOverride.copyOf(override, copyOptions));
        }

        dependencies.addAll(original.getBundleIdentifiers());
    }

    @Nullable
    public static MutableExecutable copyOf(@Nullable BareExecutable original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableExecutable(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        MutableExecutable other = (MutableExecutable) otherObject;
        return Objects.equals(module, other.module)
            && Objects.equals(overrides, other.overrides)
            && Objects.equals(dependencies, other.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, overrides, dependencies);
    }

    @Override
    protected MutableExecutable self() {
        return this;
    }

    @XmlElementRef
    @Override
    @Nullable
    public MutableModule<?> getModule() {
        return module;
    }

    public MutableExecutable setModule(@Nullable MutableModule<?> module) {
        this.module = module;
        return this;
    }

    @XmlElementWrapper(name = "overrides")
    @XmlElement(name = "override")
    @Override
    public List<MutableOverride> getOverrides() {
        return overrides;
    }

    public MutableExecutable setOverrides(List<MutableOverride> overrides) {
        Objects.requireNonNull(overrides);
        List<MutableOverride> backup = new ArrayList<>(overrides);
        this.overrides.clear();
        this.overrides.addAll(backup);
        return this;
    }

    @XmlElementWrapper(name = "bundle-identifiers")
    @XmlElement(name = "bundle-identifiers")
    @Override
    public List<URI> getBundleIdentifiers() {
        return dependencies;
    }

    public MutableExecutable setBundleIdentifiers(List<URI> dependencies) {
        Objects.requireNonNull(dependencies);
        List<URI> backup = new ArrayList<>(dependencies);
        this.dependencies.clear();
        this.dependencies.addAll(backup);
        return this;
    }
}
