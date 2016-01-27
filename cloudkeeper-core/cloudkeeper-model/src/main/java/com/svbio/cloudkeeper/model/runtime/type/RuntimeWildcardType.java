package com.svbio.cloudkeeper.model.runtime.type;

import com.svbio.cloudkeeper.model.bare.type.BareWildcardType;

import javax.annotation.Nullable;

public interface RuntimeWildcardType extends RuntimeTypeMirror, BareWildcardType {
    @Override
    @Nullable
    RuntimeTypeMirror getExtendsBound();

    @Override
    @Nullable
    RuntimeTypeMirror getSuperBound();
}
