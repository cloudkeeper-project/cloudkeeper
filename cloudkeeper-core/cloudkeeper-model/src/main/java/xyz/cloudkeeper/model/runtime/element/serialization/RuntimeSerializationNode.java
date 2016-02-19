package xyz.cloudkeeper.model.runtime.element.serialization;

import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.element.serialization.BareSerializationNode;
import xyz.cloudkeeper.model.immutable.element.Key;

import javax.annotation.Nullable;

/**
 * Linked serialization node.
 */
public interface RuntimeSerializationNode extends BareSerializationNode, Immutable {
    @Override
    Key getKey();

    /**
     * Calls the visitor method that is appropriate for the actual type of this serialization node.
     *
     * @param visitor serialization-node visitor
     * @param parameter parameter passed to respective visit method in {@link RuntimeSerializationNodeVisitor}
     * @param <T> return type of visitor
     * @param <P> parameter type
     * @return result returned by visitor
     */
    @Nullable
    <T, P> T accept(RuntimeSerializationNodeVisitor<T, P> visitor, @Nullable P parameter);
}
