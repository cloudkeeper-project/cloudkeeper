package com.svbio.cloudkeeper.model.api;

import net.florianschoppmann.java.reflect.ReflectionTypes;

import javax.lang.model.element.TypeElement;

/**
 * This class consists exclusively of immutable singletons.
 *
 * <p>These fields are meant to be package private and can therefore not reside in any of the interfaces.
 */
final class Singletons {
    private Singletons() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static final ReflectionTypes TYPES = ReflectionTypes.getInstance();
    static final TypeElement SERIALIAZATION_CLASS_MIRROR = TYPES.typeElement(Marshaler.class);
}
