package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.model.immutable.Location;

import java.lang.annotation.Annotation;

public final class MissingAnnotationException extends DSLException {
    private static final long serialVersionUID = 4855296750107416136L;

    public MissingAnnotationException(Class<?> clazz, Class<? extends Annotation> missingAnnotation,
            Location location) {
        super(String.format("Expected annotation %s on %s", missingAnnotation.getName(), clazz), location);
    }
}
