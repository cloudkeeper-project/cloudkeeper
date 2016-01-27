package com.svbio.cloudkeeper.model.runtime.element.module;

import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.runtime.element.serialization.RuntimeSerializationRoot;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeTypeMirror;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface RuntimeInputModule extends RuntimeModule, BareInputModule {
    /**
     * {@inheritDoc}
     *
     * <p>The result of this method is equal to the result of
     * {@code getEnclosedElement(RuntimePort.class, SimpleName.identifier(OUT_PORT_NAME)).getType()}.
     */
    @Override
    @Nonnull
    RuntimeTypeMirror getOutPortType();

    @Override
    @Nullable
    RuntimeSerializationRoot getRaw();
}
