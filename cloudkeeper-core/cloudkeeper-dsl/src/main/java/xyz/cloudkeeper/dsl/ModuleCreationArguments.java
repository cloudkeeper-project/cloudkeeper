package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.model.api.ModuleConnector;
import xyz.cloudkeeper.model.immutable.Location;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Objects;

final class ModuleCreationArguments {
    static final ThreadLocal<ModuleCreationArguments> CHILD_CREATION_THREAD_LOCAL = new ThreadLocal<>();

    private final ModuleFactory moduleFactory;
    @Nullable private final DSLParentModule<?> parent;
    @Nullable private final Location location;
    private final Type genericType;
    @Nullable private final ModuleConnector moduleConnector;

    ModuleCreationArguments(ModuleFactory moduleFactory, @Nullable DSLParentModule<?> parent, Type genericType,
        @Nullable Location location, @Nullable ModuleConnector moduleConnector) {

        this.moduleFactory = Objects.requireNonNull(moduleFactory);
        this.parent = parent;
        this.genericType = Objects.requireNonNull(genericType);
        this.location = location;
        this.moduleConnector = moduleConnector;
    }

    ModuleFactory getModuleFactory() {
        return moduleFactory;
    }

    @Nullable
    DSLParentModule<?> getParent() {
        return parent;
    }

    Type getGenericType() {
        return genericType;
    }

    @Nullable
    Location getLocation() {
        return location;
    }

    @Nullable
    ModuleConnector getModuleConnector() {
        return moduleConnector;
    }
}
