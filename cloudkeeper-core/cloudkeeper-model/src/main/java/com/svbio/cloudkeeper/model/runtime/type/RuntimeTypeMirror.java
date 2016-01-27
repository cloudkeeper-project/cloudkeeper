package com.svbio.cloudkeeper.model.runtime.type;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.runtime.element.module.TypeRelationship;
import com.svbio.cloudkeeper.model.runtime.element.type.RuntimeTypeDeclaration;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.lang.model.type.TypeMirror;

public interface RuntimeTypeMirror extends BareTypeMirror, TypeMirror, Immutable {
    /**
     * Returns whether the given type is a subtype of this type. Any type is considered to be a subtype of itself.
     *
     * If this method returns {@code true}, a port of this type may be connected, without downcast, from a port of type
     * {@code type}.
     *
     * @param type type to test for being a subtype
     * @return whether the given type is a subtype of this type
     *
     * @see javax.lang.model.util.Types#isSubtype(TypeMirror, TypeMirror)
     */
    boolean	isLinkableFrom(RuntimeTypeMirror type);

    /**
     * Returns a list of type declarations (transitively) referenced by this type.
     */
    ImmutableList<? extends RuntimeTypeDeclaration> asTypeDeclaration();

    /**
     * Returns how this type relates to the given type in a potential connection.
     *
     * @param otherType other type
     * @return how this type relates to the given type in a potential connection
     */
    TypeRelationship relationshipTo(RuntimeTypeMirror otherType);
}
