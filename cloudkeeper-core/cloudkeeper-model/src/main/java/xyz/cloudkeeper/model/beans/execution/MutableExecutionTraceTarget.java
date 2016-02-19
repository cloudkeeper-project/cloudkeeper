package xyz.cloudkeeper.model.beans.execution;

import xyz.cloudkeeper.model.bare.execution.BareExecutionTrace;
import xyz.cloudkeeper.model.bare.execution.BareExecutionTraceTarget;
import xyz.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlValue;
import java.util.Objects;

public final class MutableExecutionTraceTarget
        extends MutableOverrideTarget<MutableExecutionTraceTarget>
        implements BareExecutionTraceTarget {
    private static final long serialVersionUID = 4406474767043361447L;

    @Nullable private ExecutionTrace executionTrace;

    public MutableExecutionTraceTarget() { }

    private MutableExecutionTraceTarget(BareExecutionTraceTarget original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        @Nullable BareExecutionTrace originalExecutionTrace = original.getExecutionTrace();
        executionTrace = originalExecutionTrace == null
            ? null
            : ExecutionTrace.copyOf(originalExecutionTrace);
    }

    @Nullable
    public static MutableExecutionTraceTarget copyOfExecutionTraceTarget(@Nullable BareExecutionTraceTarget original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableExecutionTraceTarget(original, copyOptions);
    }

    @Override
    protected MutableExecutionTraceTarget self() {
        return this;
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return Objects.equals(executionTrace, ((MutableExecutionTraceTarget) otherObject).executionTrace);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(executionTrace);
    }

    @Override
    @Nullable
    public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitExecutionTraceTarget(this, parameter);
    }

    @XmlValue
    @Override
    @Nullable
    public ExecutionTrace getExecutionTrace() {
        return executionTrace;
    }

    public MutableExecutionTraceTarget setExecutionTrace(@Nullable ExecutionTrace executionTrace) {
        this.executionTrace = executionTrace;
        return this;
    }

    public MutableExecutionTraceTarget setExecutionTrace(String executionTrace) {
        return setExecutionTrace(ExecutionTrace.valueOf(executionTrace));
    }
}
