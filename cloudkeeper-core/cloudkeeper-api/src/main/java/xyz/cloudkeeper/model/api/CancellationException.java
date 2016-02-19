package xyz.cloudkeeper.model.api;

/**
 * Signals that a workflow execution was cancelled.
 */
public class CancellationException extends WorkflowExecutionException {
    private static final long serialVersionUID = 8478747169675290622L;

    public CancellationException() {
        super("Execution cancelled.");
    }
}
