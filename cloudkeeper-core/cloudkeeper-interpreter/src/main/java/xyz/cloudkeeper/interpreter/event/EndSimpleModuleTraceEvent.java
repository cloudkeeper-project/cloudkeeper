package xyz.cloudkeeper.interpreter.event;

import akka.japi.Option;
import xyz.cloudkeeper.model.api.executor.SimpleModuleExecutorResult;
import xyz.cloudkeeper.model.runtime.execution.RuntimeExecutionTrace;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Signals that the interpreter finished a simple module.
 *
 * <p>If successful, this event contains a simple-module execution result, available with
 * {@link #getModuleExecutorResult()}.
 */
public final class EndSimpleModuleTraceEvent extends EndExecutionTraceEvent {
    private static final long serialVersionUID = -2819494280752793163L;

    @Nullable private final SimpleModuleExecutorResult moduleExecutorResult;

    private EndSimpleModuleTraceEvent(long executionId, long timestamp, RuntimeExecutionTrace executionTrace,
            @Nullable SimpleModuleExecutorResult moduleExecutorResult) {
        super(executionId, timestamp, executionTrace,
            moduleExecutorResult != null && moduleExecutorResult.getExecutionException().isEmpty());
        this.moduleExecutorResult = moduleExecutorResult;
    }

    public static EndSimpleModuleTraceEvent of(long executionId, long timestamp, RuntimeExecutionTrace executionTrace,
            @Nullable SimpleModuleExecutorResult moduleExecutionResult) {
        return new EndSimpleModuleTraceEvent(executionId, timestamp, executionTrace,
            moduleExecutionResult);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!super.equals(otherObject)) {
            return false;
        }

        return Objects.equals(moduleExecutorResult, ((EndSimpleModuleTraceEvent) otherObject).moduleExecutorResult);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(moduleExecutorResult);
    }

    public Option<SimpleModuleExecutorResult> getModuleExecutorResult() {
        return moduleExecutorResult == null
            ? Option.<SimpleModuleExecutorResult>none()
            : Option.some(moduleExecutorResult);
    }
}
