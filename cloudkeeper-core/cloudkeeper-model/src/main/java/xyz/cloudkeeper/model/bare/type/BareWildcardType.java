package xyz.cloudkeeper.model.bare.type;

import javax.annotation.Nullable;

/**
 * Wildcard type argument.
 *
 * <p>A wildcard type argument is not a type according to the Java Language Specification, but only a type argument.
 * However, the {@link javax.lang.model} interfaces model is as subinterface of
 * {@link javax.lang.model.type.TypeMirror}.
 *
 * <p>This interface corresponds to {@link javax.lang.model.type.WildcardType}, and both interfaces can be implemented
 * with covariant return types.
 */
public interface BareWildcardType extends BareTypeMirror {
    /**
     * Returns the upper bound of this wildcard.
     *
     * <p>If no upper bound is explicitly declared, {@code null} is returned.
     *
     * @see javax.lang.model.type.WildcardType#getExtendsBound()
     */
    @Nullable
    BareTypeMirror getExtendsBound();

    /**
     * Returns the lower bound of this wildcard.
     *
     * If no lower bound is explicitly declared, {@code null} is returned.
     *
     * @see javax.lang.model.type.WildcardType#getSuperBound()
     */
    @Nullable
    BareTypeMirror getSuperBound();


    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        /**
         * Default implementation for {@link BareWildcardType#toString()}.
         */
        public static String toString(BareWildcardType instance) {
            @Nullable BareTypeMirror extendsBound = instance.getExtendsBound();
            @Nullable BareTypeMirror superBound = instance.getSuperBound();

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('?');
            if (extendsBound != null) {
                stringBuilder.append(" extends ").append(extendsBound);
            }
            if (superBound != null) {
                stringBuilder.append(" super ").append(superBound);
            }
            return stringBuilder.toString();
        }
    }
}
