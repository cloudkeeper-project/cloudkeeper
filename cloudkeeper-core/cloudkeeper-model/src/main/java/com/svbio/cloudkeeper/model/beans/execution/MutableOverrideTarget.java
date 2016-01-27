package com.svbio.cloudkeeper.model.beans.execution;

import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.bare.execution.BareElementPatternTarget;
import com.svbio.cloudkeeper.model.bare.execution.BareElementTarget;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTracePatternTarget;
import com.svbio.cloudkeeper.model.bare.execution.BareExecutionTraceTarget;
import com.svbio.cloudkeeper.model.bare.execution.BareOverrideTarget;
import com.svbio.cloudkeeper.model.bare.execution.BareOverrideTargetVisitor;
import com.svbio.cloudkeeper.model.beans.CopyOption;
import com.svbio.cloudkeeper.model.beans.MutableLocatable;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlSeeAlso({
    MutableElementTarget.class, MutableElementPatternTarget.class, MutableExecutionTraceTarget.class,
    MutableExecutionTracePatternTarget.class
})
@XmlJavaTypeAdapter(JAXBAdapters.MutableOverrideTargetAdapter.class)
public abstract class MutableOverrideTarget<D extends MutableOverrideTarget<D>>
        extends MutableLocatable<D>
        implements BareOverrideTarget {
    private static final long serialVersionUID = 6542186380176627158L;

    MutableOverrideTarget() { }

    MutableOverrideTarget(BareLocatable original, CopyOption[] copyOptions) {
        super(original, copyOptions);
    }

    private enum CopyVisitor implements BareOverrideTargetVisitor<MutableOverrideTarget<?>, CopyOption[]> {
        INSTANCE;

        @Override
        public MutableOverrideTarget<?> visitElementTarget(BareElementTarget original,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableElementTarget.copyOfElementTarget(original, copyOptions);
        }

        @Override
        public MutableOverrideTarget<?> visitElementPatternTarget(BareElementPatternTarget original,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableElementPatternTarget.copyOfElementPatternTarget(original, copyOptions);
        }

        @Override
        public MutableOverrideTarget<?> visitExecutionTraceTarget(BareExecutionTraceTarget original,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableExecutionTraceTarget.copyOfExecutionTraceTarget(original, copyOptions);
        }

        @Override
        public MutableOverrideTarget<?> visitExecutionTracePatternTarget(BareExecutionTracePatternTarget original,
                @Nullable CopyOption[] copyOptions) {
            assert copyOptions != null;
            return MutableExecutionTracePatternTarget.copyOfExecutionTracePatternTarget(original, copyOptions);
        }
    }

    @Nullable
    public static MutableOverrideTarget<?> copyOfOverrideTarget(@Nullable BareOverrideTarget original,
            CopyOption... copyOptions) {
        return original == null
            ? null
            : original.accept(CopyVisitor.INSTANCE, copyOptions);
    }
}
