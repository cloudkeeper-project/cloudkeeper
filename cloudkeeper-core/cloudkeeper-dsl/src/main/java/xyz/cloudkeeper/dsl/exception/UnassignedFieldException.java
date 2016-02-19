package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

import java.lang.reflect.Field;

public final class UnassignedFieldException extends DSLException {
    private static final long serialVersionUID = 5573248692834571044L;

    private final Field field;

    public UnassignedFieldException(Field field, Location location) {
        super(String.format("Field %s of type %s cannot be null.", field.getName(), field.getType().getSimpleName()),
            location);
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
