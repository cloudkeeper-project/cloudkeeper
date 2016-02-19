package xyz.cloudkeeper.model.runtime.element;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.BareParameterizable;
import xyz.cloudkeeper.model.runtime.element.type.RuntimeTypeParameterElement;
import xyz.cloudkeeper.model.util.ImmutableList;

/**
 * A mixin interface for an element that has type parameters.
 *
 * This is the linked immutable version of {@link xyz.cloudkeeper.model.bare.element.BareParameterizable}, and it
 * corresponds to {@link javax.lang.model.element.Parameterizable}.
 */
public interface RuntimeParameterizable extends BareParameterizable, RuntimeElement, Immutable {
    @Override
    ImmutableList<? extends RuntimeTypeParameterElement> getTypeParameters();
}
