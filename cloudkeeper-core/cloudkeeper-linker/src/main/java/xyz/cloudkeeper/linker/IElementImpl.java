package xyz.cloudkeeper.linker;

import xyz.cloudkeeper.model.runtime.element.RuntimeElement;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;

interface IElementImpl extends RuntimeElement, IAnnotatedConstructImpl {
    @Override
    @Nullable
    IElementImpl getSuperAnnotatedConstruct();

    @Override
    IElementImpl getEnclosingElement();

    /**
     * {@inheritDoc}
     *
     * <p>This method must return valid results already during {@link AbstractFreezable#finish(FinishContext)}.
     */
    @Override
    ImmutableList<? extends IElementImpl> getEnclosedElements();
}
