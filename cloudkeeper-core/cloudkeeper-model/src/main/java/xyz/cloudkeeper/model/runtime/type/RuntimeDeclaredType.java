package xyz.cloudkeeper.model.runtime.type;

import xyz.cloudkeeper.model.bare.type.BareDeclaredType;
import xyz.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration;
import xyz.cloudkeeper.model.util.ImmutableList;

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
