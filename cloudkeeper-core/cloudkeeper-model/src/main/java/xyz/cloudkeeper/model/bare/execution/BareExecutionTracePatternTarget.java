package xyz.cloudkeeper.model.bare.execution;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public interface BareExecutionTracePatternTarget extends BareOverrideTarget {
    /**
     * Returns the pattern used to match execution traces.
     */
    @Nullable
    Pattern getPattern();
}
