package com.svbio.cloudkeeper.model.runtime.element;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.element.BareParameterizable;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeParameterElement;
import com.svbio.cloudkeeper.model.util.ImmutableList;

/**
 * A mixin interface for an element that has type parameters.
 *
 * This is the linked immutable version of {@link com.svbio.cloudkeeper.model.bare.element.BareParameterizable}, and it
 * corresponds to {@link javax.lang.model.element.Parameterizable}.
 */
public interface RuntimeParameterizable extends BareParameterizable, RuntimeElement, Immutable {
    @Override
    ImmutableList<? extends RuntimeTypeParameterElement> getTypeParameters();
}
