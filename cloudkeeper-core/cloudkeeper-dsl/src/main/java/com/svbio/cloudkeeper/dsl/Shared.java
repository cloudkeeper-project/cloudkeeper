package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

final class Shared {
    private Shared() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns the binary name of the given class without the package name.
     */
    static SimpleName simpleNameOfClass(Class<?> clazz) {
        return SimpleName.identifier(clazz.getName().substring(clazz.getPackage().getName().length() + 1));
    }
}
