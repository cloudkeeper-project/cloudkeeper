package com.svbio.cloudkeeper.dsl.exception;

public class InvalidClassException extends DSLException {
    private static final long serialVersionUID = 4743297731156822744L;

    public InvalidClassException(String message) {
        super(message, null);
    }

    public InvalidClassException(String message, Throwable cause) {
        super(message, cause, null);
    }
}
