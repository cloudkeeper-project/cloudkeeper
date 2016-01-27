package com.svbio.cloudkeeper.model.runtime.type;

import com.svbio.cloudkeeper.model.bare.type.BareDeclaredType;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;

public interface RuntimeDeclaredType extends RuntimeTypeMirror, BareDeclaredType {
    @Override
    @Nonnull
    RuntimeTypeMirror getEnclosingType();

    @Override
    @Nonnull
    RuntimeTypeDeclaration getDeclaration();

    @Override
    ImmutableList<? extends RuntimeTypeMirror> getTypeArguments();
}
