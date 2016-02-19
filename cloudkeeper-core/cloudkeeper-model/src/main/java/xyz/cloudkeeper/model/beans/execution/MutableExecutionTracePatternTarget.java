package xyz.cloudkeeper.model.beans.execution;

import xyz.cloudkeeper.model.bare.execution.BareExecutionTracePatternTarget;
import xyz.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import xyz.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlValue;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MutableExecutionTracePatternTarget
        extends MutableOverrideTarget<MutableExecutionTracePatternTarget>
        implements BareExecutionTracePatternTarget {
    private static final long serialVersionUID = -202329324880766190L;

    @Nullable private Pattern pattern;

    public MutableExecutionTracePatternTarget() { }

    private MutableExecutionTracePatternTarget(BareExecutionTracePatternTarget original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        pattern = original.getPattern();
    }

    @Nullable
    public static MutableExecutionTracePatternTarget copyOfExecutionTracePatternTarget(
            @Nullable BareExecutionTracePatternTarget original, CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableExecutionTracePatternTarget(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        @Nullable Pattern otherPattern = ((MutableExecutionTracePatternTarget) otherObject).pattern;
        @Nullable String string = pattern != null
            ? pattern.toString()
            : null;
        @Nullable String otherString = otherPattern != null
            ? otherPattern.toString()
            : null;
        return Objects.equals(string, otherString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pattern != null
            ? pattern.toString()
            : null
        );
    }

    @Override
    protected MutableExecutionTracePatternTarget self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitExecutionTracePatternTarget(this, parameter);
    }

    @XmlValue
    @Override
    @Nullable
    public Pattern getPattern() {
        return pattern;
    }

    public MutableExecutionTracePatternTarget setPattern(@Nullable Pattern pattern) {
        this.pattern = pattern;
        return this;
    }
}
