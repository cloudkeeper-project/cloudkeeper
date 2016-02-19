package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.BareQualifiedNameable;
import xyz.cloudkeeper.model.bare.element.annotation.BareAnnotation;
import xyz.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import xyz.cloudkeeper.model.bare.element.module.BareProxyModule;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

final class DSLProxyModule implements BareProxyModule, Immutable {
    private final Module<?> module;
    private final MutableQualifiedNamable declaration;

    DSLProxyModule(Module<?> module) {
        Objects.requireNonNull(module);
        if (!(module instanceof SimpleModule<?>) && !(module instanceof CompositeModule<?>)) {
            throw new IllegalArgumentException(String.format(
                "Expected module class in %s, but got %s.",
                Arrays.asList(SimpleModule.class, CompositeModule.class),
                module.getClass()
            ));
        }

        this.module = module;
        declaration = new MutableQualifiedNamable()
            .setQualifiedName(Name.qualifiedName(module.getModuleClass().getName()));
    }

    @Override
    public String toString() {
        return Default.toString(this);
    }

    @Override
    public <T, P> T accept(BareModuleVisitor<T, P> visitor, P parameter) {
        return visitor.visitLinkedModule(this, parameter);
    }

    @Override
    public SimpleName getSimpleName() {
        return module.getSimpleName();
    }

    @Override
    public BareQualifiedNameable getDeclaration() {
        // Defensive copy
        return MutableQualifiedNamable.copyOf(declaration);
    }

    @Override
    public List<? extends BareAnnotation> getDeclaredAnnotations() {
        return module.getDeclaredAnnotations();
    }

    @Override
    public Location getLocation() {
        return module.getLocation();
    }
}
