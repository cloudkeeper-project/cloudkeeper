package com.svbio.cloudkeeper.model.runtime.element.type;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeElement;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeParameterizable;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import com.svbio.cloudkeeper.model.util.ImmutableList;

public interface RuntimeTypeParameterElement extends BareTypeParameterElement, RuntimeElement, Immutable {
    @Override
    RuntimeParameterizable getEnclosingElement();

    @Override
    SimpleName getSimpleName();

    @Override
    ImmutableList<? extends RuntimeTypeMirror> getBounds();
}
