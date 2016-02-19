package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.LinkerException;
import xyz.cloudkeeper.model.bare.element.BareSimpleNameable;
import xyz.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

final class SimpleNameReference extends LocatableImpl implements BareSimpleNameable, Immutable {
    private final SimpleName simpleName;

    SimpleNameReference(@Nullable BareSimpleNameable original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        assert original != null;
        simpleName = Preconditions.requireNonNull(
            original.getSimpleName(), getCopyContext().newContextForProperty("simpleName"));
    }

    @Override
    public String toString() {
        return simpleName.toString();
    }

    @Override
    @Nonnull
    public SimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    void collectEnclosed(Collection<AbstractFreezable> freezables) { }

    @Override
    void preProcessFreezable(FinishContext context) { }

    @Override
    void finishFreezable(FinishContext context) { }

    @Override
    void verifyFreezable(VerifyContext context) { }
}
