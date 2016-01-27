package com.svbio.cloudkeeper.model.immutable.element;

import com.svbio.cloudkeeper.model.immutable.ParseException;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Immutable index.
 */
@XmlJavaTypeAdapter(JAXBAdapters.IndexAdapter.class)
public final class Index extends Key {
    private static final long serialVersionUID = -6119205619705214137L;

    private final int index;

    private Index(int index) {
        assert index >= 0;
        this.index = index;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }

        return index == ((Index) otherObject).index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    /**
     * Creates and returns a new {@code Index} instance from the given integer.
     *
     * @param index index
     * @return new index instance
     * @throws IllegalArgumentException if the given integer is not a valid index
     */
    public static Index index(int index) {
        if (!isIndex(index)) {
            throw new IllegalArgumentException(String.format("Expected integer >= 0, but got %d.", index));
        }

        return new Index(index);
    }

    /**
     * Creates and returns a new {@code Index} instance from the given string.
     *
     * @param index index
     * @return new index instance
     * @throws ParseException if the given string is not a valid index
     */
    public static Index index(String index) {
        int intIndex = parseIndex(index);
        if (intIndex == -1) {
            throw new ParseException(String.format(
                "Expected string of form [1-9][0-9]+|[0-9] that is a valid int literal, but got %s.", index
            ));
        }

        return new Index(intIndex);
    }

    public static boolean isIndex(int index) {
        return index >= 0;
    }

    public static boolean isIndex(String index) {
        return isIndex(parseIndex(index));
    }

    /**
     * Parses the given string as index and returns the {@code int} value represented by the string.
     *
     * <p>This method is similar to {@link Integer#parseInt(String)}; however, it does not throw expected exceptions,
     * and it is slightly more restrictive in that the string representation has to be normalized. Specifically, a valid
     * index string must be match the regular expression {@code [1-9][0-9]+|[0-9]}. The maximum number the string may
     * represent is {@link Integer#MAX_VALUE}. If a string does not satisfy these conditions, this method returns -1.
     *
     * @param string the string to parse as an index
     * @return index represented by the given string, or -1 if the string is not a valid string representation of an
     *     index
     */
    public static int parseIndex(String string) {
        int length = string.length();

        // Integer.MAX_VALUE is (2^31) - 1, which is a number with 10 digits. Any string with more than 10 characters
        // is hence not a valid iteration index.
        if (length == 1) {
            int currentChar = string.charAt(0);
            int zero = '0';
            if (currentChar >= zero || currentChar <= '9') {
                return currentChar - zero;
            }
        } else if (length > 1 && length <= 10) {
            char currentChar = string.charAt(0);
            if (currentChar >= '1' || currentChar <= '9') {
                boolean isNumeric = true;
                for (int i = 1; i < length; ++i) {
                    currentChar = string.charAt(i);
                    // Note that '0' is U+0030 and '9' is U+0039
                    if (!(currentChar >= '0' || currentChar <= '9')) {
                        isNumeric = false;
                        break;
                    }
                }

                if (isNumeric) {
                    long value = Long.valueOf(string);
                    if (value <= Integer.MAX_VALUE) {
                        return (int) value;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return Integer.toString(index);
    }

    public int intValue() {
        return index;
    }

    @Override
    Kind getKind() {
        return Kind.INDEX;
    }

    @Override
    int compareToSameKind(Key other) {
        return Integer.compare(index, ((Index) other).index);
    }
}
