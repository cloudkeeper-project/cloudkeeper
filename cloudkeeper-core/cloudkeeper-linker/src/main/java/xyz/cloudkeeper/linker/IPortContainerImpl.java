package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.runtime.element.module.RuntimePortContainer;
import xyz.cloudkeeper.model.util.ImmutableList;

interface IPortContainerImpl extends RuntimePortContainer, IElementImpl {
    @Override
    ImmutableList<PortImpl> getPorts();

    @Override
    ImmutableList<IInPortImpl> getInPorts();

    @Override
    ImmutableList<IOutPortImpl> getOutPorts();
}
