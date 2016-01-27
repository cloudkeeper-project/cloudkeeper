package com.svbio.cloudkeeper.model.beans.execution;

import com.svbio.cloudkeeper.model.bare.execution.BareElementPatternTarget;
import com.svbio.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlValue;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MutableElementPatternTarget
        extends MutableOverrideTarget<MutableElementPatternTarget>
        implements BareElementPatternTarget {
    private static final long serialVersionUID = -8210895328668683259L;

    @Nullable private Pattern pattern;

    public MutableElementPatternTarget() { }

    private MutableElementPatternTarget(BareElementPatternTarget original, CopyOption[] copyOptions) {
        super(original, copyOptions);
        pattern = original.getPattern();
    }

    @Nullable
    public static MutableElementPatternTarget copyOfElementPatternTarget(@Nullable BareElementPatternTarget original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : new MutableElementPatternTarget(original, copyOptions);
    }

    @Override
    public boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        @Nullable Pattern otherPattern = ((MutableElementPatternTarget) otherObject).pattern;
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
    protected MutableElementPatternTarget self() {
        return this;
    }

    @Override
    @Nullable
    public <T, P> T accept(BareOverrideTargetVisitor<T, P> visitor, @Nullable P parameter) {
        return visitor.visitElementPatternTarget(this, parameter);
    }

    @XmlValue
    @Override
    @Nullable
    public Pattern getPattern() {
        return pattern;
    }

    public MutableElementPatternTarget setPattern(@Nullable Pattern pattern) {
        this.pattern = pattern;
        return this;
    }
}
