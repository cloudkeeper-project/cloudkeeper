package xyz.cloudkeeper.model.runtime.execution;

/**
 * Signals that an illegal execution trace was detected.
 *
 * <p>This exception is thrown if one of the {@code resolve} methods in
 * {@link RuntimeExecutionTrace} is used where not allowed according to the
 * grammar defined by {@link xyz.cloudkeeper.model.bare.execution.BareExecutionTrace}. For instance,
 * this exception is thrown if {@link RuntimeExecutionTrace#resolveOutPort} is called on an execution trace of type
 * {@link RuntimeExecutionTrace.Type#IN_PORT} because no execution trace may contain more than one port element.
 *
 * <p>This exception is also thrown if one of the {@code resolve} methods in {@link RuntimeAnnotatedExecutionTrace} is
 * used ways inconsistent with the represented language elements. For instance, this exception is thrown if
 * {@link RuntimeAnnotatedExecutionTrace#resolveOutPort} is called on an execution trace of type
 * {@link RuntimeExecutionTrace.Type#MODULE} if the module represented by the execution trace does not contain an
 * out-port with the specified name.
 */
public class IllegalExecutionTraceException extends IllegalStateException {
    private static final long serialVersionUID = -9073173380374763339L;

    /**
     * Constructs an exception with the given details message.
     *
     * @param message detailed message
     */
    public IllegalExecutionTraceException(String message) {
        super(message);
    }
}
