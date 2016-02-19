package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.runtime.element.RuntimeParameterizable;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.lang.model.element.Parameterizable;

interface IParameterizableImpl extends ITypeElementImpl, RuntimeParameterizable, Parameterizable {
    @Override
    ITypeElementImpl getEnclosingElement();

    @Override
    ImmutableList<TypeParameterElementImpl> getTypeParameters();
}
