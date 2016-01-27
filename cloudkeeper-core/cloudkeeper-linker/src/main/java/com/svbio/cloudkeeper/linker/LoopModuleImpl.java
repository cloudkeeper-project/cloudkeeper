package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.linker.PortImpl.OutPortImpl;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeLoopModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModuleVisitor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

final class LoopModuleImpl extends ParentModuleImpl implements RuntimeLoopModule {
    private final OutPortImpl continuePort;

    /**
     * Constructs a new loop module that is a deep mutable copy of the given loop module.
     */
    LoopModuleImpl(BareLoopModule original, CopyContext parentContext, int index) throws LinkerException {
        super(original, parentContext, index, Collections.singletonList(newContinuePort()));

        continuePort = Objects.requireNonNull(
            getEnclosedElement(OutPortImpl.class, SimpleName.identifier(CONTINUE_PORT_NAME))
        );
    }

    private static MutableOutPort newContinuePort() {
        return new MutableOutPort()
            .setSimpleName(CONTINUE_PORT_NAME)
            .setType(new MutableDeclaredType().setDeclaration(Boolean.class.getName()));
    }

    @Override
    public String toString() {
        return BareLoopModule.Default.toString(this);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitLoopModule(this, parameter);
    }

    @Override
    @Nullable
    public <T, P> T accept(RuntimeModuleVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public OutPortImpl getContinuePort() {
        return continuePort;
    }

    @Override
    void collectEnclosedByParentModule(Collection<AbstractFreezable> freezables) {
        freezables.add(continuePort);
    }
}
