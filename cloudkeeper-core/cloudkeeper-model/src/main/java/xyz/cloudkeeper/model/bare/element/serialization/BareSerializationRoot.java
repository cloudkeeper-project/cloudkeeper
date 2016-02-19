package xyz.cloudkeeper.model.bare.element.serialization;

import xyz.cloudkeeper.model.bare.element.BareQualifiedNameable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Node in a persistence tree that can have child nodes.
 *
 * @see BareSerializationNode
 */
public interface BareSerializationRoot extends BareSerializationNode {
    /**
     * Human-readable name of the entity modeled by this interface.
     */
    String NAME = "serialization tree";

    /**
     * Returns the name of the CloudKeeper persistence plugin used to create this persistence tree.
     *
     * <p>The referenced CloudKeeper serialization class will be used to deserialize this tree of byte sequences into a
     * Java object.
     *
     * @return name of the CloudKeeper serialization class used to create this serialized representation of a value
     */
    @Nullable
    BareQualifiedNameable getDeclaration();

    /**
     * Returns the child nodes of this root node.
     */
    List<? extends BareSerializationNode> getEntries();

    /**
     * Default implementations for standard methods.
     */
    final class Default {
        private Default() { }

        public static String toString(BareSerializationRoot instance) {
            return String.format("%s '%s'", NAME, instance.getKey());
        }
    }
}
