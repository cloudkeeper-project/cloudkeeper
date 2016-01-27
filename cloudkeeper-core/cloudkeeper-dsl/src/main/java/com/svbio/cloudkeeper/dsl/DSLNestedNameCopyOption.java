package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.beans.NestedNameCopyOption;

enum DSLNestedNameCopyOption implements NestedNameCopyOption {
    INSTANCE;

    @Override
    public boolean isNested(Class<?> clazz) {
        return false;
    }
}
