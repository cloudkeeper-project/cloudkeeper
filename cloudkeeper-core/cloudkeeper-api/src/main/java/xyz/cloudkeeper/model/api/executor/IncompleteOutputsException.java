package xyz.cloudkeeper.model.api.executor;

import xyz.cloudkeeper.model.api.ExecutionException;

/**
 * Signals that there is an out-port for which
 * {@link xyz.cloudkeeper.model.api.ModuleConnector#setOutput(xyz.cloudkeeper.model.immutable.element.SimpleName, Object)}
 * was not called.
 */
public class IncompleteOutputsException extends ExecutionException {
    private static final long serialVersionUID = 2399514957358466277L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public IncompleteOutputsException(String message) {
        super(message);
    }
}
