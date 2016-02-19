package xyz.cloudkeeper.marshaling;

import xyz.cloudkeeper.model.api.Marshaler;
import xyz.cloudkeeper.model.immutable.element.Key;

import java.util.List;

/**
 * Represents a predicate (boolean-valued function) that returns whether an object should be marshaled.
 *
 * <p>Instances of this functional interface are intended to be used for creating marshaling-tree builders using
 * {@link MarshalingTreeBuilder#create(ShouldMarshalPredicate)}.
 *
 * @see {@link TreeBuilder#shouldMarshal(Marshaler, Object)}
 */
@FunctionalInterface
public interface ShouldMarshalPredicate {
    /**
     * Evaluates this predicate.
     *
     * <p>If called by {@link MarshalingTreeBuilder}, it is guaranteed that {@link Marshaler#isImmutable(Object)}
     * will return {@code true} for the given marshaler and {@code object} as argument.
     *
     * @param path the sequence of keys of all ancestor nodes and the current node (starting at the root node)
     * @param marshaler marshaler that would be used if {@code false} is returned
     * @param object object to be examined
     * @return whether the given object should be marshaled
     */
    boolean test(List<Key> path, Marshaler<?> marshaler, Object object);
}
