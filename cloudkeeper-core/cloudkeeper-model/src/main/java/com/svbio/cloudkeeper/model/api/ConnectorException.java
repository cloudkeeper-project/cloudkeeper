package com.svbio.cloudkeeper.model.api;

/**
 * Signals that the {@link com.svbio.cloudkeeper.model.api.ModuleConnector} encountered an exception.
 *
 * This exception typically occurs when user-defined code calls CloudKeeper code (via the
 * {@link com.svbio.cloudkeeper.model.api.ModuleConnector}), meaning that this exception is typically not
 * recoverable. It is therefore a {@link RuntimeException}.
 */
public class ConnectorException extends RuntimeException {
    private static final long serialVersionUID = 1910611073043712090L;

    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
