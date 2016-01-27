package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "simple-module-declaration")
public final class MutableSimpleModuleDeclaration
        extends MutableModuleDeclaration<MutableSimpleModuleDeclaration>
        implements BareSimpleModuleDeclaration {
    private static final long serialVersionUID = 3514553530856180309L;

    private final ArrayList<MutablePort<?>> ports = new ArrayList<>();

    public MutableSimpleModuleDeclaration() { }

    private MutableSimpleModuleDeclaration(BareSimpleModuleDeclaration original, CopyOption[] copyOptions) {
        super(original, copyOptions);

        for (BarePort port: original.getPorts()) {
            ports.add(MutablePort.copyOfPort(port, copyOptions));
        }
    }

    @Nullable
    public static MutableSimpleModuleDeclaration copyOfSimpleModuleDeclaration(
            @Nullable BareSimpleModuleDeclaration original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableSimpleModuleDeclaration(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return ports.equals(((MutableSimpleModuleDeclaration) otherObject).ports);

    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + ports.hashCode();
    }

    @Override
    public String toString() {
        return BareSimpleModuleDeclaration.Default.toString(this);
    }

    @Override
    protected MutableSimpleModuleDeclaration self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleDeclarationVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @XmlElementWrapper(name = "ports")
    @XmlElementRef
    @Override
    public List<MutablePort<?>> getPorts() {
        return ports;
    }

    public MutableSimpleModuleDeclaration setPorts(List<MutablePort<?>> ports) {
        Objects.requireNonNull(ports);
        List<MutablePort<?>> backup = new ArrayList<>(ports);
        this.ports.clear();
        this.ports.addAll(backup);
        return this;
    }
}
