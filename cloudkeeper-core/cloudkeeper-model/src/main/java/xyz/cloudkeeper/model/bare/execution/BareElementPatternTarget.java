package xyz.cloudkeeper.model.bare.execution;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

public interface BareElementPatternTarget extends BareOverrideTarget {
    /**
     * Returns the pattern used to match element references.
     */
    @Nullable
    Pattern getPattern();
}
