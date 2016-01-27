package com.svbio.cloudkeeper.dsl.exception;

import com.svbio.cloudkeeper.model.immutable.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static java.util.Objects.requireNonNull;

/**
 * Signals that two fields in a {@link com.svbio.cloudkeeper.dsl.Module} instance contain the same value.
 */
public final class RedundantFieldException extends DSLException {
    private static final long serialVersionUID = -5613995740830885378L;

    private final ArrayList<String> fields;

    /**
     * Constructor.
     *
     * @param fields collection of at least two fields that contain the same value
     * @param fieldContainerClass name of class containing the fields, must be result of {@link Class#toString()}
     * @param fieldValueClass name of the class of the redundant field value, must be result of {@link Class#toString()}
     * @param location the location where the value was created, may be {@code null} if unknown
     */
    public RedundantFieldException(Collection<String> fields, String fieldContainerClass, String fieldValueClass,
        Location location) {

        super(
            String.format(
                "Fields %s in %s cannot contain the same reference (instance of %s).",
                requireNonNullWithTwoElemements(fields),
                fieldContainerClass,
                fieldValueClass
            ),
            location
        );
        if (fields.size() < 2) {
            throw new IllegalArgumentException(String.format("Expected more than one field, but got %s", fields));
        }
        this.fields = new ArrayList<>(fields);
    }

    public Collection<String> getFields() {
        return Collections.unmodifiableCollection(fields);
    }

    static Collection<String> requireNonNullWithTwoElemements(Collection<String> fields) {
        requireNonNull(fields);
        if (fields.size() < 2) {
            throw new IllegalArgumentException(String.format("Expected more than one field, but got %s", fields));
        }
        return fields;
    }
}
