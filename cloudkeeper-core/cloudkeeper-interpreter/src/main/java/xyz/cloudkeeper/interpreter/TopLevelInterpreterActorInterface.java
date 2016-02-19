package xyz.cloudkeeper.interpreter;

/**
 * Message interface of {@link TopLevelInterpreterActor}.
 */
final class TopLevelInterpreterActorInterface {
    private TopLevelInterpreterActorInterface() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    /**
     * Starts the execution represented by this top-level interpreter.
     */
    enum Start {
        INSTANCE
    }
}
