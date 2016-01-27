package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleVisitor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

final class CompositeModuleImpl extends ParentModuleImpl implements RuntimeCompositeModule {
    CompositeModuleImpl(@Nullable BareCompositeModule original, CopyContext parentContext, int index)
            throws LinkerException {
        super(original, parentContext, index, Collections.<BarePort>emptyList());
    }

    @Override
    public String toString() {
        return BareCompositeModule.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitCompositeModule(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimeModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    void collectEnclosedByParentModule(Collection<AbstractFreezable> freezables) { }
}
