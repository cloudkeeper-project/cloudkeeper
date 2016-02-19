package xyz.cloudkeeper.model.runtime.type;

import xyz.cloudkeeper.model.bare.type.BareTypeVariable;
import xyz.cloudkeeper.model.runtime.element.type.RuntimeTypeParameterElement;

import javax.annotation.Nonnull;

public interface RuntimeTypeVariable extends RuntimeTypeMirror, BareTypeVariable {
    @Override
    @Nonnull
    RuntimeTypeParameterElement getFormalTypeParameter();
}
