package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "composite-module")
public final class MutableCompositeModule
        extends MutableParentModule<MutableCompositeModule>
        implements BareCompositeModule {
    private static final long serialVersionUID = -192306489008924470L;

    public MutableCompositeModule() { }

    private MutableCompositeModule(BareCompositeModule original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    @Nullable
    public static MutableCompositeModule copyOfCompositeModule(@Nullable BareCompositeModule original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableCompositeModule(original, copyOptions);
    }

    @Override
    public String toString() {
        return BareCompositeModule.Default.toString(this);
    }

    @Override
    protected MutableCompositeModule self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitCompositeModule(this, parameter);
    }
}
