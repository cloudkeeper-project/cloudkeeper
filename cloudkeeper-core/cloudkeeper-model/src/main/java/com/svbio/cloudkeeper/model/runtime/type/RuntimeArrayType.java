package com.svbio.cloudkeeper.model.runtime.type;

import com.svbio.cloudkeeper.model.bare.type.BareArrayType;

import javax.annotation.Nonnull;

public interface RuntimeArrayType extends BareArrayType, RuntimeTypeMirror {
    @Override
    @Nonnull
    RuntimeTypeMirror getComponentType();
}
