package com.svbio.cloudkeeper.model.immutable.element;

import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.immutable.ParseException;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;

@XmlJavaTypeAdapter(JAXBAdapters.KeyAdapter.class)
public abstract class Key implements Immutable, Serializable, Comparable<Key> {
    private static final long serialVersionUID = -1589923897175707612L;

    enum Kind {
        EMPTY,
        NAME,
        INDEX
    }

    Key() { }

    /**
     * Returns a key corresponding to the given string representation.
     *
     * The object may be a valid string representation of one of the following:
     * <ul><li>
     *     a simple name (as specified by {@link SimpleName}),
     * </li><li>
     *     a qualified name (as specified by {@link Name}, if it is not a simple name),
     * </li><li>
     *     a non-negative index, or
     * </li><li>
     *     {@code null} (in which case the empty token provided by {@link NoKey()} will be returned).
     * </li></ul>
     *
     * @param string string to convert into a token, may be {@code null}
     * @return corresponding {@code Token} instance
     * @throws com.svbio.cloudkeeper.model.immutable.ParseException if the given string cannot be parsed
     */
    public static Key valueOf(String string) {
        if (string == null || string.isEmpty()) {
            return NoKey.instance();
        } else if (SimpleName.isIdentifier(string)) {
            return new SimpleName(string);
        } else if (Name.isQualifiedName(string)) {
            return new Name(string);
        } else if (Index.isIndex(string)) {
            return Index.index(string);
        }

        throw new ParseException(String.format("%s is not a valid key.", string));
    }

    @Override
    public abstract String toString();

    abstract Kind getKind();

    abstract int compareToSameKind(Key other);

    @Override
    public final int compareTo(Key other) {
        Kind kind = getKind();
        int result = kind.compareTo(other.getKind());
        if (result != 0) {
            return result;
        }

        return compareToSameKind(other);
    }
}
