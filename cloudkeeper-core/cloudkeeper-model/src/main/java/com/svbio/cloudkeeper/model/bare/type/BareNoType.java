package com.svbio.cloudkeeper.model.bare.type;

/**
 * A pseudo-type used where no actual type is appropriate.
 *
 * <p>This interface corresponds to {@link javax.lang.model.type.NoType}, and both interfaces can be implemented with
 * covariant return types.
 *
 * <p>The interfaces in this package do not require an implementation of this interface, as {@code null} can also be
 * used to represent no type. However, (non-bare) subinterfaces may disallow {@code null}, in which case instances of
 * this interface have to be used instead.
 */
public interface BareNoType extends BareTypeMirror { }
