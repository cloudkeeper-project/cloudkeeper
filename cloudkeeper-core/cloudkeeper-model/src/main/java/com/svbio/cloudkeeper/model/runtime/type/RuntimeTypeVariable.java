package com.svbio.cloudkeeper.model.runtime.type;

import com.svbio.cloudkeeper.model.bare.type.BareTypeVariable;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeParameterElement;

import javax.annotation.Nonnull;

public interface RuntimeTypeVariable extends RuntimeTypeMirror, BareTypeVariable {
    @Override
    @Nonnull
    RuntimeTypeParameterElement getFormalTypeParameter();
}
