package com.svbio.cloudkeeper.model.runtime.element.type;

import com.svbio.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeParameterizable;
import com.svbio.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;
import com.svbio.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nonnull;

public interface RuntimeTypeDeclaration extends RuntimePluginDeclaration, BareTypeDeclaration, RuntimeParameterizable {
    @Nonnull
    @Override
    RuntimeTypeMirror getSuperclass();

    @Override
    ImmutableList<? extends RuntimeTypeMirror> getInterfaces();

    /**
     * Returns the Java class represented by this type declaration.
     */
    Class<?> toClass();
}
