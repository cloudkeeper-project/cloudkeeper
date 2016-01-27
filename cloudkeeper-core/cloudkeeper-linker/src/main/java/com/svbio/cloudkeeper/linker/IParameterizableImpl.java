package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.runtime.element.RuntimeParameterizable;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.lang.model.element.Parameterizable;

interface IParameterizableImpl extends ITypeElementImpl, RuntimeParameterizable, Parameterizable {
    @Override
    ITypeElementImpl getEnclosingElement();

    @Override
    ImmutableList<TypeParameterElementImpl> getTypeParameters();
}
