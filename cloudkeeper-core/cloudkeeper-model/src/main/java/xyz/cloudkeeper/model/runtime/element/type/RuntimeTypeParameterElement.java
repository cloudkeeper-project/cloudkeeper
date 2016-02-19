package xyz.cloudkeeper.model.runtime.element.type;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.type.BareTypeParameterElement;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.runtime.element.RuntimeParameterizable;
import xyz.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import xyz.cloudkeeper.model.util.ImmutableList;

public interface RuntimeTypeParameterElement extends BareTypeParameterElement, RuntimeElement, Immutable {
    @Override
    RuntimeParameterizable getEnclosingElement();

    @Override
    SimpleName getSimpleName();

    @Override
    ImmutableList<? extends RuntimeTypeMirror> getBounds();
}
