package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.BareQualifiedNameable;
import com.svbio.cloudkeeper.model.immutable.element.Name;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

final class NameReference extends LocatableImpl implements BareQualifiedNameable, Immutable {
    private final Name name;

    NameReference(@Nullable BareQualifiedNameable original, CopyContext parentContext) throws LinkerException {
        super(original, parentContext);
        assert original != null;
        name = Preconditions.requireNonNull(
            original.getQualifiedName(), getCopyContext().newContextForProperty("qualifiedName"));
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    @Nonnull
    public Name getQualifiedName() {
        return name;
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
