package com.svbio.cloudkeeper.model.beans.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareOverride;
import com.svbio.cloudkeeper.model.bare.execution.BareOverrideTarget;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.element.MutableAnnotatedConstruct;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlType(propOrder = { "targets", "declaredAnnotations" })
public final class MutableOverride
        extends MutableAnnotatedConstruct<MutableOverride>
        implements BareOverride {
    private static final long serialVersionUID = -6994761353557555857L;

    private final ArrayList<MutableOverrideTarget<?>> overrideTargets = new ArrayList<>();

    public MutableOverride() { }

    private MutableOverride(BareOverride bareOverride, CopyOption[] copyOptions) {
        super(bareOverride, copyOptions);
        for (BareOverrideTarget target: bareOverride.getTargets()) {
            overrideTargets.add(MutableOverrideTarget.copyOfOverrideTarget(target, copyOptions));
        }
    }

    @Nullable
    public static MutableOverride copyOf(@Nullable BareOverride original, CopyOption... copyOptions) {
        return original != null
            ? new MutableOverride(original, copyOptions)
            : null;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return overrideTargets.equals(((MutableOverride) otherObject).overrideTargets);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + overrideTargets.hashCode();
    }

    @Override
    protected MutableOverride self() {
        return this;
    }

    @XmlElementWrapper(name = "targets")
    @XmlElements({
        @XmlElement(type = JAXBAdapters.JAXBElementPatternTarget.class, name = "element-regex"),
        @XmlElement(type = JAXBAdapters.JAXBElementTarget.class, name = "element"),
        @XmlElement(type = JAXBAdapters.JAXBExecutionTracePatternTarget.class, name = "execution-trace-regex"),
        @XmlElement(type = JAXBAdapters.JAXBExecutionTraceTarget.class, name = "execution-trace")
    })
    @Override
    public List<MutableOverrideTarget<?>> getTargets() {
        return overrideTargets;
    }

    public MutableOverride setTargets(List<MutableOverrideTarget<?>> overrideTargets) {
        Objects.requireNonNull(overrideTargets);
        List<MutableOverrideTarget<?>> backup = new ArrayList<>(overrideTargets);
        this.overrideTargets.clear();
        this.overrideTargets.addAll(backup);
        return this;
    }
}
