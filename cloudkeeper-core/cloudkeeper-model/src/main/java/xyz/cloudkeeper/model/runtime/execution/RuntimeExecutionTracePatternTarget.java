package xyz.cloudkeeper.model.runtime.execution;

import xyz.cloudkeeper.model.bare.execution.BareExecutionTracePatternTarget;

import javax.annotation.Nonnull;
import java.util.regex.Pattern;

public interface RuntimeExecutionTracePatternTarget extends RuntimeOverrideTarget, BareExecutionTracePatternTarget {
    @Override
    @Nonnull
    Pattern getPattern();
}
