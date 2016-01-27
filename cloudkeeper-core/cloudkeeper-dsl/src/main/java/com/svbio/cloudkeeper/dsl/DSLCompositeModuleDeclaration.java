package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.InvalidClassException;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.immutable.Location;

final class DSLCompositeModuleDeclaration extends DSLModuleDeclaration implements BareCompositeModuleDeclaration {
    private final CompositeModule<?> template;

    /**
     * Construct a composite-module declaration from a Java class.
     *
     * @param clazz class
     * @param moduleFactory module factory that will dynamically create a subclass and load this class
     * @throws InvalidClassException if the given class is not a composite-module declaration
     */
    <T extends CompositeModule<T>> DSLCompositeModuleDeclaration(Class<T> clazz, ModuleFactory moduleFactory) {
        super(clazz);

        if (!CompositeModule.class.isAssignableFrom(clazz)) {
            throw new InvalidClassException(String.format(
                "Expected subclass of %s, but got %s.", CompositeModule.class, clazz
            ));
        }

        template = (CompositeModule<T>) moduleFactory.create(clazz);
    }

    @Override
    public String toString() {
        return BareCompositeModuleDeclaration.Default.toString(this);
    }

    @Override
    public Location getLocation() {
        return template.getDeclarationLocation();
    }

    @Override
    public <U, P> U accept(BareModuleDeclarationVisitor<U, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public BareCompositeModule getTemplate() {
        return template;
    }
}
