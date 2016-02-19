package xyz.cloudkeeper.model.runtime.element.type;

import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;
import xyz.cloudkeeper.model.runtime.element.RuntimeParameterizable;
import xyz.cloudkeeper.model.runtime.element.RuntimePluginDeclaration;
import xyz.cloudkeeper.model.runtime.type.RuntimeTypeMirror;
import xyz.cloudkeeper.model.util.ImmutableList;

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
