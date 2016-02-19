package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.model.beans.NestedNameCopyOption;

enum DSLNestedNameCopyOption implements NestedNameCopyOption {
    INSTANCE;

    @Override
    public boolean isNested(Class<?> clazz) {
        return false;
    }
}
