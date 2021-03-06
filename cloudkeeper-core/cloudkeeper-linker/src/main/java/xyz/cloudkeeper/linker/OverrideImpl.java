package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.execution.BareOverride;
import xyz.cloudkeeper.model.runtime.execution.RuntimeOverride;
import xyz.cloudkeeper.model.util.ImmutableList;

import java.util.Collection;

final class OverrideImpl extends AnnotatedConstructImpl implements RuntimeOverride {
    private final ImmutableList<OverrideTargetImpl> targets;

    OverrideImpl(BareOverride original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        targets = immutableListOf(original.getTargets(), "targets", OverrideTargetImpl::copyOf);
    }

    @Override
    public ImmutableList<OverrideTargetImpl> getTargets() {
        return targets;
    }

    /**
     * {@inheritDoc}
     *
     * An override is not an element in the CloudKeeper language, and there is likewise no super element. Hence, this
     * method always returns {@code null}.
     */
    @Override
    public AnnotatedConstructImpl getSuperAnnotatedConstruct() {
        return null;
    }

    @Override
    void collectEnclosedByAnnotatedConstruct(Collection<AbstractFreezable> freezables) {
        freezables.addAll(targets);
    }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
