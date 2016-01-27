package com.svbio.cloudkeeper.model.bare.type;

import com.svbio.cloudkeeper.model.bare.BareLocatable;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * Type mirror.
 *
 * <p>This interface corresponds to {@link javax.lang.model.type.TypeMirror}, and both interfaces can be implemented
 * with covariant return types.
 *
 * <p>Each instance is guaranteed to implement <em>at most</em> one of the following interfaces:
 * <ul><li>
 *     {@link BareArrayType}
 * </li><li>
 *     {@link BareDeclaredType}
 * </li><li>
 *     {@link BareNoType}
 * </li><li>
 *     {@link BarePrimitiveType}
 * </li><li>
 *     {@link BareTypeVariable}
 * </li><li>
 *     {@link BareWildcardType}
 * </li></ul>
 *
 * <p>Unlike required for other super-interfaces in related packages, instances of this interface are <em>not</em>
 * guaranteed to implement one of the specialized interfaces above. This is because concrete implementations may contain
 * additional types (such as intersection types) that are not modelled in this package.
 */
public interface BareTypeMirror extends BareLocatable {
    /**
     * Applies a visitor to this type.
     *
     * @param visitor the visitor operating on this type
     * @param parameter additional parameter to the visitor
     * @param <T> the return type of the visitor's methods
     * @param <P> the type of the additional parameter to the visitor's methods
     * @return a visitor-specified result
     */
    @Nullable
    <T, P> T accept(BareTypeMirrorVisitor<T, P> visitor, @Nullable P parameter);

    /**
     * Returns the string representation of this type.
     */
    @Override
    String toString();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Returns the concatenation of the string representation of the given types, with the given glue in between.
         */
        static String toString(List<? extends BareTypeMirror> types, String glue) {
            StringBuilder builder = new StringBuilder();
            Iterator<? extends BareTypeMirror> it = types.iterator();
            while (it.hasNext()) {
                builder.append(it.next());
                if (it.hasNext()) {
                    builder.append(glue);
                }
            }
            return builder.toString();
        }
    }
}
