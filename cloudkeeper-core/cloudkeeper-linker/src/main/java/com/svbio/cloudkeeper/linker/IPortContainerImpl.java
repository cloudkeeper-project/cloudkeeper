package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.runtime.element.module.RuntimePortContainer;
import com.svbio.cloudkeeper.model.util.ImmutableList;

interface IPortContainerImpl extends RuntimePortContainer, IElementImpl {
    @Override
    ImmutableList<PortImpl> getPorts();

    @Override
    ImmutableList<IInPortImpl> getInPorts();

    @Override
    ImmutableList<IOutPortImpl> getOutPorts();
}
