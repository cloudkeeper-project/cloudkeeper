package com.svbio.cloudkeeper.model.immutable.element;

import java.io.ObjectStreamException;

public final class NoKey extends Key {
    private static final long serialVersionUID = -4619889102357058206L;

    private static final NoKey INSTANCE = new NoKey();

    public static NoKey instance() {
        return INSTANCE;
    }

    private NoKey() { }

    @Override
    public boolean equals(Object object) {
        return object != null && object.getClass().equals(this.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }

    @Override
    public String toString() {
        return "";
    }

    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }

    @Override
    Kind getKind() {
        return Kind.EMPTY;
    }

    @Override
    int compareToSameKind(Key other) {
        return 0;
    }
}
