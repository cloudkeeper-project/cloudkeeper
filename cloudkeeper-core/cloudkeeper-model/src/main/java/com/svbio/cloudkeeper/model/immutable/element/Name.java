package com.svbio.cloudkeeper.model.immutable.element;

import com.svbio.cloudkeeper.model.immutable.ParseException;
import com.svbio.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.lang.model.SourceVersion;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable name.
 */
@XmlJavaTypeAdapter(JAXBAdapters.NameAdapter.class)
public class Name extends Key implements javax.lang.model.element.Name {
    private static final long serialVersionUID = 1024297931248849759L;
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final String qualifiedName;

    /**
     * Package-protected constructor in order to disallow inheritance outside of this package.
     */
    Name(String qualifiedName) {
        this.qualifiedName = requireQualifiedName(qualifiedName);
    }

    @Override
    public final boolean equals(@Nullable Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!(otherObject instanceof Name)) {
            return false;
        }

        return qualifiedName.equals(((Name) otherObject).qualifiedName);
    }

    @Override
    public final int hashCode() {
        return qualifiedName.hashCode();
    }

    /**
     * Creates and returns a new name from the given string.
     *
     * If the given name is a simple name, a {@link SimpleName} instance is returned.
     *
     * @param qualifiedName qualified-name string
     * @return new qualified-name instance
     * @throws NullPointerException if the given string is null
     * @throws ParseException if the given string is not a valid Java qualified name according to §6.2 JLS
     */
    public static Name qualifiedName(String qualifiedName) {
        return SimpleName.isIdentifier(Objects.requireNonNull(qualifiedName))
            ? new SimpleName(qualifiedName)
            : new Name(qualifiedName);
    }

    /**
     * Returns whether the given string is a valid name.
     *
     * @throws NullPointerException if the given string is null
     */
    public static boolean isQualifiedName(String string) {
        return SourceVersion.isName(string);
    }

    private static String requireQualifiedName(String string) {
        if (!isQualifiedName(string)) {
            throw new ParseException(String.format(
                "Expected valid qualified name according to §6.2 of the Java Language Specification, but got %s.",
                string
            ));
        }
        return string;
    }

    @Override
    public final boolean contentEquals(CharSequence charSequence) {
        return qualifiedName.contentEquals(charSequence);
    }

    @Override
    public final int length() {
        return qualifiedName.length();
    }

    @Override
    public final char charAt(int index) {
        return qualifiedName.charAt(index);
    }

    @Override
    public final CharSequence subSequence(int start, int end) {
        return qualifiedName.subSequence(start, end);
    }

    public SimpleName toSimpleName() {
        // Simple names should always be represented by class SimpleName!
        assert qualifiedName.lastIndexOf('.') >= 0 : "Expected qualified name, but got " + qualifiedName;

        return SimpleName.identifier(qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1));
    }

    @Override
    public final String toString() {
        return qualifiedName;
    }

    @Override
    final Kind getKind() {
        return Kind.NAME;
    }

    @Override
    final int compareToSameKind(Key other) {
        return qualifiedName.compareTo(((Name) other).qualifiedName);
    }

    public ImmutableList<SimpleName> asList() {
        String[] array = DOT_PATTERN.split(qualifiedName);
        List<SimpleName> list = new ArrayList<>(array.length);
        for (String element: array) {
            list.add(SimpleName.identifier(element));
        }
        return ImmutableList.copyOf(list);
    }

    static void requireValidRange(int size, int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex = " + fromIndex);
        } else if (toIndex > size) {
            throw new IllegalArgumentException("toIndex = " + toIndex);
        } else if (fromIndex >= toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") >= toIndex(" + toIndex + ')');
        }
    }

    public Name subname(int fromIndex, int toIndex) {
        List<SimpleName> list = asList();
        requireValidRange(list.size(), fromIndex, toIndex);
        StringBuilder stringBuilder = new StringBuilder(qualifiedName.length());
        boolean first = true;
        for (SimpleName simpleName: list.subList(fromIndex, toIndex)) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('.');
            }
            stringBuilder.append(simpleName);
        }
        return qualifiedName(stringBuilder.toString());
    }

    public Name join(Name name) {
        return qualifiedName(qualifiedName + '.' + name);
    }

    private int numElementsForPackageName() {
        int packageNameElements = 0;
        for (SimpleName simpleName : asList()) {
            if (Character.isUpperCase(simpleName.toString().codePointAt(0))) {
                break;
            }
            ++packageNameElements;
        }
        return packageNameElements;
    }

    /**
     * Returns whether this name is a valid CloudKeeper package name.
     *
     * <p>A valid CloudKeeper package name only consists of identifiers that start with a lower-case letter.
     */
    public boolean isPackageName() {
        return numElementsForPackageName() == asList().size();
    }

    public final Name getPackageName() {
        int packageNameElements = numElementsForPackageName();
        return packageNameElements == 0
            ? null
            : subname(0, packageNameElements);
    }

    public Name getBinaryName() {
        int packageNameElements = numElementsForPackageName();
        List<SimpleName> simpleNames = asList();
        boolean first = true;

        StringBuilder stringBuilder = new StringBuilder(qualifiedName.length());
        int i = 0;
        for (SimpleName simpleName: simpleNames) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(
                    i <= packageNameElements
                        ? '.'
                        : '$'
                );
            }
            stringBuilder.append(simpleName);
            ++i;
        }
        return qualifiedName(stringBuilder.toString());
    }
}
