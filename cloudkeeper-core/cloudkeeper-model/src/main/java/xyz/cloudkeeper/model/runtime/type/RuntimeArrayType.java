package xyz.cloudkeeper.model.runtime.type;

import xyz.cloudkeeper.model.bare.type.BareArrayType;

import javax.annotation.Nonnull;

public interface RuntimeArrayType extends BareArrayType, RuntimeTypeMirror {
    @Override
    @Nonnull
    RuntimeTypeMirror getComponentType();
}
