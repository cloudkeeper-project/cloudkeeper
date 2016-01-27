package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.ConstraintException;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.MissingPropertyException;

import javax.annotation.Nullable;

final class Preconditions {
    private Preconditions() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Verifies that model constraint is satisfied, throws a {@link ConstraintException}
     * if not.
     *
     * @param condition condition that needs to be satisfied
     * @param copyContext copy context
     * @param message format string of exception message if condition is not satisfied
     * @param arguments format arguments
     *
     * @see String#format(String, Object...)
     * @throws ConstraintException if {@code condition} is false
     */
    static void requireCondition(boolean condition, CopyContext copyContext, String message, Object... arguments)
            throws ConstraintException {
        if (!condition) {
            throw new ConstraintException(String.format(message, arguments), copyContext.toLinkerTrace());
        }
    }

    /**
     * Verifies that the first argument is not null, throws a {@link MissingPropertyException} if it is null.
     *
     * @param object object that must not be null
     * @param copyContext copy context
     * @param <T> type of the first argument
     * @return the first argument
     * @throws MissingPropertyException if the first argument is null
     */
    static <T> T requireNonNull(@Nullable T object, CopyContext copyContext) throws LinkerException {
        if (object == null) {
            throw copyContext.newMissingException();
        }
        return object;
    }
}
