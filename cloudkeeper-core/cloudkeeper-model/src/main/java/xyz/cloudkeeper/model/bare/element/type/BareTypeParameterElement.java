package xyz.cloudkeeper.model.bare.element.type;

import xyz.cloudkeeper.model.bare.element.BareElement;
import xyz.cloudkeeper.model.bare.element.BareSimpleNameable;
import xyz.cloudkeeper.model.bare.type.BareTypeMirror;

import java.util.Iterator;
import java.util.List;

/**
 * Formal type parameter of a generic class or interface.
 *
 * <p>This interface corresponds to {@link javax.lang.model.element.TypeParameterElement}, and both interfaces can be
 * implemented with covariant return types.
 *
 * @see xyz.cloudkeeper.model.bare.type.BareTypeVariable
 * @see javax.lang.model.element.TypeParameterElement
 */
public interface BareTypeParameterElement extends BareSimpleNameable, BareElement {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "type parameter";

    /**
     * Returns the bounds of this type parameter.
     *
     * These are the types given by the {@code extends} clause used to declare this type parameter.
     *
     * If this is {@code null} or an empty list, then {@code java.lang.Object} is considered to be the sole bound.
     *
     * @see javax.lang.model.element.TypeParameterElement#getBounds()
     */
    List<? extends BareTypeMirror> getBounds();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareTypeParameterElement#toString()}.
         */
        public static String toHumanReadableString(BareTypeParameterElement instance) {
            StringBuilder stringBuilder = new StringBuilder();

            // append(null) will produce "null"
            stringBuilder.append(NAME).append(' ').append(instance.getSimpleName());

            List<? extends BareTypeMirror> bounds = instance.getBounds();
            if (!bounds.isEmpty()) {
                stringBuilder.append(" extends ");
                Iterator<? extends BareTypeMirror> boundIterator = bounds.iterator();
                while (boundIterator.hasNext()) {
                    stringBuilder.append(boundIterator.next());
                    if (boundIterator.hasNext()) {
                        stringBuilder.append(" & ");
                    }
                }
            }

            return stringBuilder.toString();
        }
    }
}
