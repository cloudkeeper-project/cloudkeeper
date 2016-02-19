package xyz.cloudkeeper.model.runtime.type;

import xyz.cloudkeeper.model.bare.type.BareWildcardType;

import javax.annotation.Nullable;

public interface RuntimeWildcardType extends RuntimeTypeMirror, BareWildcardType {
    @Override
    @Nullable
    RuntimeTypeMirror getExtendsBound();

    @Override
    @Nullable
    RuntimeTypeMirror getSuperBound();
}
