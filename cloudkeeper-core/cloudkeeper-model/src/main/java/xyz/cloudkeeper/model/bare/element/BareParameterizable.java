package xyz.cloudkeeper.model.bare.element;

import xyz.cloudkeeper.model.bare.element.type.BareTypeParameterElement;

import java.util.List;

/**
 * A mixin interface for an element that has type parameters.
 *
 * This interface corresponds to {@link javax.lang.model.element.Parameterizable}, and both interfaces can be
 * implemented with covariant return types.
 */
public interface BareParameterizable extends BareElement {
    /**
     * Returns the formal type parameters of the element in declaration order.
     *
     * @see javax.lang.model.element.Parameterizable#getTypeParameters()
     */
    List<? extends BareTypeParameterElement> getTypeParameters();
}
