package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareConnectionVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareShortCircuitConnection;
import com.svbio.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "fromPort", "toPort" })
public final class MutableShortCircuitConnection
        extends MutableConnection<MutableShortCircuitConnection>
        implements BareShortCircuitConnection {
    private static final long serialVersionUID = 9155345943326140284L;

    public MutableShortCircuitConnection() { }

    private MutableShortCircuitConnection(BareShortCircuitConnection original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    @Nullable
    public static MutableShortCircuitConnection copyOfShortCircuitConnection(
            @Nullable BareShortCircuitConnection original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableShortCircuitConnection(original, copyOptions);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareConnectionVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitShortCircuitConnection(this, parameter);
    }

    @Override
    protected MutableShortCircuitConnection self() {
        return this;
    }
}
