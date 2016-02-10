package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.InvalidClassException;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleDeclarationVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BarePort;
import com.svbio.cloudkeeper.model.bare.element.module.BareSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.immutable.Location;

import java.util.List;

final class DSLSimpleModuleDeclaration extends DSLModuleDeclaration implements BareSimpleModuleDeclaration {
    private final SimpleModule<?> simpleModule;

    /**
     * Construct a composite-module declaration from a Java class.
     *
     * @param clazz class
     * @param moduleFactory module factory that will dynamically create a subclass and load this class
     * @throws InvalidClassException if the given class is not a composite-module declaration
     */
    <T extends SimpleModule<T>> DSLSimpleModuleDeclaration(Class<T> clazz, ModuleFactory moduleFactory) {
        super(clazz);

        if (!SimpleModule.class.isAssignableFrom(clazz)) {
            throw new InvalidClassException(String.format(
                "Expected subclass of %s, but got %s.", SimpleModule.class, clazz
            ));
        }

        simpleModule = (SimpleModule<T>) moduleFactory.create(clazz);
    }

    @Override
    public String toString() {
        return BareSimpleModuleDeclaration.Default.toString(this);
    }

    @Override
    public Location getLocation() {
        return simpleModule.getDeclarationLocation();
    }

    @Override
    public <U, P> U accept(BareModuleDeclarationVisitor<U, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    @Override
    public List<? extends BarePort> getPorts() {
        return simpleModule.getDeclaredPortsInternal();
    }
}
