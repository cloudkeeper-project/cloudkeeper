package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.model.immutable.Location;

final class Locatables {
    static final String PACKAGE_NAME = Locatables.class.getPackage().getName();

    enum State {
        FIND_THIS_PACKAGE,
//        FIND_CALLING_PACKAGE,
        FIND_USER_PACKAGE
    }

    private Locatables() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Returns the first stack-trace element that does not stem from this package.
     *
     * If stack-trace information is unavailable, a special {@link StackTraceElement} instance is returns, where all
     * fields are set to indicate unavailable information.
     *
     * This function should be used as close to the invocation by the client as possible (that is, as low in the stack
     * backtrace as possible).
     *
     * @return stack-trace element (always nonnull)
     */
    public static Location getCallingStackTraceElement() {
        // The following is a state variable: During state FIND_THIS_PACKAGE, we continue until we find a
        // StackTraceElement from this package. Once we found one, the state is switched to FIND_CALLING_PACKAGE, and
        // we look until the first package outside this package. This is assumed to be the calling package. State is
        // switched to FIND_USER_PACKAGE, and we continue further until a package outside the calling package is found.
        // This is assumed to be the appropriate stack frame.
        // The rationale for search phase FIND_THIS_PACKAGE is: It is not well-defined what the first element in the
        // stack trace return by Thread#getStackTrace() is. It may, for instance, be Thread#getStackTrace(), but it may
        // also be this method:
        // The documentation of Thread#currentThread() specifies: "Some virtual machines may, under
        // some circumstances, omit one or more stack frames from the stack trace."
        State state = State.FIND_THIS_PACKAGE;

        StackTraceElement callingFrame = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String userPackage = null;
        for (StackTraceElement stackTraceElement: stackTrace) {
            String className = stackTraceElement.getClassName();
            if (stackTraceElement.getClassName() == null) {
                break;
            }

            int lastIndexOfDot = className.lastIndexOf('.');
            String packageName = lastIndexOfDot < 0
                ? ""
                : className.substring(0, lastIndexOfDot);

            if (state == State.FIND_THIS_PACKAGE && PACKAGE_NAME.equals(packageName)) {
                // We found a stack-trace element from this package. Good, now go on and find the first strack-trace
                // element out of this package.
                // state = State.FIND_CALLING_PACKAGE;
                state = State.FIND_USER_PACKAGE;
            // } else if (state == State.FIND_CALLING_PACKAGE && !PACKAGE_NAME.equals(packageName)) {
            //    state = State.FIND_USER_PACKAGE;
            //    userPackage = packageName;
            // } else if (state == State.FIND_USER_PACKAGE && !userPackage.equals(packageName)) {
            } else if (state == State.FIND_USER_PACKAGE && !PACKAGE_NAME.equals(packageName)) {
                callingFrame = stackTraceElement;
                break;
            }
        }
        // fileName == null indicates that information is unavailable
        // lineNumber < 0 indicates that information is unavailable
        return toLocation(callingFrame);
    }

    private static Location toLocation(StackTraceElement stackTraceElement) {
        return stackTraceElement == null
            ? null
            : new Location("java:" + stackTraceElement.getFileName() + ':' + stackTraceElement.getClassName()
                + '#' + stackTraceElement.getMethodName(), stackTraceElement.getLineNumber(), -1);
    }
}
