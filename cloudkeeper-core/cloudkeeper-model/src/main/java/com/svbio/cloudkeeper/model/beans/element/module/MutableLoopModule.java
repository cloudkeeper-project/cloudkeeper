package com.svbio.cloudkeeper.model.beans.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "loop-module")
public final class MutableLoopModule extends MutableParentModule<MutableLoopModule> implements BareLoopModule {
    private static final long serialVersionUID = -1954984713258560579L;

    public MutableLoopModule() { }

    private MutableLoopModule(BareLoopModule original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    @Nullable
    public static MutableLoopModule copyOfLoopModule(@Nullable BareLoopModule original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableLoopModule(original, copyOptions);
    }

    @Override
    protected MutableLoopModule self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitLoopModule(this, parameter);
    }

    @Override
    public String toString() {
        return BareLoopModule.Default.toString(this);
    }
}
