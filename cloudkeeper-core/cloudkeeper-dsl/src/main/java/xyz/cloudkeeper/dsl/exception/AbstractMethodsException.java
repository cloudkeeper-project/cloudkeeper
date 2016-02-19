package xyz.cloudkeeper.dsl.exception;

import xyz.cloudkeeper.model.immutable.Location;

public class AbstractMethodsException extends DSLException {
    private static final long serialVersionUID = 8755019301821212500L;

    public AbstractMethodsException(String className, String methodName, Location location) {
        super(String.format("Class %s must implement method %s.", className, methodName), location);
    }
}
